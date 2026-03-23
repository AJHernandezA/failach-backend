package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.BusinessRuleException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.OrderRepository;
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
class ConfirmManualPaymentUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    private ConfirmManualPaymentUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new ConfirmManualPaymentUseCaseImpl(orderRepository, productRepository);
    }

    private Order bankTransferOrder(PaymentStatus paymentStatus) {
        Instant now = Instant.now();
        return new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 123", "Bogotá", null, null),
                PaymentMethod.BANK_TRANSFER, paymentStatus, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(OrderStatus.PENDING, now, "Creada")),
                null, now, now);
    }

    @Test
    void shouldConfirmBankTransferAndDeductStock() {
        Order order = bankTransferOrder(PaymentStatus.PENDING);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(order));

        Product product = new Product("prod-1", TENANT, "Granola", "", BigDecimal.valueOf(18500), null,
                List.of("img.jpg"), "cat-1", "Alimentos", 10, List.of(), ProductStatus.ACTIVE, 0,
                Instant.now(), Instant.now());
        when(productRepository.findById(TENANT, "prod-1")).thenReturn(Optional.of(product));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234", "Transferencia verificada");

        assertEquals(PaymentStatus.PAID, result.paymentStatus());
        assertEquals(OrderStatus.CONFIRMED, result.orderStatus());
        verify(orderRepository).update(any(Order.class));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldConfirmCashOnDeliveryWithoutDeductingStock() {
        Instant now = Instant.now();
        Order cashOrder = new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 123", "Bogotá", null, null),
                PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, now, now);

        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(cashOrder));

        Order result = useCase.execute(TENANT, "ORD-ABCD-1234", null);

        assertEquals(PaymentStatus.PAID, result.paymentStatus());
        verify(orderRepository).update(any(Order.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findByCode(TENANT, "ORD-XXXX")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "ORD-XXXX", null));
    }

    @Test
    void shouldThrowWhenOrderAlreadyPaid() {
        Order paidOrder = bankTransferOrder(PaymentStatus.PAID);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(paidOrder));

        assertThrows(BusinessRuleException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", null));
    }

    @Test
    void shouldThrowWhenPaymentMethodIsWompi() {
        Instant now = Instant.now();
        Order wompiOrder = new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(), DeliveryMethod.PICKUP, null,
                PaymentMethod.WOMPI, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, now, now);

        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(wompiOrder));

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", null));
    }
}
