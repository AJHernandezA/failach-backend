package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.CreateCategoryRequest;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.models.UserRole;
import com.projectx.backend.domain.ports.in.CreateCategoryUseCase;
import com.projectx.backend.domain.ports.in.ListCategoriesUseCase;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller para los endpoints de categorías.
 * GET es público, POST requiere SUPER_ADMIN o MERCHANT.
 */
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final ListCategoriesUseCase listCategories;
    private final CreateCategoryUseCase createCategory;

    @Inject
    public CategoryController(ListCategoriesUseCase listCategories, CreateCategoryUseCase createCategory) {
        this.listCategories = listCategories;
        this.createCategory = createCategory;
    }

    /**
     * Registra las rutas de categorías en la instancia de Javalin.
     */
    public void register(Javalin app) {
        String base = ApiConstants.API_PREFIX + "/tenants/{tenantId}/categories";

        // GET /api/v1/tenants/:tenantId/categories — Listar categorías (público)
        app.get(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            List<Category> categories = listCategories.execute(tenantId);
            ctx.json(Map.of("data", categories));
        });

        // POST /api/v1/tenants/:tenantId/categories — Crear categoría (auth requerida)
        app.post(base, ctx -> {
            String tenantId = ctx.pathParam("tenantId");
            RoleEnforcer.requireRoleAndTenant(ctx, tenantId, UserRole.SUPER_ADMIN, UserRole.MERCHANT);
            CreateCategoryRequest request = ctx.bodyAsClass(CreateCategoryRequest.class);
            Category category = createCategory.execute(tenantId, request);
            ctx.status(201).json(Map.of("data", category));
        });

        log.info("Category endpoints registrados: GET/POST {}/tenants/:tenantId/categories", ApiConstants.API_PREFIX);
    }
}
