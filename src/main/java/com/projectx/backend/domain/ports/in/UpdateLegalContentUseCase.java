package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.LegalContent;

/**
 * Caso de uso para actualizar contenido legal de un tenant.
 */
public interface UpdateLegalContentUseCase {
    LegalContent execute(String tenantId, String type, String title, String content);
}
