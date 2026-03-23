package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.models.CartItem;
import com.projectx.backend.domain.ports.out.CartRepository;
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
class RemoveCartItemUseCaseImplTest {

    @Mock private CartRepository cartRepository;
    private RemoveCartItemUseCaseImpl useCase;

    private static final String TENANT = "idoneo";
    private static final String SESSION = "session-1";

    @BeforeEach
    void setUp() {
        useCase = new RemoveCartItemUseCaseImpl(cartRepository);
    }

    @Test
    void debeEliminarProductoDelCarrito() {
        CartItem item1 = new CartItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
        CartItem item2 = new CartItem("prod-2", "Jugo", BigDecimal.valueOf(12000), 1, "img2.jpg", null, null);
        Cart cart = Cart.empty(SESSION, TENANT).withItems(List.of(item1, item2));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cart));

        Cart result = useCase.execute(TENANT, SESSION, "prod-1");

        assertEquals(1, result.items().size());
        assertEquals("prod-2", result.items().get(0).productId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void debeLanzarExcepcionSiCarritoNoExiste() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-1"));
    }

    @Test
    void debeLanzarExcepcionSiProductoNoEstaEnCarrito() {
        CartItem item = new CartItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
        Cart cart = Cart.empty(SESSION, TENANT).withItems(List.of(item));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cart));

        assertThrows(NotFoundException.class, () ->
                useCase.execute(TENANT, SESSION, "prod-x"));
    }
}
