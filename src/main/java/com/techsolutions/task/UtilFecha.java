/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.techsolutions.task;

/**
 *
 * @author Luis
 */

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/*
 * Utilidad muy defensiva para tratar de parsear fechas en formato ISO 8601 u otros comunes.
 * Si no puede parsear, retorna null.
 */
public class UtilFecha {

    public static Long parseToEpochMillis(String fecha) {
        if (fecha == null || fecha.isEmpty()) return null;

        // Intento directo como Instant (ej: 2025-07-01T10:00:00Z)
        try {
            Instant inst = Instant.parse(fecha);
            return inst.toEpochMilli();
        } catch (DateTimeParseException ignored) {}

        // Intento con OffsetDateTime
        try {
            OffsetDateTime odt = OffsetDateTime.parse(fecha, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {}

        // Intento con LocalDateTime asumiendo UTC
        try {
            LocalDateTime ldt = LocalDateTime.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeParseException ignored) {}

        // Intento con solo fecha
        try {
            LocalDate ld = LocalDate.parse(fecha, DateTimeFormatter.ISO_LOCAL_DATE);
            return ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {}

        return null;
    }
}

