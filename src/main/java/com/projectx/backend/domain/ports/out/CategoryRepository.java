package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Category;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de categorías.
 */
public interface CategoryRepository {

    List<Category> findByTenant(String tenantId);

    Optional<Category> findById(String tenantId, String categoryId);

    void save(Category category);
}
