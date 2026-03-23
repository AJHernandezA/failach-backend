package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.TenantNotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.in.ListCategoriesUseCase;
import com.projectx.backend.domain.ports.in.ListProductsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStorefrontDataUseCaseImplTest {

    @Mock
    private GetTenantConfigUseCase getTenantConfig;
    @Mock
    private ListCategoriesUseCase listCategories;
    @Mock
    private ListProductsUseCase listProducts;

    private GetStorefrontDataUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new GetStorefrontDataUseCaseImpl(getTenantConfig, listCategories, listProducts);
    }

    @Test
    void debeRetornarStorefrontDataCompleta() {
        Tenant tenant = new Tenant(TENANT, "Idoneo", "desc", "logo.png", null, "banner.png",
                new TenantColors("#000", "#666", "#ff6600", "#fff", "#1a1a1a"),
                "Inter", List.of(), List.of("Bogotá"), "573001234567", "test@test.com",
                "3001234567", "Calle 1", "Lun-Vie 9-6", null, true,
                null, null, null, null, Instant.now(), Instant.now());

        List<Category> categories = List.of(
                new Category("cat-1", TENANT, "Alimentos", "desc", "img.jpg", 0));

        Product product = new Product("prod-1", TENANT, "Granola", "desc", BigDecimal.valueOf(18500),
                null, List.of("img.jpg"), "cat-1", "Alimentos", 10, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());

        when(getTenantConfig.execute(TENANT)).thenReturn(tenant);
        when(listCategories.execute(TENANT)).thenReturn(categories);
        when(listProducts.execute(eq(TENANT), any(ProductFilter.class)))
                .thenReturn(new Page<>(List.of(product), 0, 8, 1));

        StorefrontData data = useCase.execute(TENANT);

        assertNotNull(data);
        assertEquals(TENANT, data.tenant().tenantId());
        assertEquals(1, data.categories().size());
        assertEquals(1, data.featuredProducts().size());
        assertEquals("Granola", data.featuredProducts().get(0).name());
    }

    @Test
    void debeFallarConTenantInexistente() {
        when(getTenantConfig.execute("no-existe")).thenThrow(new TenantNotFoundException("no-existe"));

        assertThrows(TenantNotFoundException.class, () -> useCase.execute("no-existe"));
    }
}
