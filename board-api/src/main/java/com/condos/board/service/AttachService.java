package com.condos.board.service;

import com.condos.board.api.dto.AttachmentDto;
import com.condos.board.api.dto.CompleteUploadDto;
import com.condos.board.api.dto.CreateCommentDto;
import com.condos.board.api.dto.PresignRequest;
import com.condos.board.api.dto.PresignResponse;
import com.condos.board.api.dto.TaskCommentDto;
import com.condos.board.api.model.TaskCommentDoc;
import com.condos.board.model.AttachmentDoc;
import com.condos.board.repository.AttachmentRepo;
import com.condos.board.repository.TaskCommentRepo;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachService {

    // Si en el futuro quieres que este servicio hable directo con MinIO,
    // ya tienes el cliente inyectado. Por ahora no se usa.
    private final MinioClient minioInternal;

    private final AttachmentRepo attachmentRepo;
    private final TaskCommentRepo commentRepo;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.presignMinutes}")
    private int mins;

    /**
     * Base pública de la API, por ejemplo:
     *   public.api.base = http://206.189.69.68
     *
     * SIN el path /condos/api. Ese se agrega aquí.
     */
    @Value("${public.api.base}")
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

    /**
     * Genera un key de objeto y devuelve la URL de subida,
     * que SIEMPRE es vía tu API:
     *
     *   POST {publicApiBase}/condos/api/files/{key}
     */
    public Mono<PresignResponse> presign(String boardId, String taskId, PresignRequest req) {
        String ext = switch (req.contentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };

        String key = "boards/%s/tasks/%s/%s.%s".formatted(
                boardId,
                taskId,
                UUID.randomUUID(),
                ext
        );

        String uploadUrl = "%s/condos/api/files/%s".formatted(publicApiBase, key);

        return Mono.just(new PresignResponse(key, uploadUrl));
    }

    /**
     * Marca en Mongo que la subida terminó y devuelve el DTO
     * con la URL pública de lectura, que también es SIEMPRE
     * vía la API:
     *
     *   GET {publicApiBase}/condos/api/files/{key}
     */
    public Mono<AttachmentDto> complete(String boardId, String taskId, CompleteUploadDto req, String user) {
        var doc = AttachmentDoc.builder()
                .taskId(taskId)
                .key(req.key())
                .contentType(req.contentType())
                .size(req.size())
                .createdAt(Instant.now())
                .createdBy(user)
                .build();

        return attachmentRepo.save(doc)
                .map(saved -> {
                    String readUrl = "%s/condos/api/files/%s".formatted(publicApiBase, saved.getKey());
                    return new AttachmentDto(
                            saved.getId(),
                            saved.getKey(),
                            saved.getContentType(),
                            saved.getSize(),
                            readUrl
                    );
                });
    }

    /**
     * Lista adjuntos de una tarea, devolviendo siempre URLs de lectura
     * vía la API (mismo formato que complete()).
     */
    public Flux<AttachmentDto> listAttachments(String taskId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return attachmentRepo.findByTaskId(taskId, pageable)
                .map(doc -> new AttachmentDto(
                        doc.getId(),
                        doc.getKey(),
                        doc.getContentType(),
                        doc.getSize(),
                        String.format("%s/condos/api/files/%s", publicApiBase, doc.getKey())
                ));
    }
}