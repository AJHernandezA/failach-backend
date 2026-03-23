package com.projectx.backend.domain.ports.in;

import com.projectx.backend.application.dto.CreateCategoryRequest;
import com.projectx.backend.domain.models.Category;

public interface CreateCategoryUseCase {
    Category execute(String tenantId, CreateCategoryRequest request);
}
