package com.projectx.backend.infra.adapters.out.email;

import com.projectx.backend.domain.models.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el motor de templates de email.
 * Verifica carga de templates, reemplazo de placeholders y generación de HTML
 * de items.
 */
class EmailTemplateEngineTest {

    private EmailTemplateEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EmailTemplateEngine();
    }

    @Test
    void debeRenderizarTemplateCargadoConPlaceholders() {
        String html = engine.render("order-confirmation.html", Map.ofEntries(
                entry("tenantName", "Mi Tienda"),
                entry("tenantLogo", "https://example.com/logo.png"),
                entry("primaryColor", "#ff5500"),
                entry("orderCode", "ORD-TEST-1234"),
                entry("customerName", "Juan Pérez"),
                entry("total", "$37.000"),
                entry("subtotal", "$37.000"),
                entry("shippingCost", "$0"),
                entry("trackingUrl", "https://mitienda.projectx.com/order/ORD-TEST-1234"),
                entry("whatsappUrl", "https://wa.me/573001234567"),
                entry("items", "<tr><td>Granola</td><td>2</td><td>$37.000</td></tr>")));

        assertFalse(html.isEmpty(), "El template no debería estar vacío");
        assertTrue(html.contains("Mi Tienda"), "Debe contener el nombre del tenant");
        assertTrue(html.contains("ORD-TEST-1234"), "Debe contener el código de orden");
        assertTrue(html.contains("Juan Pérez"), "Debe contener el nombre del cliente");
        assertTrue(html.contains("#ff5500"), "Debe contener el color primario");
        assertTrue(html.contains("$37.000"), "Debe contener el total");
        assertFalse(html.contains("{{"), "No deben quedar placeholders sin reemplazar");
    }

    @Test
    void debeRenderizarTemplateDeInstruccionesDePago() {
        String html = engine.render("payment-instructions.html", Map.ofEntries(
                entry("tenantName", "Mi Tienda"),
                entry("tenantLogo", ""),
                entry("primaryColor", "#1a1a1a"),
                entry("orderCode", "ORD-PAY-0001"),
                entry("customerName", "María"),
                entry("total", "$50.000"),
                entry("subtotal", "$50.000"),
                entry("shippingCost", "$0"),
                entry("trackingUrl", ""),
                entry("whatsappUrl", ""),
                entry("bankName", "Bancolombia"),
                entry("accountType", "Ahorros"),
                entry("accountNumber", "123-456-789"),
                entry("accountHolder", "Tienda SAS"),
                entry("documentType", "NIT"),
                entry("documentNumber", "900123456-7")));

        assertTrue(html.contains("Bancolombia"));
        assertTrue(html.contains("123-456-789"));
        assertTrue(html.contains("Tienda SAS"));
    }

    @Test
    void debeRenderizarTemplateDeActualizacionDeEstado() {
        String html = engine.render("status-update.html", Map.ofEntries(
                entry("tenantName", "Mi Tienda"),
                entry("tenantLogo", ""),
                entry("primaryColor", "#1a1a1a"),
                entry("orderCode", "ORD-UPD-0001"),
                entry("customerName", "Carlos"),
                entry("total", "$25.000"),
                entry("subtotal", "$25.000"),
                entry("shippingCost", "$0"),
                entry("trackingUrl", "https://mitienda.projectx.com/order/ORD-UPD-0001"),
                entry("whatsappUrl", ""),
                entry("statusLabel", "En preparación"),
                entry("statusColor", "#ca8a04")));

        assertTrue(html.contains("En preparación"));
        assertTrue(html.contains("#ca8a04"));
    }

    @Test
    void debeRenderizarTemplateDeCancelacion() {
        String html = engine.render("order-cancellation.html", Map.ofEntries(
                entry("tenantName", "Mi Tienda"),
                entry("tenantLogo", ""),
                entry("primaryColor", "#1a1a1a"),
                entry("orderCode", "ORD-CAN-0001"),
                entry("customerName", "Ana"),
                entry("total", "$15.000"),
                entry("subtotal", "$15.000"),
                entry("shippingCost", "$0"),
                entry("trackingUrl", ""),
                entry("whatsappUrl", ""),
                entry("reason", "Producto agotado")));

        assertTrue(html.contains("Producto agotado"));
        assertTrue(html.contains("cancelado"));
    }

    @Test
    void debeRetornarVacioParaTemplateInexistente() {
        String html = engine.render("no-existe.html", Map.of());
        assertEquals("", html);
    }

    @Test
    void debeConstruirVariablesDeOrden() {
        Tenant tenant = new Tenant("mitienda", "Mi Tienda", "Desc", "https://logo.png", null, null,
                new TenantColors("#ff5500", "#333", "#eee", "#fff", "#000"),
                null, List.of(), List.of("Bogotá"), "+573001234567", null, null, null, null,
                new BankInfo("Bancolombia", "Ahorros", "123", "Titular", "CC", "999"),
                true, null, null, null, null, Instant.now(), Instant.now());

        Order order = new Order("id-1", "ORD-TEST-0001", "mitienda",
                new Customer("Juan Pérez", "juan@mail.com", "+573001234567"),
                List.of(new OrderItem("p1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null)),
                DeliveryMethod.SHIPPING, new DeliveryInfo("Calle 1", "Bogotá", null, null),
                PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(37000), BigDecimal.ZERO, BigDecimal.valueOf(37000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, Instant.now(), Instant.now());

        Map<String, String> vars = engine.buildOrderVariables(tenant, order);

        assertEquals("Mi Tienda", vars.get("tenantName"));
        assertEquals("https://logo.png", vars.get("tenantLogo"));
        assertEquals("#ff5500", vars.get("primaryColor"));
        assertEquals("ORD-TEST-0001", vars.get("orderCode"));
        assertEquals("Juan Pérez", vars.get("customerName"));
        assertTrue(vars.get("whatsappUrl").contains("573001234567"));
    }

    @Test
    void debeGenerarHtmlDeItems() {
        Order order = new Order("id-1", "ORD-1", "t",
                new Customer("C", "c@e.co", "+573001234567"),
                List.of(
                        new OrderItem("p1", "Granola", BigDecimal.valueOf(18500), 2, "img.jpg", null, null),
                        new OrderItem("p2", "Miel", BigDecimal.valueOf(12000), 1, "img2.jpg", "v1", "500ml")),
                DeliveryMethod.PICKUP, null, PaymentMethod.WOMPI, PaymentStatus.PENDING, OrderStatus.PENDING,
                BigDecimal.valueOf(49000), BigDecimal.ZERO, BigDecimal.valueOf(49000),
                BigDecimal.ZERO, BigDecimal.ZERO, false,
                List.of(), null, Instant.now(), Instant.now());

        String itemsHtml = engine.renderItems(order);

        assertTrue(itemsHtml.contains("Granola"));
        assertTrue(itemsHtml.contains("Miel"));
        assertTrue(itemsHtml.contains("500ml")); // variante
        assertTrue(itemsHtml.contains("<tr>"));
    }

    @Test
    void debeFormatearPrecioEnFormatoColombia() {
        String formatted = EmailTemplateEngine.formatPrice(BigDecimal.valueOf(37000));
        // El formato colombiano usa $ y puntos como separador de miles
        assertTrue(formatted.contains("37"));
        assertNotNull(formatted);
    }

    @Test
    void debeFormatearPrecioNull() {
        assertEquals("$0", EmailTemplateEngine.formatPrice(null));
    }
}
