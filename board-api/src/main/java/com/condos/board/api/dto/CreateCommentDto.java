package com.condos.board.api.dto;

import com.condos.board.model.AttachmentDoc;

import java.util.List;

public record CreateCommentDto(String authorId, String text, List<AttachmentDto> attachments) {}