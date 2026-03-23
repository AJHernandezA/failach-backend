package com.projectx.backend.application.usecases;

import com.projectx.backend.application.dto.UpdateStatusRequest;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private EmailService emailService;
    private UpdateOrderStatusUseCaseImpl useCase;

    private static final String TENANT = "idoneo";
    private static final String CODE = "ORD-ABCD-1234";

    @BeforeEach
    void setUp() {
        useCase = new UpdateOrderStatusUseCaseImpl(orderRepository, tenantRepository, emailService);
    }

    private Order order(OrderStatus status) {
        Instant now = Instant.now();
        return new Order("id-1", CODE, TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 123", "Bogotá", null, null),
                PaymentMethod.WOMPI, PaymentStatus.PAID, status,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(status, now, "Estado inicial")),
                null, now, now);
    }

    @Test
    void shouldTransitionPendingToConfirmed() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.CONFIRMED, "Pago recibido"));
        assertEquals(OrderStatus.CONFIRMED, result.orderStatus());
        assertEquals(2, result.statusHistory().size());
        verify(orderRepository).update(any(Order.class));
    }

    @Test
    void shouldTransitionConfirmedToPreparing() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.PREPARING, null));
        assertEquals(OrderStatus.PREPARING, result.orderStatus());
        verify(orderRepository).update(any(Order.class));
    }

    @Test
    void shouldTransitionPreparingToShipped() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.PREPARING)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.SHIPPED, "Guía #12345"));
        assertEquals(OrderStatus.SHIPPED, result.orderStatus());
    }

    @Test
    void shouldTransitionShippedToDelivered() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.SHIPPED)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.DELIVERED, null));
        assertEquals(OrderStatus.DELIVERED, result.orderStatus());
    }

    @Test
    void shouldAllowCancelFromPending() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.CANCELLED, "Sin pago"));
        assertEquals(OrderStatus.CANCELLED, result.orderStatus());
    }

    @Test
    void shouldAllowCancelFromConfirmed() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED)));
        Order result = useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.CANCELLED, null));
        assertEquals(OrderStatus.CANCELLED, result.orderStatus());
    }

    @Test
    void shouldThrowOnInvalidTransitionPendingToPreparing() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.PENDING)));
        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.PREPARING, null)));
        assertTrue(ex.getMessage().contains("PENDING"));
        assertTrue(ex.getMessage().contains("PREPARING"));
    }

    @Test
    void shouldThrowOnInvalidTransitionDeliveredToAnything() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.DELIVERED)));
        assertThrows(BusinessRuleException.class,
                () -> useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.CANCELLED, null)));
    }

    @Test
    void shouldThrowOnInvalidTransitionCancelledToAnything() {
        when(orderRepository.findByCode(TENANT, CODE)).thenReturn(Optional.of(order(OrderStatus.CANCELLED)));
        assertThrows(BusinessRuleException.class,
                () -> useCase.execute(new UpdateStatusRequest(TENANT, CODE, OrderStatus.CONFIRMED, null)));
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findByCode(TENANT, "ORD-XXXX")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> useCase.execute(new UpdateStatusRequest(TENANT, "ORD-XXXX", OrderStatus.CONFIRMED, null)));
    }
}
