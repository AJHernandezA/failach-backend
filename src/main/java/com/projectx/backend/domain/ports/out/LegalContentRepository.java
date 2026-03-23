package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.LegalContent;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de contenido legal.
 */
public interface LegalContentRepository {
    Optional<LegalContent> findByType(String tenantId, String type);
    void save(LegalContent content);
}
