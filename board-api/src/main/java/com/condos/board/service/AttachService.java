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
import net.bytebuddy.TypeCache;
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

    private final MinioClient minio;

    private final AttachmentRepo attachmentRepo;
    private final TaskCommentRepo commentRepo;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.presignMinutes}")
    private int mins;

    // ========= 1️⃣ COMENTARIOS =========
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
                        presignGet(doc.getKey()).toString()   // URL fresca
                ));
    }
    // ========= 2️⃣ ARCHIVOS =========

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

    public Mono<PresignResponse> presign(String boardId, String taskId, PresignRequest req) {
        // genera key único dentro del bucket
        String ext = switch (req.contentType()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
        String key = "boards/%s/tasks/%s/%s.%s".formatted(boardId, taskId, UUID.randomUUID(), ext);

        URI uploadUrl = presignPut(key, req.contentType());
        return Mono.just(new PresignResponse(key, uploadUrl.toString()));
    }

    public Mono<AttachmentDto> complete(String boardId ,String taskId, CompleteUploadDto req, String user) {
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
                    var readUrl = presignGet(saved.getKey());           // firma GET con tu host externo
                    return new AttachmentDto(
                            saved.getId(), saved.getKey(), saved.getContentType(), saved.getSize(), readUrl.toString()
                    );
                });
    }

    // ========= 3️⃣ MÉTODOS INTERNOS (ya los tienes) =========

    public URI presignPut(String key, String contentType) {
        try {
            String url = minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .expiry(mins * 60)
                            .build()
            );
            return URI.create(url);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public URI presignGet(String key) {
        try {
            return URI.create(minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(5 * 60)
                            .build()
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}