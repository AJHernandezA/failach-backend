package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.models.CartItem;
import com.projectx.backend.domain.ports.out.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCartUseCaseImplTest {

    @Mock private CartRepository cartRepository;
    private GetCartUseCaseImpl useCase;

    private static final String TENANT = "idoneo";
    private static final String SESSION = "session-1";

    @BeforeEach
    void setUp() {
        useCase = new GetCartUseCaseImpl(cartRepository);
    }

    @Test
    void debeRetornarCarritoExistente() {
        CartItem item = new CartItem("prod-1", "Granola", java.math.BigDecimal.valueOf(18500), 2, "img.jpg", null, null);
        Cart cart = Cart.empty(SESSION, TENANT).withItems(List.of(item));
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.of(cart));

        Cart result = useCase.execute(TENANT, SESSION);

        assertEquals(1, result.items().size());
        assertEquals(2, result.itemCount());
    }

    @Test
    void debeRetornarCarritoVacioSiNoExiste() {
        when(cartRepository.findById(TENANT, SESSION)).thenReturn(Optional.empty());

        Cart result = useCase.execute(TENANT, SESSION);

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.itemCount());
        assertEquals(TENANT, result.tenantId());
    }
}
