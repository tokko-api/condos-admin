package com.condos.board.api.dto;

import com.condos.board.model.Amenity;

import java.time.LocalDate;
import java.util.List;

/**
 * Regla de bloqueo recurrente: a partir de anchorDate (su día de la semana
 * es el que se repite), cada intervalWeeks semanas, hasta `until` si se indica.
 * Ej. anchorDate=un martes, intervalWeeks=1 -> "todos los martes";
 * intervalWeeks=2 -> "cada 2 semanas ese martes".
 */
public record RecurringBlockDto(LocalDate anchorDate, Integer intervalWeeks, LocalDate until, String reason) {

    public Amenity.RecurringBlock toModel() {
        return new Amenity.RecurringBlock(anchorDate, intervalWeeks, until, reason);
    }

    public static RecurringBlockDto from(Amenity.RecurringBlock rb) {
        return new RecurringBlockDto(rb.getAnchorDate(), rb.getIntervalWeeks(), rb.getUntil(), rb.getReason());
    }

    public static List<Amenity.RecurringBlock> toModelList(List<RecurringBlockDto> dtos) {
        return dtos == null ? List.of() : dtos.stream().map(RecurringBlockDto::toModel).toList();
    }

    public static List<RecurringBlockDto> fromModelList(List<Amenity.RecurringBlock> models) {
        return models == null ? List.of() : models.stream().map(RecurringBlockDto::from).toList();
    }
}
