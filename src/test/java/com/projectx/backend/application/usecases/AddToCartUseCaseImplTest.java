package com.projectx.backend.application.usecases;

import com.projectx.backend.application.dto.AddToCartRequest;
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
class AddToCartUseCaseImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;

    private AddToCartUseCaseImpl useCase;

    private static final String TENANT = "idoneo";
    private static final String SESSION = "session-1";

    private Product activeProduct() {
        return new Product("prod-1", TENANT, "Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"),
                "cat-1", "Alimentos", 10, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());
    }

    @BeforeEach
    void setUp() {
        useCase = new AddToCartUseCaseImpl(cartRepository, productRepository);
    }

    @Test
    void debeAgregarProductoNuevoAlCarrito() {
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(activeProduct()));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

        Cart result = useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-1", 2, null));

        assertEquals(1, result.items().size());
        assertEquals("prod-1", result.items().get(0).productId());
        assertEquals(2, result.items().get(0).quantity());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void debeSumarCantidadSiProductoYaExiste() {
        CartItem existing = new CartItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
        Cart cart = Cart.empty(SESSION, TENANT).withItems(List.of(existing));

        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(activeProduct()));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cart));

        Cart result = useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-1", 3, null));

        assertEquals(1, result.items().size());
        assertEquals(5, result.items().get(0).quantity());
    }

    @Test
    void debeLanzarExcepcionSiProductoNoExiste() {
        when(productRepository.findById(TENANT, "prod-x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-x", 1, null)));
    }

    @Test
    void debeLanzarExcepcionSiProductoInactivo() {
        Product inactive = new Product("prod-1", TENANT, "Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"),
                "cat-1", "Alimentos", 10, List.of(),
                ProductStatus.OUT_OF_STOCK, 0, Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(inactive));

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-1", 1, null)));
    }

    @Test
    void debeLanzarExcepcionSiStockInsuficiente() {
        Product lowStock = new Product("prod-1", TENANT, "Granola", "desc",
                BigDecimal.valueOf(18500), null, List.of("img.jpg"),
                "cat-1", "Alimentos", 2, List.of(),
                ProductStatus.ACTIVE, 0, Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(lowStock));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () ->
                useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-1", 5, null)));
    }

    @Test
    void debeLanzarExcepcionSiCantidadMenorA1() {
        assertThrows(BadRequestException.class, () ->
                useCase.execute(TENANT, SESSION, new AddToCartRequest("prod-1", 0, null)));
    }
}
