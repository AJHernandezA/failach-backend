package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.GetSitemapDataUseCase;
import com.projectx.backend.domain.ports.in.ListCategoriesUseCase;
import com.projectx.backend.domain.ports.in.ListProductsUseCase;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del caso de uso para generar datos de sitemap.
 * Agrega homepage, categorías y productos activos del tenant.
 */
public class GetSitemapDataUseCaseImpl implements GetSitemapDataUseCase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ListCategoriesUseCase listCategories;
    private final ListProductsUseCase listProducts;

    @Inject
    public GetSitemapDataUseCaseImpl(ListCategoriesUseCase listCategories,
                                     ListProductsUseCase listProducts) {
        this.listCategories = listCategories;
        this.listProducts = listProducts;
    }

    @Override
    public SitemapData execute(String tenantId) {
        List<SitemapEntry> entries = new ArrayList<>();

        // Homepage
        entries.add(new SitemapEntry("/", DATE_FMT.format(Instant.now()), "daily", 1.0));

        // Categorías
        List<Category> categories = listCategories.execute(tenantId);
        for (Category cat : categories) {
            entries.add(new SitemapEntry(
                    "/" + cat.categoryId(),
                    DATE_FMT.format(Instant.now()),
                    "weekly",
                    0.8
            ));
        }

        // Productos activos (todos, sin límite de paginación)
        ProductFilter filter = new ProductFilter(null, null, ProductStatus.ACTIVE, 0, 1000);
        Page<Product> products = listProducts.execute(tenantId, filter);
        for (Product p : products.items()) {
            String lastMod = p.updatedAt() != null ? DATE_FMT.format(p.updatedAt()) : DATE_FMT.format(Instant.now());
            entries.add(new SitemapEntry(
                    "/product/" + p.productId(),
                    lastMod,
                    "weekly",
                    0.6
            ));
        }

        return new SitemapData(entries);
    }
}
