package com.projectx.backend.infra.adapters.out.payment;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentLink;
import com.projectx.backend.domain.models.TransactionVerification;
import com.projectx.backend.domain.ports.out.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación mock del servicio de pagos para desarrollo.
 * No requiere llaves de Wompi reales. Simula respuestas válidas.
 * Equivalente al MockMercadoPagoService de Tiquetera.
 */
public class MockWompiPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(MockWompiPaymentService.class);

    /** Almacén en memoria de links de pago creados durante la sesión */
    private final ConcurrentHashMap<String, PaymentLink> paymentLinks = new ConcurrentHashMap<>();

    public MockWompiPaymentService() {
        log.info("[MOCK] MockWompiPaymentService inicializado. No se requieren llaves de Wompi.");
    }

    @Override
    public PaymentInitData initiate(String tenantId, Order order, String redirectUrl) {
        String reference = "PX-" + tenantId + "-" + order.orderCode();
        long amountInCents = order.total().longValue() * 100;
        String currency = "COP";

        // Usar formato válido de llave pública de prueba de Wompi
        // Formato: pub_test_<24 caracteres alfanuméricos>
        String mockPublicKey = "pub_test_X0zDA9xoKdePzhd8a0x9HAez7HgGO2fH";

        // Calcular firma de integridad mock (en producción se usa el secreto real)
        String mockSignature = calculateIntegritySignature(reference, amountInCents, currency);

        log.info("[MOCK] Pago iniciado: ref={}, monto={} centavos", reference, amountInCents);

        return new PaymentInitData(reference, amountInCents, currency, mockSignature, mockPublicKey, redirectUrl);
    }

    @Override
    public boolean verifyWebhookSignature(String transactionId, String status, long amountInCents,
            String receivedSignature) {
        log.info("[MOCK] Verificación de webhook: txId={}, status={} — siempre válido en mock", transactionId, status);
        return true;
    }

    @Override
    public String calculateIntegritySignature(String reference, long amountInCents, String currency) {
        // En mock, generar firma SHA256 válida usando un secreto mock
        // Formato Wompi: SHA256(reference + amountInCents + currency + integritySecret)
        String mockIntegritySecret = "test_integrity_aBcDeFgHiJkLmNoPqRsTuVwXyZ123456";
        String toSign = reference + amountInCents + currency + mockIntegritySecret;

        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(toSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("[MOCK] Error generando firma SHA256", e);
            return "mock_integrity_fallback_" + reference;
        }
    }

    @Override
    public PaymentLink createPaymentLink(String name, String description, boolean singleUse,
            Long amountInCents, String imageUrl, String redirectUrl) {
        String id = UUID.randomUUID().toString().substring(0, 6);
        PaymentLink link = new PaymentLink(
                id, name, description, singleUse, amountInCents, "COP",
                imageUrl, "https://checkout.wompi.co/l/" + id, true);

        paymentLinks.put(id, link);
        log.info("[MOCK] Link de pago creado: id={}, name={}", id, name);
        return link;
    }

    @Override
    public List<PaymentLink> listPaymentLinks() {
        log.info("[MOCK] Listando {} links de pago mock", paymentLinks.size());
        return new ArrayList<>(paymentLinks.values());
    }

    @Override
    public PaymentLink getPaymentLink(String linkId) {
        PaymentLink link = paymentLinks.get(linkId);
        if (link == null) {
            log.info("[MOCK] Link de pago {} no encontrado", linkId);
        }
        return link;
    }

    @Override
    public Optional<TransactionVerification> verifyTransaction(String transactionId) {
        // En mock, simular que la transacción siempre está aprobada
        log.info("[MOCK] Verificación server-side de transacción: {} — siempre APPROVED en mock", transactionId);
        return Optional.of(new TransactionVerification(
                transactionId, "APPROVED", "PX-mock-ORD-MOCK", 0, "COP", "CARD"));
    }
}
