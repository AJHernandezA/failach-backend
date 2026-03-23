package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.ports.in.ListCategoriesUseCase;
import com.projectx.backend.domain.ports.out.CategoryRepository;

import java.util.List;

/**
 * Implementación del caso de uso para listar categorías de un tenant.
 */
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    @Inject
    public ListCategoriesUseCaseImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> execute(String tenantId) {
        return categoryRepository.findByTenant(tenantId);
    }
}
