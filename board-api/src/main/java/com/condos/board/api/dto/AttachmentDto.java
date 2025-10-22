package com.condos.board.api.dto;

public record AttachmentDto(String id, String key, String contentType, Long size, String url) {}