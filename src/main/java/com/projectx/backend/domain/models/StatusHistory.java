package com.projectx.backend.domain.models;

import java.time.Instant;

/**
 * Entrada del historial de estados de una orden.
 *
 * @param status    estado al que cambió
 * @param timestamp momento del cambio
 * @param note      nota opcional sobre el cambio
 */
public record StatusHistory(
        OrderStatus status,
        Instant timestamp,
        String note
) {}
