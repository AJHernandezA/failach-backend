package com.projectx.backend.application.usecases;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListOrdersUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    private ListOrdersUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListOrdersUseCaseImpl(orderRepository);
    }

    @Test
    void shouldReturnPaginatedOrders() {
        OrderFilter filter = new OrderFilter(null, null, 0, 20);
        Instant now = Instant.now();
        Order o1 = new Order("id-1", "ORD-0001", "idoneo",
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(), DeliveryMethod.PICKUP, null,
                PaymentMethod.WOMPI, PaymentStatus.PAID, OrderStatus.CONFIRMED,
                BigDecimal.valueOf(50000), BigDecimal.ZERO, BigDecimal.valueOf(50000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, now, now);

        when(orderRepository.findByTenant("idoneo", filter))
                .thenReturn(new Page<>(List.of(o1), 0, 20, 1));

        Page<Order> result = useCase.execute("idoneo", filter);

        assertEquals(1, result.items().size());
        assertEquals(1, result.total());
        assertEquals(0, result.page());
    }

    @Test
    void shouldReturnEmptyPageWhenNoOrders() {
        OrderFilter filter = new OrderFilter(OrderStatus.PENDING, null, 0, 20);
        when(orderRepository.findByTenant("idoneo", filter))
                .thenReturn(new Page<>(List.of(), 0, 20, 0));

        Page<Order> result = useCase.execute("idoneo", filter);

        assertTrue(result.items().isEmpty());
        assertEquals(0, result.total());
    }
}
