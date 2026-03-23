package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;

/**
 * Puerto de salida para la integración con la pasarela de pagos (Wompi).
 */
public interface PaymentService {

    /**
     * Genera los datos de inicialización para el widget de Wompi.
     */
    PaymentInitData initiate(String tenantId, Order order, String redirectUrl);

    /**
     * Verifica la firma de un evento de webhook de Wompi.
     */
    boolean verifyWebhookSignature(String transactionId, String status, long amountInCents, String receivedSignature);

    /**
     * Calcula la firma de integridad para una referencia de pago.
     */
    String calculateIntegritySignature(String reference, long amountInCents, String currency);
}
