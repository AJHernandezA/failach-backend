package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.LegalContent;

/**
 * Caso de uso para obtener contenido legal de un tenant por tipo.
 */
public interface GetLegalContentUseCase {
    LegalContent execute(String tenantId, String type);
}
