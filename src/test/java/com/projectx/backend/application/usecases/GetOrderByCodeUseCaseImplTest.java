package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.OrderRepository;
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
class GetOrderByCodeUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    private GetOrderByCodeUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new GetOrderByCodeUseCaseImpl(orderRepository);
    }

    @Test
    void debeRetornarOrdenPorCodigo() {
        Instant now = Instant.now();
        Order order = new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 1, "img.jpg", null, null)),
                DeliveryMethod.PICKUP, null, PaymentMethod.WOMPI,
                PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(18500), BigDecimal.ZERO, BigDecimal.valueOf(18500),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(OrderStatus.PENDING, now, "Creada")),
                null, now, now);

        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(order));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234");

        assertEquals("ORD-ABCD-1234", result.orderCode());
        assertEquals(TENANT, result.tenantId());
    }

    @Test
    void debeLanzarExcepcionSiOrdenNoExiste() {
        when(orderRepository.findByCode(TENANT, "ORD-XXXX-0000")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "ORD-XXXX-0000"));
    }
}
