package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.OrderRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelOrderUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    private CancelOrderUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new CancelOrderUseCaseImpl(orderRepository, productRepository);
    }

    private Order order(PaymentMethod pm, PaymentStatus ps, OrderStatus os) {
        Instant now = Instant.now();
        return new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 123", "Bogotá", null, null),
                pm, ps, os,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(os, Instant.now(), "Estado inicial")),
                null, now, now);
    }

    @Test
    void shouldCancelPendingBankTransferWithoutRestoringStock() {
        // Transferencia pendiente: stock NO fue descontado → no restaurar
        Order pending = order(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, OrderStatus.PENDING);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(pending));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234", "No pagó");

        assertEquals(OrderStatus.CANCELLED, result.orderStatus());
        verify(orderRepository).update(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldCancelPaidOrderAndRestoreStock() {
        // Orden pagada: stock SÍ fue descontado → restaurar
        Order paid = order(PaymentMethod.BANK_TRANSFER, PaymentStatus.PAID, OrderStatus.CONFIRMED);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(paid));

        Product product = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 8, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(product));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234", "Cliente desistió");

        assertEquals(OrderStatus.CANCELLED, result.orderStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldCancelCashOnDeliveryAndRestoreStock() {
        // Efectivo contraentrega: stock SÍ fue descontado al crear → restaurar
        Order cashOrder = order(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, OrderStatus.PENDING);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(cashOrder));

        Product product = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 8, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(product));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234", "Cancelado");

        assertEquals(OrderStatus.CANCELLED, result.orderStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findByCode(TENANT, "ORD-XXXX")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "ORD-XXXX", null));
    }

    @Test
    void shouldThrowWhenOrderIsDelivered() {
        Order delivered = order(PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PAID, OrderStatus.DELIVERED);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(delivered));

        assertThrows(BusinessRuleException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", "Cancelar"));
    }

    @Test
    void shouldThrowWhenOrderAlreadyCancelled() {
        Order cancelled = order(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, OrderStatus.CANCELLED);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(cancelled));

        assertThrows(BusinessRuleException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", "Cancelar otra vez"));
    }
}
