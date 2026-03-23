package com.projectx.backend.infra.adapters.out.payment;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.ports.out.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Implementación del servicio de pagos con Wompi.
 * Usa las llaves del tenant para generar firmas y verificar webhooks.
 * En desarrollo usa sandbox; en producción usa llaves reales.
 */
public class WompiPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(WompiPaymentService.class);

    // En producción se obtienen de Secrets Manager por tenant
    private static final String DEFAULT_PUBLIC_KEY = "pub_stagtest_g2u0HQd3ZMh05hsSgTS2lUV8t3s4mOt7";
    private static final String DEFAULT_INTEGRITY_SECRET = "stagtest_integrity_nAIBuqayW70XpUqJS4qf4STYiISd89Fp";
    private static final String DEFAULT_EVENTS_SECRET = "stagtest_events_secret";

    @Override
    public PaymentInitData initiate(String tenantId, Order order, String redirectUrl) {
        String reference = "PX-" + tenantId + "-" + order.orderCode();
        long amountInCents = order.total().longValue() * 100;
        String currency = "COP";

        String signature = calculateIntegritySignature(reference, amountInCents, currency);

        log.info("Wompi payment iniciado: ref={}, monto={} centavos", reference, amountInCents);

        return new PaymentInitData(
                reference,
                amountInCents,
                currency,
                signature,
                DEFAULT_PUBLIC_KEY,
                redirectUrl
        );
    }

    @Override
    public boolean verifyWebhookSignature(String transactionId, String status, long amountInCents, String receivedSignature) {
        // Firma del webhook: SHA256(transactionId + status + amountInCents + events_secret)
        String toSign = transactionId + status + amountInCents + DEFAULT_EVENTS_SECRET;
        String expected = sha256(toSign);
        boolean valid = expected.equals(receivedSignature);
        if (!valid) {
            log.warn("Firma de webhook inválida. Esperado: {}, Recibido: {}", expected, receivedSignature);
        }
        return valid;
    }

    @Override
    public String calculateIntegritySignature(String reference, long amountInCents, String currency) {
        // SHA256(referencia + monto_centavos + moneda + integrity_secret)
        String toSign = reference + amountInCents + currency + DEFAULT_INTEGRITY_SECRET;
        return sha256(toSign);
    }

    /**
     * Calcula SHA256 hex de un string.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando SHA256", e);
        }
    }
}
