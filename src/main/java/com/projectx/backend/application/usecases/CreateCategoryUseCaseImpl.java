package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateCategoryRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.ports.in.CreateCategoryUseCase;
import com.projectx.backend.domain.ports.out.CategoryRepository;

import java.util.UUID;

/**
 * Implementación del caso de uso para crear una categoría.
 */
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    @Inject
    public CreateCategoryUseCaseImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category execute(String tenantId, CreateCategoryRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Nombre de la categoría es requerido");
        }

        Category category = new Category(
                UUID.randomUUID().toString(),
                tenantId,
                request.name(),
                request.description() != null ? request.description() : "",
                request.imageUrl() != null ? request.imageUrl() : "",
                request.sortOrder()
        );

        categoryRepository.save(category);
        return category;
    }
}
