package com.condos.board.api.dto;

import com.condos.board.model.Amenity;

import java.time.LocalDate;
import java.util.List;

/** Fecha sin disponibilidad para reservar una amenidad (mantenimiento u otro motivo). */
public record BlockedDateDto(LocalDate date, String reason) {

    public Amenity.BlockedDate toModel() {
        return new Amenity.BlockedDate(date, reason);
    }

    public static BlockedDateDto from(Amenity.BlockedDate b) {
        return new BlockedDateDto(b.getDate(), b.getReason());
    }

    public static List<Amenity.BlockedDate> toModelList(List<BlockedDateDto> dtos) {
        return dtos == null ? List.of() : dtos.stream().map(BlockedDateDto::toModel).toList();
    }

    public static List<BlockedDateDto> fromModelList(List<Amenity.BlockedDate> models) {
        return models == null ? List.of() : models.stream().map(BlockedDateDto::from).toList();
    }
}
