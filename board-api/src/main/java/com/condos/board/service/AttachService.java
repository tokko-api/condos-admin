package com.condos.board.service;

import com.condos.board.api.dto.*;
import com.condos.board.api.model.TaskCommentDoc;
import com.condos.board.model.AttachmentDoc;
import com.condos.board.repository.AttachmentRepo;
import com.condos.board.repository.TaskCommentRepo;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachService {

    private final MinioClient minioInternal;
    private final MinioClient minioPublic;
    private final AttachmentRepo attachmentRepo;
    private final TaskCommentRepo commentRepo;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.presignMinutes}")
    private int mins;

    /** Base pública (HTTPS) expuesta por Traefik para MinIO, p.ej.:
     *  public.minio.base = https://d9f803cbaa64.ngrok-free.app/minio
     */
    @Value("${public.minio.base}")
    private String publicMinioBase;

    @Value("${public.api.base}")     // p.ej. https://zr9p52s-....exp.direct  (SIN /minio)
    private String publicApiBase;

    // ========= 1) COMENTARIOS =========

    public Mono<TaskCommentDto> addComment(String taskId, CreateCommentDto req) {
        TaskCommentDoc doc = TaskCommentDoc.builder()
                .taskId(taskId)
                .authorId(req.authorId())
                .text(req.text())
                .createdAt(Instant.now())
                .build();

        return commentRepo.save(doc)
                .map(saved -> TaskCommentDto.builder()
                        .id(saved.getId())
                        .taskId(saved.getTaskId())
                        .authorId(saved.getAuthorId())
                        .text(saved.getText())
                        .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt().toEpochMilli() : null)
                        .build());
    }

    public Flux<AttachmentDto> listAttachments(String taskId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return attachmentRepo.findByTaskId(taskId, pageable)
                .map(doc -> new AttachmentDto(
                        doc.getId(),
                        doc.getKey(),
                        doc.getContentType(),
                        doc.getSize(),
                        String.format("%s/condos/api/files/%s", publicApiBase, doc.getKey()) // 👈 SIEMPRE API
                ));
    }

    public Flux<TaskCommentDto> listComments(String taskId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        return commentRepo.findByTaskIdOrderByCreatedAtAsc(taskId, pageable)
                .map(doc -> TaskCommentDto.builder()
                        .id(doc.getId())
                        .taskId(doc.getTaskId())
                        .authorId(doc.getAuthorId())
                        .text(doc.getText())
                        .createdAt(doc.getCreatedAt() != null ? doc.getCreatedAt().toEpochMilli() : null)
                        .build());
    }

    // ========= 2) ARCHIVOS =========

    public Mono<PresignResponse> presign(String boardId, String taskId, PresignRequest req) {
        String ext = switch (req.contentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
        String key = "boards/%s/tasks/%s/%s.%s".formatted(boardId, taskId, UUID.randomUUID(), ext);

        // ⬅️ SUBIDA por TU API (no a MinIO directo)
        String uploadUrl = "%s/condos/api/files/%s".formatted(publicApiBase, key);

        return Mono.just(new PresignResponse(key, uploadUrl));
    }

    public Mono<AttachmentDto> complete(String boardId, String taskId, CompleteUploadDto req, String user) {
        var doc = AttachmentDoc.builder()
                .taskId(taskId)
                .key(req.key())
                .contentType(req.contentType())
                .size(req.size())
                .createdAt(Instant.now())
                .createdBy(user)
                .build();

        return attachmentRepo.save(doc).map(saved -> {
            // ⬅️ LECTURA presign GET (MinIO público)
            String readUrl = presignGet(saved.getKey()).toString();
            return new AttachmentDto(saved.getId(), saved.getKey(), saved.getContentType(), saved.getSize(), readUrl);
        });
    }

    // ========= 3) MÉTODOS INTERNOS =========

    /** Reescribe la URL presignada de MinIO (interna) a la base pública HTTPS (Traefik/ngrok).
     *  También añade el query 'ngrok-skip-browser-warning=true' para evitar la interstitial.
     */

    private static String normalizeBase(String base) {
        return base != null && base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private URI toPublic(URI internal) {
        // p.ej. internal = http://minio:9000/condos-attachments/...?...X-Amz-...
        String baseInternal = internal.getScheme() + "://" + internal.getAuthority(); // http://minio:9000
        String publicBase   = normalizeBase(publicMinioBase);                         // https://<ngrok>/minio

        String out = internal.toString().replace(baseInternal, publicBase);

        if (!out.contains("ngrok-skip-browser-warning=")) {
            out += (out.contains("?") ? "&" : "?") + "ngrok-skip-browser-warning=true";
        }
        return URI.create(out);
    }


    public URI presignPut(String key, String contentType) {
        try {
            String url = minioPublic.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .expiry(mins * 60)
                            .build()
            );
            return toPublic(URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private URI presignGet(String key) {
        try {
            String url = minioPublic.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(5 * 60) // 5 min
                            .build()
            );
            // tip: agregar el bypass de ngrok si quieres:
            if (!url.contains("ngrok-skip-browser-warning=")) {
                url += (url.contains("?") ? "&" : "?") + "ngrok-skip-browser-warning=true";
            }
            return toPublic(URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
