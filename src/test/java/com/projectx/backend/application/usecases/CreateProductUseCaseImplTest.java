package com.projectx.backend.application.usecases;

import com.projectx.backend.application.dto.CreateProductRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Category;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProductUseCaseImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    private CreateProductUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new CreateProductUseCaseImpl(productRepository, categoryRepository);
    }

    private CreateProductRequest validRequest() {
        return new CreateProductRequest("Granola", "desc", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", 10, List.of(), 0);
    }

    @Test
    void debeCrearProductoExitosamente() {
        when(categoryRepository.findById(TENANT, "cat-1"))
                .thenReturn(Optional.of(new Category("cat-1", TENANT, "Alimentos", "", "", 0)));

        Product result = useCase.execute(TENANT, validRequest());

        assertNotNull(result.productId());
        assertEquals("Granola", result.name());
        assertEquals(ProductStatus.ACTIVE, result.status());
        assertEquals("Alimentos", result.categoryName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void debeSetearOutOfStockSiStockCero() {
        when(categoryRepository.findById(TENANT, "cat-1"))
                .thenReturn(Optional.of(new Category("cat-1", TENANT, "Alimentos", "", "", 0)));

        CreateProductRequest req = new CreateProductRequest("Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"), "cat-1", 0, List.of(), 0);

        Product result = useCase.execute(TENANT, req);

        assertEquals(ProductStatus.OUT_OF_STOCK, result.status());
    }

    @Test
    void debeFallarSiNombreVacio() {
        CreateProductRequest req = new CreateProductRequest("", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"), "cat-1", 10, List.of(), 0);

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, req));
    }

    @Test
    void debeFallarSiPrecioInvalido() {
        CreateProductRequest req = new CreateProductRequest("Granola", "desc",
                BigDecimal.ZERO, null, List.of("img.jpg"), "cat-1", 10, List.of(), 0);

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, req));
    }

    @Test
    void debeFallarSiSinImagenes() {
        CreateProductRequest req = new CreateProductRequest("Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of(), "cat-1", 10, List.of(), 0);

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, req));
    }

    @Test
    void debeFallarSiCategoriaNoExiste() {
        when(categoryRepository.findById(TENANT, "cat-x")).thenReturn(Optional.empty());

        CreateProductRequest req = new CreateProductRequest("Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"), "cat-x", 10, List.of(), 0);

        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, req));
    }

    @Test
    void debeFallarSiStockNegativo() {
        CreateProductRequest req = new CreateProductRequest("Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"), "cat-1", -1, List.of(), 0);

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, req));
    }
}
