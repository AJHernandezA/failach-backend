package com.projectx.backend.infra.adapters.out.payment;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentLink;
import com.projectx.backend.domain.ports.out.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
        String mockSignature = "mock_signature_" + reference;
        String mockPublicKey = "pub_test_mock_key";

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
        return "mock_integrity_" + reference;
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
}
