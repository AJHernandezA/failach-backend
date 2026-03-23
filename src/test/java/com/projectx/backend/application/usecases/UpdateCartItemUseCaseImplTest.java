package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.CartRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCartItemUseCaseImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    private UpdateCartItemUseCaseImpl useCase;

    private static final String TENANT = "idoneo";
    private static final String SESSION = "session-1";

    @BeforeEach
    void setUp() {
        useCase = new UpdateCartItemUseCaseImpl(cartRepository, productRepository);
    }

    private Cart cartWithItem() {
        CartItem item = new CartItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
        return Cart.empty(SESSION, TENANT).withItems(List.of(item));
    }

    @Test
    void debeActualizarCantidad() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItem()));
        Product p = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 10, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

        Cart result = useCase.execute(TENANT, SESSION, "prod-1", 5);

        assertEquals(5, result.items().get(0).quantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void debeLanzarExcepcionSiCarritoNoExiste() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-1", 3));
    }

    @Test
    void debeLanzarExcepcionSiProductoNoEstaEnCarrito() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItem()));

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-x", 3));
    }

    @Test
    void debeLanzarExcepcionSiStockInsuficiente() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cartWithItem()));
        Product p = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 3, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

        assertThrows(BusinessRuleException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-1", 10));
    }

    @Test
    void debeLanzarExcepcionSiCantidadMenorA1() {
        assertThrows(BadRequestException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-1", 0));
    }
}
