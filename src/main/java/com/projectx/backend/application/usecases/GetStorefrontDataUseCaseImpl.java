package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.GetStorefrontDataUseCase;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.in.ListCategoriesUseCase;
import com.projectx.backend.domain.ports.in.ListProductsUseCase;

import java.util.List;

/**
 * Implementación del caso de uso para obtener datos de la homepage.
 * Agrega tenant config + categorías + 8 productos destacados en un solo request.
 */
public class GetStorefrontDataUseCaseImpl implements GetStorefrontDataUseCase {

    private static final int FEATURED_PRODUCTS_LIMIT = 8;

    private final GetTenantConfigUseCase getTenantConfig;
    private final ListCategoriesUseCase listCategories;
    private final ListProductsUseCase listProducts;

    @Inject
    public GetStorefrontDataUseCaseImpl(
            GetTenantConfigUseCase getTenantConfig,
            ListCategoriesUseCase listCategories,
            ListProductsUseCase listProducts) {
        this.getTenantConfig = getTenantConfig;
        this.listCategories = listCategories;
        this.listProducts = listProducts;
    }

    @Override
    public StorefrontData execute(String tenantId) {
        // 1. Obtener config del tenant (lanza excepción si no existe o está inactivo)
        Tenant tenant = getTenantConfig.execute(tenantId);

        // 2. Obtener categorías
        List<Category> categories = listCategories.execute(tenantId);

        // 3. Obtener productos destacados (los más recientes activos, limitados a 8)
        ProductFilter filter = new ProductFilter(null, null, ProductStatus.ACTIVE, 0, FEATURED_PRODUCTS_LIMIT);
        Page<Product> productsPage = listProducts.execute(tenantId, filter);
        List<Product> featuredProducts = productsPage.items();

        return new StorefrontData(tenant, categories, featuredProducts);
    }
}
