package com.projectx.backend.infra.adapters.out.email;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

/**
 * Implementación de EmailService usando AWS SES.
 * Envía emails transaccionales con templates HTML personalizados por tenant.
 * Si SES falla, loguea el error pero NO lanza excepción.
 */
public class SesEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SesEmailService.class);

    private static final Map<OrderStatus, String> STATUS_LABELS = Map.of(
            OrderStatus.PENDING, "Pedido recibido",
            OrderStatus.CONFIRMED, "Pago confirmado",
            OrderStatus.PREPARING, "En preparación",
            OrderStatus.SHIPPED, "En camino",
            OrderStatus.DELIVERED, "Entregado",
            OrderStatus.CANCELLED, "Cancelado"
    );

    private static final Map<OrderStatus, String> STATUS_COLORS = Map.of(
            OrderStatus.PENDING, "#6b7280",
            OrderStatus.CONFIRMED, "#2563eb",
            OrderStatus.PREPARING, "#ca8a04",
            OrderStatus.SHIPPED, "#2563eb",
            OrderStatus.DELIVERED, "#16a34a",
            OrderStatus.CANCELLED, "#dc2626"
    );

    private final SesClient sesClient;
    private final EmailTemplateEngine templateEngine;
    private final String fromEmail;

    @Inject
    public SesEmailService(SesClient sesClient, AppConfig appConfig) {
        this.sesClient = sesClient;
        this.templateEngine = new EmailTemplateEngine();
        this.fromEmail = appConfig.getSesFromEmail();
    }

    @Override
    public void sendOrderConfirmation(Tenant tenant, Order order) {
        try {
            var vars = templateEngine.buildOrderVariables(tenant, order);
            vars.put("items", templateEngine.renderItems(order));
            String html = templateEngine.render("order-confirmation.html", vars);
            String subject = "¡Pedido recibido! — " + order.orderCode() + " | " + tenant.name();
            send(order.customer().email(), subject, html);
            log.info("Email de confirmación enviado a {} para orden {}", order.customer().email(), order.orderCode());
        } catch (Exception e) {
            log.warn("Error enviando email de confirmación para orden {}: {}", order.orderCode(), e.getMessage());
        }
    }

    @Override
    public void sendPaymentInstructions(Tenant tenant, Order order) {
        try {
            var vars = templateEngine.buildOrderVariables(tenant, order);
            BankInfo bank = tenant.bankInfo();
            if (bank != null) {
                vars.put("bankName", safe(bank.bankName()));
                vars.put("accountType", safe(bank.accountType()));
                vars.put("accountNumber", safe(bank.accountNumber()));
                vars.put("accountHolder", safe(bank.accountHolder()));
                vars.put("documentType", safe(bank.documentType()));
                vars.put("documentNumber", safe(bank.documentNumber()));
            }
            String html = templateEngine.render("payment-instructions.html", vars);
            String subject = "Datos para transferencia — " + order.orderCode() + " | " + tenant.name();
            send(order.customer().email(), subject, html);
            log.info("Email de instrucciones de pago enviado a {} para orden {}", order.customer().email(), order.orderCode());
        } catch (Exception e) {
            log.warn("Error enviando email de instrucciones de pago para orden {}: {}", order.orderCode(), e.getMessage());
        }
    }

    @Override
    public void sendStatusUpdate(Tenant tenant, Order order, OrderStatus newStatus) {
        try {
            var vars = templateEngine.buildOrderVariables(tenant, order);
            vars.put("statusLabel", STATUS_LABELS.getOrDefault(newStatus, newStatus.name()));
            vars.put("statusColor", STATUS_COLORS.getOrDefault(newStatus, "#1a1a1a"));
            String html = templateEngine.render("status-update.html", vars);
            String subject = "Tu pedido: " + STATUS_LABELS.getOrDefault(newStatus, newStatus.name()) + " — " + order.orderCode();
            send(order.customer().email(), subject, html);
            log.info("Email de actualización de estado enviado a {} para orden {} ({})", order.customer().email(), order.orderCode(), newStatus);
        } catch (Exception e) {
            log.warn("Error enviando email de actualización de estado para orden {}: {}", order.orderCode(), e.getMessage());
        }
    }

    @Override
    public void sendOrderCancellation(Tenant tenant, Order order, String reason) {
        try {
            var vars = templateEngine.buildOrderVariables(tenant, order);
            vars.put("reason", safe(reason));
            String html = templateEngine.render("order-cancellation.html", vars);
            String subject = "Pedido cancelado — " + order.orderCode() + " | " + tenant.name();
            send(order.customer().email(), subject, html);
            log.info("Email de cancelación enviado a {} para orden {}", order.customer().email(), order.orderCode());
        } catch (Exception e) {
            log.warn("Error enviando email de cancelación para orden {}: {}", order.orderCode(), e.getMessage());
        }
    }

    /**
     * Envía un email HTML usando AWS SES.
     */
    private void send(String to, String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(to).build())
                .message(Message.builder()
                        .subject(Content.builder().data(subject).charset("UTF-8").build())
                        .body(Body.builder()
                                .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                .build())
                        .build())
                .build();

        sesClient.sendEmail(request);
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
