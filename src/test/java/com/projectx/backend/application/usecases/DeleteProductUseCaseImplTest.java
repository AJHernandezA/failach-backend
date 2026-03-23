package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductStatus;
import com.projectx.backend.domain.ports.out.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProductUseCaseImplTest {

    @Mock private ProductRepository productRepository;
    private DeleteProductUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new DeleteProductUseCaseImpl(productRepository);
    }

    @Test
    void debeSoftDeleteProducto() {
        Product p = new Product("prod-1", TENANT, "Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"),
                "cat-1", "Alimentos", 10, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

        useCase.execute(TENANT, "prod-1");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(ProductStatus.INACTIVE, captor.getValue().status());
    }

    @Test
    void debeLanzarExcepcionSiNoExiste() {
        when(productRepository.findById(TENANT, "prod-x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "prod-x"));
    }
}
