package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Category;
import java.util.List;

public interface ListCategoriesUseCase {
    List<Category> execute(String tenantId);
}
