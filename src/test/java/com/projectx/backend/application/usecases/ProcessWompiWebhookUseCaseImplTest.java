package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.PaymentService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessWompiWebhookUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ProductRepository productRepository;
    private ProcessWompiWebhookUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    private Order pendingOrder() {
        Instant now = Instant.now();
        return new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 123", "Bogotá", null, null),
                PaymentMethod.WOMPI, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(OrderStatus.PENDING, Instant.now(), "Creada")),
                null, now, now);
    }

    @BeforeEach
    void setUp() {
        useCase = new ProcessWompiWebhookUseCaseImpl(orderRepository, paymentService, productRepository);
    }

    @Test
    void debeAprobarPagoYActualizarOrden() {
        WompiWebhookEvent event = new WompiWebhookEvent(
                "transaction.updated", "valid-sig", System.currentTimeMillis(),
                "txn-123", "APPROVED", "PX-idoneo-ORD-ABCD-1234", 3700000);

        when(paymentService.verifyWebhookSignature("txn-123", "APPROVED", 3700000, "valid-sig")).thenReturn(true);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(pendingOrder()));

        Product p = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 10, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(p));

        useCase.execute(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).update(captor.capture());
        Order updated = captor.getValue();
        assertEquals(PaymentStatus.PAID, updated.paymentStatus());
        assertEquals(OrderStatus.CONFIRMED, updated.orderStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void debeRechazarPagoYActualizarOrden() {
        WompiWebhookEvent event = new WompiWebhookEvent(
                "transaction.updated", "valid-sig", System.currentTimeMillis(),
                "txn-123", "DECLINED", "PX-idoneo-ORD-ABCD-1234", 3700000);

        when(paymentService.verifyWebhookSignature("txn-123", "DECLINED", 3700000, "valid-sig")).thenReturn(true);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(pendingOrder()));

        useCase.execute(event);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).update(captor.capture());
        assertEquals(PaymentStatus.FAILED, captor.getValue().paymentStatus());
    }

    @Test
    void debeFallarConFirmaInvalida() {
        WompiWebhookEvent event = new WompiWebhookEvent(
                "transaction.updated", "bad-sig", System.currentTimeMillis(),
                "txn-123", "APPROVED", "PX-idoneo-ORD-ABCD-1234", 3700000);

        when(paymentService.verifyWebhookSignature("txn-123", "APPROVED", 3700000, "bad-sig")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> useCase.execute(event));
    }

    @Test
    void debeIgnorarWebhookDuplicadoOrdenYaPagada() {
        Instant now = Instant.now();
        Order paidOrder = new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(), DeliveryMethod.PICKUP, null,
                PaymentMethod.WOMPI, PaymentStatus.PAID, OrderStatus.CONFIRMED,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, now, now);

        WompiWebhookEvent event = new WompiWebhookEvent(
                "transaction.updated", "valid-sig", System.currentTimeMillis(),
                "txn-123", "APPROVED", "PX-idoneo-ORD-ABCD-1234", 3700000);

        when(paymentService.verifyWebhookSignature("txn-123", "APPROVED", 3700000, "valid-sig")).thenReturn(true);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(paidOrder));

        useCase.execute(event);

        verify(orderRepository, never()).update(any());
    }
}
