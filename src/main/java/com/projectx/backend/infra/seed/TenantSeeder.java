package com.projectx.backend.infra.seed;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Seeder de datos iniciales para tenants.
 * Inserta los 3 tenants base si no existen en DynamoDB.
 * Solo se ejecuta en desarrollo.
 */
public class TenantSeeder {

    private static final Logger log = LoggerFactory.getLogger(TenantSeeder.class);

    private final TenantRepository tenantRepository;

    @Inject
    public TenantSeeder(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Ejecuta el seed de datos. Solo inserta si el tenant no existe.
     */
    public void seed() {
        seedIfNotExists(createIdoneo());
        seedIfNotExists(createChicha());
        seedIfNotExists(createTech());
        log.info("Seed de tenants completado");
    }

    private void seedIfNotExists(Tenant tenant) {
        if (tenantRepository.findById(tenant.tenantId()).isEmpty()) {
            tenantRepository.save(tenant);
            log.info("Tenant creado: {}", tenant.tenantId());
        } else {
            log.debug("Tenant ya existe: {}", tenant.tenantId());
        }
    }

    private Tenant createIdoneo() {
        return new Tenant(
                "idoneo",
                "IDONEO",
                "Tienda de productos naturales y saludables",
                "",
                null,
                "",
                new TenantColors("#2D5016", "#4A7C23", "#F59E0B", "#FFFFFF", "#1A1A1A"),
                "Inter",
                List.of(
                        new SocialLink("instagram", "https://instagram.com/idoneo", true),
                        new SocialLink("facebook", "https://facebook.com/idoneo", true)),
                List.of("Bogotá", "Medellín", "Cali"),
                "+573001234567",
                "contacto@idoneo.com",
                "+573001234567",
                "Bogotá, Colombia",
                "Lunes a Viernes 8am - 6pm",
                new BankInfo("Bancolombia", "Ahorros", "12345678901", "IDONEO SAS", "NIT", "900123456"),
                true,
                null, // thankYouMessage
                null, // analyticsId
                new ShippingConfig(BigDecimal.valueOf(6000), BigDecimal.valueOf(70000), true, false, "Domicilio"),
                new ManualPaymentDiscountConfig(true, BigDecimal.valueOf(0.03),
                        "3% de descuento por pago con transferencia o efectivo",
                        List.of("BANK_TRANSFER", "CASH_ON_DELIVERY")),
                Instant.now(),
                Instant.now());
    }

    private Tenant createChicha() {
        return new Tenant(
                "chicha",
                "CHICHA",
                "Accesorios y moda artesanal colombiana",
                "",
                null,
                "",
                new TenantColors("#DC2626", "#B91C1C", "#FBBF24", "#FFF7ED", "#1A1A1A"),
                "Poppins",
                List.of(
                        new SocialLink("instagram", "https://instagram.com/chicha", true),
                        new SocialLink("tiktok", "https://tiktok.com/@chicha", true)),
                List.of("Bogotá", "Cartagena"),
                "+573009876543",
                "hola@chicha.com",
                "+573009876543",
                "Cartagena, Colombia",
                "Lunes a Sábado 9am - 7pm",
                new BankInfo("Davivienda", "Corriente", "98765432101", "CHICHA LTDA", "NIT", "900654321"),
                true,
                null, // thankYouMessage
                null, // analyticsId
                new ShippingConfig(BigDecimal.valueOf(5000), BigDecimal.valueOf(40000), true, false, "Domicilio"),
                new ManualPaymentDiscountConfig(true, BigDecimal.valueOf(0.05),
                        "5% de descuento por pago con transferencia o efectivo",
                        List.of("BANK_TRANSFER", "CASH_ON_DELIVERY")),
                Instant.now(),
                Instant.now());
    }

    private Tenant createTech() {
        return new Tenant(
                "tech",
                "Tech Store",
                "Tienda de tecnología y accesorios electrónicos",
                "",
                null,
                "",
                new TenantColors("#3B82F6", "#1D4ED8", "#10B981", "#F8FAFC", "#0F172A"),
                "Inter",
                List.of(
                        new SocialLink("instagram", "https://instagram.com/techstore", true),
                        new SocialLink("facebook", "https://facebook.com/techstore", true),
                        new SocialLink("twitter", "https://twitter.com/techstore", true)),
                List.of("Bogotá", "Medellín", "Cali", "Barranquilla"),
                "+573005551234",
                "info@techstore.com",
                "+573005551234",
                "Medellín, Colombia",
                "Lunes a Domingo 10am - 8pm",
                new BankInfo("Nequi", "Ahorros", "3005551234", "TECH STORE SAS", "NIT", "900789012"),
                true,
                null, // thankYouMessage
                null, // analyticsId
                new ShippingConfig(BigDecimal.valueOf(12000), BigDecimal.valueOf(200000), true, false, "Envío"),
                new ManualPaymentDiscountConfig(true, BigDecimal.valueOf(0.02),
                        "2% de descuento por pago con transferencia o efectivo",
                        List.of("BANK_TRANSFER", "CASH_ON_DELIVERY")),
                Instant.now(),
                Instant.now());
    }
}
