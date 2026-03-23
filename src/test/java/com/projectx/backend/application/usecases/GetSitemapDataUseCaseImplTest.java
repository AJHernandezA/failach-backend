package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.models.*;
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
class GetSitemapDataUseCaseImplTest {

    @Mock private ListCategoriesUseCase listCategories;
    @Mock private ListProductsUseCase listProducts;

    private GetSitemapDataUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new GetSitemapDataUseCaseImpl(listCategories, listProducts);
    }

    @Test
    void debeRetornarEntradasDeSitemap() {
        List<Category> categories = List.of(
                new Category("cat-1", TENANT, "Alimentos", "desc", "img.jpg", 0));

        Product product = new Product("prod-1", TENANT, "Granola", "desc", BigDecimal.valueOf(18500),
                null, List.of("img.jpg"), "cat-1", "Alimentos", 10, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());

        when(listCategories.execute(TENANT)).thenReturn(categories);
        when(listProducts.execute(eq(TENANT), any(ProductFilter.class)))
                .thenReturn(new Page<>(List.of(product), 0, 1000, 1));

        SitemapData data = useCase.execute(TENANT);

        assertNotNull(data);
        // Homepage + 1 categoría + 1 producto = 3 entradas
        assertEquals(3, data.entries().size());

        // Homepage
        assertEquals("/", data.entries().get(0).url());
        assertEquals(1.0, data.entries().get(0).priority());
        assertEquals("daily", data.entries().get(0).changeFrequency());

        // Categoría
        assertEquals("/cat-1", data.entries().get(1).url());
        assertEquals(0.8, data.entries().get(1).priority());

        // Producto
        assertEquals("/product/prod-1", data.entries().get(2).url());
        assertEquals(0.6, data.entries().get(2).priority());
    }

    @Test
    void debeRetornarSoloHomepageSinProductosNiCategorias() {
        when(listCategories.execute(TENANT)).thenReturn(List.of());
        when(listProducts.execute(eq(TENANT), any(ProductFilter.class)))
                .thenReturn(new Page<>(List.of(), 0, 1000, 0));

        SitemapData data = useCase.execute(TENANT);

        assertEquals(1, data.entries().size());
        assertEquals("/", data.entries().get(0).url());
    }
}
