package com.condos.board.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDoc {
    @Id
    String id;
    String taskId;
    String key;               // boards/{boardId}/tasks/{taskId}/{uuid}.jpg
    String contentType;
    Long size;
    Instant createdAt;
    String createdBy;
}