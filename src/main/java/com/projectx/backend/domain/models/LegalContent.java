package com.projectx.backend.domain.models;

import java.time.Instant;

/**
 * Contenido legal de un tenant (términos, privacidad, devoluciones).
 */
public record LegalContent(
    String tenantId,
    String type,
    String title,
    String content,
    Instant updatedAt
) {}
