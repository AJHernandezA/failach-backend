package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateProductRequest;
import com.projectx.backend.application.dto.UpdateProductRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.*;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para los endpoints de productos.
 * GET son públicos, POST/PUT/DELETE requieren SUPER_ADMIN o MERCHANT.
 */
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;
    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final DeleteProductUseCase deleteProduct;

    @Inject
    public ProductController(
            ListProductsUseCase listProducts,
            GetProductUseCase getProduct,
            CreateProductUseCase createProduct,
            UpdateProductUseCase updateProduct,
            DeleteProductUseCase deleteProduct
    ) {
        this.listProducts = listProducts;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
    }

    /**
     * Registra las rutas de productos en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/products";

        // GET /api/v1/tenants/:tenantId/products — Listar productos (público)
        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String categoryId = ctx.queryParam("category");
            String search = ctx.queryParam("search");
            String statusParam = ctx.queryParam("status");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            ProductStatus status = ProductStatus.ACTIVE;
            if (statusParam != null) {
                try {
                    status = ProductStatus.valueOf(statusParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    // Mantener ACTIVE por defecto
                }
            }

            ProductFilter filter = new ProductFilter(categoryId, search, status, page, size);
            Page<Product> result = listProducts.execute(tenantId, filter);
            ctx.json(Map.of("data", result));
        });

        // GET /api/v1/tenants/:tenantId/products/:productId — Detalle de producto (público)
        app.get(base + "/{productId}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String productId = ctx.pathParam("productId");
            Product product = getProduct.execute(tenantId, productId);
            ctx.json(Map.of("data", product));
        });

        // POST /api/v1/tenants/:tenantId/products — Crear producto (auth requerida)
        app.post(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            RoleEnforcer.requireRoleAndTenant(ctx, tenantId, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            CreateProductRequest request = ctx.bodyAsClass(CreateProductRequest.class);
            Product product = createProduct.execute(tenantId, request);
            ctx.status(201).json(Map.of("data", product));
        });

        // PUT /api/v1/tenants/:tenantId/products/:productId — Actualizar producto (auth requerida)
        app.put(base + "/{productId}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String productId = ctx.pathParam("productId");
            RoleEnforcer.requireRoleAndTenant(ctx, tenantId, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            UpdateProductRequest request = ctx.bodyAsClass(UpdateProductRequest.class);
            Product product = updateProduct.execute(tenantId, productId, request);
            ctx.json(Map.of("data", product));
        });

        // DELETE /api/v1/tenants/:tenantId/products/:productId — Soft delete (auth requerida)
        app.delete(base + "/{productId}", ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            String productId = ctx.pathParam("productId");
            RoleEnforcer.requireRoleAndTenant(ctx, tenantId, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            deleteProduct.execute(tenantId, productId);
            ctx.status(204);
        });

        log.info("Product endpoints registrados: GET/POST/PUT/DELETE {}/tenants/:tenantId/products", ApiConstants.API_PREFIX);
    }
}
