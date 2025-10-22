package com.condos.board.api.model;

import com.condos.board.api.dto.AttachmentDto;
import com.condos.board.model.AttachmentDoc;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("task_comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentDoc {
    @Id
    private String id;

    private String taskId;
    private String authorId;
    private String text;
    private Instant createdAt;
}