package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.SearchProductsUseCase;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para el endpoint de búsqueda de productos.
 */
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchProductsUseCase searchProducts;

    @Inject
    public SearchController(SearchProductsUseCase searchProducts) {
        this.searchProducts = searchProducts;
    }

    /**
     * Registra las rutas de búsqueda en la instancia de Javalin.
     */
    public void register(Javalin app) {
        // GET /api/v1/tenants/:tenantId/products/search?q=&category=&minPrice=&maxPrice=&sort=&page=&size=
        app.get(ApiConstants.API_PREFIX + "/tenants/{tenantId}/products/search", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String q = ctx.queryParam("q");
            String category = ctx.queryParam("category");
            String minPriceStr = ctx.queryParam("minPrice");
            String maxPriceStr = ctx.queryParam("maxPrice");
            String sort = ctx.queryParam("sort");
            String pageStr = ctx.queryParam("page");
            String sizeStr = ctx.queryParam("size");

            Integer minPrice = minPriceStr != null ? Integer.parseInt(minPriceStr) : null;
            Integer maxPrice = maxPriceStr != null ? Integer.parseInt(maxPriceStr) : null;
            int page = pageStr != null ? Integer.parseInt(pageStr) : 1;
            int size = sizeStr != null ? Integer.parseInt(sizeStr) : 20;

            SearchProductsRequest request = new SearchProductsRequest(
                    tenantId, q, category, minPrice, maxPrice,
                    SortOption.fromString(sort), page, size);

            SearchResult<Product> result = searchProducts.execute(request);
            ctx.json(Map.of("data", result));
        });

        log.info("Search endpoint registrado en {}/tenants/:tenantId/products/search", ApiConstants.API_PREFIX);
    }
}
