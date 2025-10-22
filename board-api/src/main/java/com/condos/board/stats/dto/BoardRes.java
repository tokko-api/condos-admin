package com.condos.board.stats.dto;
public record BoardRes(String boardId, String boardName, long open, long inProgress, long done) {}