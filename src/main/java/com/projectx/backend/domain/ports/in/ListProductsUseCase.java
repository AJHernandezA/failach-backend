package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductFilter;

public interface ListProductsUseCase {
    Page<Product> execute(String tenantId, ProductFilter filter);
}
