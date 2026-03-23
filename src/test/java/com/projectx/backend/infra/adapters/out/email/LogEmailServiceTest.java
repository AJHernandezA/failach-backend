package com.projectx.backend.infra.adapters.out.email;

import com.projectx.backend.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Tests para LogEmailService.
 * Verifica que todos los métodos se ejecutan sin errores (solo loguean).
 */
class LogEmailServiceTest {

    private LogEmailService service;
    private Tenant tenant;
    private Order order;

    @BeforeEach
    void setUp() {
        service = new LogEmailService();
        tenant = new Tenant("test", "Test Store", null, null, null, null,
                new TenantColors("#ff0000", null, null, null, null),
                null, List.of(), List.of("Bogotá"), "+573001111111", null, null, null, null,
                new BankInfo("Banco", "Ahorros", "123", "Titular", "CC", "999"),
                true, null, null, null, null, Instant.now(), Instant.now());
        order = new Order("id-1", "ORD-LOG-0001", "test",
                new Customer("Juan", "juan@test.com", "+573002222222"),
                List.of(new OrderItem("p1", "Producto", BigDecimal.valueOf(10000), 1, "img.jpg", null, null)),
                DeliveryMethod.PICKUP, null, PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(10000), BigDecimal.ZERO, BigDecimal.valueOf(10000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, Instant.now(), Instant.now());
    }

    @Test
    void debeLoguearConfirmacionSinError() {
        assertDoesNotThrow(() -> service.sendOrderConfirmation(tenant, order));
    }

    @Test
    void debeLoguearInstruccionesDePagoSinError() {
        assertDoesNotThrow(() -> service.sendPaymentInstructions(tenant, order));
    }

    @Test
    void debeLoguearActualizacionDeEstadoSinError() {
        assertDoesNotThrow(() -> service.sendStatusUpdate(tenant, order, OrderStatus.CONFIRMED));
    }

    @Test
    void debeLoguearCancelacionSinError() {
        assertDoesNotThrow(() -> service.sendOrderCancellation(tenant, order, "Sin stock"));
    }
}
