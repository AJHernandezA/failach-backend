package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.NotFoundException;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.PaymentService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitiateWompiPaymentUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentService paymentService;
    private InitiateWompiPaymentUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    private Order wompiOrder(PaymentStatus paymentStatus) {
        Instant now = Instant.now();
        return new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(new OrderItem("prod-1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING,
                new DeliveryInfo("Calle 123", "Bogotá", null, null),
                PaymentMethod.WOMPI, paymentStatus, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(new StatusHistory(OrderStatus.PENDING, now, "Creada")),
                null, now, now);
    }

    @BeforeEach
    void setUp() {
        useCase = new InitiateWompiPaymentUseCaseImpl(orderRepository, paymentService);
    }

    @Test
    void debeIniciarPagoWompiExitosamente() {
        Order order = wompiOrder(PaymentStatus.PENDING);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(order));

        PaymentInitData initData = new PaymentInitData("PX-idoneo-ORD-ABCD-1234", 3700000, "COP", "sha256sig",
                "pub_key", "http://redirect");
        when(paymentService.initiate(eq(TENANT), any(Order.class), eq("http://redirect"))).thenReturn(initData);

        PaymentInitData result = useCase.execute(TENANT, "ORD-ABCD-1234", "http://redirect");

        assertEquals("PX-idoneo-ORD-ABCD-1234", result.reference());
        assertEquals(3700000, result.amountInCents());
    }

    @Test
    void debeFallarSiOrdenNoExiste() {
        when(orderRepository.findByCode(TENANT, "ORD-XXXX")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.execute(TENANT, "ORD-XXXX", "http://redirect"));
    }

    @Test
    void debeFallarSiMetodoPagoNoEsWompi() {
        Instant now = Instant.now();
        Order cashOrder = new Order("id-1", "ORD-ABCD-1234", TENANT,
                new Customer("Juan", "j@e.co", "+573001234567"),
                List.of(), DeliveryMethod.PICKUP, null,
                PaymentMethod.CASH_ON_DELIVERY, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, now, now);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(cashOrder));

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", "http://redirect"));
    }

    @Test
    void debeFallarSiOrdenYaPagada() {
        Order paidOrder = wompiOrder(PaymentStatus.PAID);
        when(orderRepository.findByCode(TENANT, "ORD-ABCD-1234")).thenReturn(Optional.of(paidOrder));

        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, "ORD-ABCD-1234", "http://redirect"));
    }
}
