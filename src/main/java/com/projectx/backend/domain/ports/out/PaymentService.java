package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentLink;
import com.projectx.backend.domain.models.TransactionVerification;

import java.util.List;
import java.util.Optional;

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

    /**
     * Verifica una transacción directamente con la API de Wompi.
     * Seguridad: no confiar ciegamente en el webhook, sino verificar server-side.
     */
    Optional<TransactionVerification> verifyTransaction(String transactionId);

    /**
     * Crea un link de pago en Wompi.
     */
    PaymentLink createPaymentLink(String name, String description, boolean singleUse,
            Long amountInCents, String imageUrl, String redirectUrl);

    /**
     * Lista los links de pago creados.
     */
    List<PaymentLink> listPaymentLinks();

    /**
     * Obtiene un link de pago por ID.
     */
    PaymentLink getPaymentLink(String linkId);
}
