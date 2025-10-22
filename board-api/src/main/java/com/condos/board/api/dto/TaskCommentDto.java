package com.condos.board.api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TaskCommentDto {
    private String id;                // generado por Mongo
    private String taskId;            // referencia a la tarea
    private String authorId;          // id del usuario que comentó
    private String text;              // texto del comentario
    private Long createdAt;           // timestamp en milisegundos
}