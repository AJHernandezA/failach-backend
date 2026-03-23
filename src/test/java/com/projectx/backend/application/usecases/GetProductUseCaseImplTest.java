package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProductUseCaseImplTest {

    @Mock private ProductRepository productRepository;
    private GetProductUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new GetProductUseCaseImpl(productRepository);
    }

    @Test
    void debeRetornarProductoExistente() {
        Product p = new Product("prod-1", TENANT, "Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"),
                "cat-1", "Alimentos", 10, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

        Product result = useCase.execute(TENANT, "prod-1");

        assertEquals("prod-1", result.productId());
        assertEquals("Granola", result.name());
    }

    @Test
    void debeLanzarExcepcionSiNoExiste() {
        when(productRepository.findById(TENANT, "prod-x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "prod-x"));
    }
}
