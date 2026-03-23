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
 * Controller para la gestión de productos desde el panel de administración.
 * Todos los endpoints requieren autenticación (MERCHANT o SUPER_ADMIN).
 * El tenantId se resuelve automáticamente del usuario autenticado.
 * SUPER_ADMIN puede pasar tenantId como query param para gestionar cualquier
 * tienda.
 */
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private final ListProductsUseCase listProducts;
    private final GetProductUseCase getProduct;
    private final CreateProductUseCase createProduct;
    private final UpdateProductUseCase updateProduct;
    private final DeleteProductUseCase deleteProduct;
    private final ListCategoriesUseCase listCategories;
    private final CreateCategoryUseCase createCategory;

    @Inject
    public AdminProductController(
            ListProductsUseCase listProducts,
            GetProductUseCase getProduct,
            CreateProductUseCase createProduct,
            UpdateProductUseCase updateProduct,
            DeleteProductUseCase deleteProduct,
            ListCategoriesUseCase listCategories,
            CreateCategoryUseCase createCategory) {
        this.listProducts = listProducts;
        this.getProduct = getProduct;
        this.createProduct = createProduct;
        this.updateProduct = updateProduct;
        this.deleteProduct = deleteProduct;
        this.listCategories = listCategories;
        this.createCategory = createCategory;
    }

    /**
     * Registra las rutas admin de productos en Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/admin/products";
        String catBase = ApiConstants.API_PREFIX + "/admin/categories";

        // GET /api/v1/admin/products — Listar productos del tenant autenticado
        app.get(base, ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));

            String categoryId = ctx.queryParam("category");
            String search = ctx.queryParam("search");
            String statusParam = ctx.queryParam("status");
            int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
            int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);

            // En admin, permitir ver todos los estados (incluido INACTIVE)
            ProductStatus status = null;
            if (statusParam != null && !statusParam.isBlank()) {
                try {
                    status = ProductStatus.valueOf(statusParam.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            ProductFilter filter = new ProductFilter(categoryId, search, status, page, size);
            Page<Product> result = listProducts.execute(tenantId, filter);
            ctx.json(Map.of("data", result));
        });

        // POST /api/v1/admin/products — Crear producto
        app.post(base, ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));

            CreateProductRequest request = ctx.bodyAsClass(CreateProductRequest.class);
            Product product = createProduct.execute(tenantId, request);
            ctx.status(201).json(Map.of("data", product));
        });

        // GET /api/v1/admin/products/:id — Obtener producto por ID
        app.get(base + "/{productId}", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String productId = ctx.pathParam("productId");

            Product product = getProduct.execute(tenantId, productId);
            ctx.json(Map.of("data", product));
        });

        // PUT /api/v1/admin/products/:id — Actualizar producto
        app.put(base + "/{productId}", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String productId = ctx.pathParam("productId");

            UpdateProductRequest request = ctx.bodyAsClass(UpdateProductRequest.class);
            Product product = updateProduct.execute(tenantId, productId, request);
            ctx.json(Map.of("data", product));
        });

        // DELETE /api/v1/admin/products/:id — Soft delete
        app.delete(base + "/{productId}", ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));
            String productId = ctx.pathParam("productId");

            deleteProduct.execute(tenantId, productId);
            ctx.json(Map.of("message", "Producto eliminado"));
        });

        // GET /api/v1/admin/categories — Listar categorías
        app.get(catBase, ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));

            var categories = listCategories.execute(tenantId);
            ctx.json(Map.of("data", categories));
        });

        // POST /api/v1/admin/categories — Crear categoría
        app.post(catBase, ctx -> {
            AuthenticatedUser user = RoleEnforcer.requireRole(ctx, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            String tenantId = resolveTenantId(user, ctx.queryParam("tenantId"));

            var body = ctx.bodyAsClass(com.projectx.backend.application.dto.CreateCategoryRequest.class);
            var category = createCategory.execute(tenantId, body);
            ctx.status(201).json(Map.of("data", category));
        });

        log.info("Admin Product endpoints registrados: {}/admin/products, {}/admin/categories", ApiConstants.API_PREFIX,
                ApiConstants.API_PREFIX);
    }

    /**
     * Resuelve el tenantId: MERCHANT usa su propio tenant, SUPER_ADMIN puede
     * elegir.
     */
    private String resolveTenantId(AuthenticatedUser user, String requestedTenantId) {
        if (user.role() == UserRole.SUPER_ADMIN && requestedTenantId != null && !requestedTenantId.isBlank()) {
            return requestedTenantId;
        }
        return user.tenantId();
    }
}
