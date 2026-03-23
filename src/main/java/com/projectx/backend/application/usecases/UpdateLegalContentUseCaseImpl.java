package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.ports.in.UpdateLegalContentUseCase;
import com.projectx.backend.domain.ports.out.LegalContentRepository;

import java.time.Instant;
import java.util.Set;

/**
 * Implementación del caso de uso para actualizar contenido legal de un tenant.
 */
public class UpdateLegalContentUseCaseImpl implements UpdateLegalContentUseCase {

    private static final Set<String> VALID_TYPES = Set.of("terms", "privacy", "returns");

    private final LegalContentRepository legalContentRepository;

    @Inject
    public UpdateLegalContentUseCaseImpl(LegalContentRepository legalContentRepository) {
        this.legalContentRepository = legalContentRepository;
    }

    @Override
    public LegalContent execute(String tenantId, String type, String title, String content) {
        if (!VALID_TYPES.contains(type)) {
            throw new BadRequestException("Tipo de contenido legal inválido: " + type);
        }
        if (title == null || title.isBlank()) {
            throw new BadRequestException("El título es requerido");
        }
        if (content == null || content.isBlank()) {
            throw new BadRequestException("El contenido es requerido");
        }

        LegalContent legal = new LegalContent(tenantId, type, title.trim(), content.trim(), Instant.now());
        legalContentRepository.save(legal);
        return legal;
    }
}
