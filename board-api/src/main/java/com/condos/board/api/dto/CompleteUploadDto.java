package com.condos.board.api.dto;

public record CompleteUploadDto(String commentId, String key, String contentType, long size) {}
