package com.projectx.backend.infra.adapters.out.email;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.ContactMessage;
import com.projectx.backend.domain.ports.out.AdminNotificationService;
import com.projectx.backend.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.Map;

/**
 * Implementación de AdminNotificationService usando AWS SES.
 * Envía notificaciones por email al administrador de la plataforma.
 * Destinatario único: adolfojhawork@gmail.com
 */
public class SesAdminNotificationService implements AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SesAdminNotificationService.class);
    private static final String ADMIN_EMAIL = "adolfojhawork@gmail.com";

    private final SesClient sesClient;
    private final EmailTemplateEngine templateEngine;
    private final String fromEmail;

    @Inject
    public SesAdminNotificationService(SesClient sesClient, AppConfig appConfig) {
        this.sesClient = sesClient;
        this.templateEngine = new EmailTemplateEngine();
        this.fromEmail = appConfig.getSesFromEmail();
    }

    @Override
    public void notifyContactForm(ContactMessage message) {
        try {
            String subject = "[Project-X] Nuevo mensaje de contacto: " + message.name();
            Map<String, String> vars = Map.ofEntries(
                    Map.entry("title", "Nuevo Mensaje de Contacto"),
                    Map.entry("subtitle", "Asunto: " + safe(message.subject())),
                    Map.entry("detail1Label", "Nombre"),
                    Map.entry("detail1Value", safe(message.name())),
                    Map.entry("detail2Label", "Email"),
                    Map.entry("detail2Value", safe(message.email())),
                    Map.entry("detail3Label", "Teléfono"),
                    Map.entry("detail3Value", safe(message.phone())),
                    Map.entry("detail4Label", "Mensaje"),
                    Map.entry("detail4Value", safe(message.message())));
            String html = templateEngine.render("admin-notification.html", vars);
            send(subject, html);
            log.info("Notificación de contacto enviada al admin por: {} <{}>", message.name(), message.email());
        } catch (Exception e) {
            log.warn("Error enviando notificación de contacto al admin: {}", e.getMessage());
        }
    }

    @Override
    public void notifyAccountCreated(String email, String name, String tenantSlug) {
        try {
            String subject = "[Project-X] Nueva cuenta registrada: " + name;
            Map<String, String> vars = Map.of(
                    "title", "Nueva Cuenta Registrada",
                    "subtitle", "Un nuevo vendedor se ha registrado en la plataforma",
                    "detail1Label", "Nombre",
                    "detail1Value", safe(name),
                    "detail2Label", "Email",
                    "detail2Value", safe(email),
                    "detail3Label", "Tienda (slug)",
                    "detail3Value", safe(tenantSlug),
                    "detail4Label", "",
                    "detail4Value", "");
            String html = templateEngine.render("admin-notification.html", vars);
            send(subject, html);
            log.info("Notificación de registro enviada al admin para: {} <{}>", name, email);
        } catch (Exception e) {
            log.warn("Error enviando notificación de registro al admin: {}", e.getMessage());
        }
    }

    @Override
    public void notifyStoreUpdated(String tenantId, String businessName) {
        try {
            String subject = "[Project-X] Tienda actualizada: " + businessName;
            Map<String, String> vars = Map.of(
                    "title", "Tienda Actualizada",
                    "subtitle", "Un vendedor ha actualizado la configuración de su tienda",
                    "detail1Label", "Tenant ID",
                    "detail1Value", safe(tenantId),
                    "detail2Label", "Negocio",
                    "detail2Value", safe(businessName),
                    "detail3Label", "",
                    "detail3Value", "",
                    "detail4Label", "",
                    "detail4Value", "");
            String html = templateEngine.render("admin-notification.html", vars);
            send(subject, html);
            log.info("Notificación de tienda actualizada enviada al admin: {} ({})", businessName, tenantId);
        } catch (Exception e) {
            log.warn("Error enviando notificación de tienda al admin: {}", e.getMessage());
        }
    }

    @Override
    public void notify(String subject, String body) {
        try {
            Map<String, String> vars = Map.of(
                    "title", safe(subject),
                    "subtitle", "",
                    "detail1Label", "Detalle",
                    "detail1Value", safe(body),
                    "detail2Label", "",
                    "detail2Value", "",
                    "detail3Label", "",
                    "detail3Value", "",
                    "detail4Label", "",
                    "detail4Value", "");
            String html = templateEngine.render("admin-notification.html", vars);
            send("[Project-X] " + subject, html);
            log.info("Notificación genérica enviada al admin: {}", subject);
        } catch (Exception e) {
            log.warn("Error enviando notificación genérica al admin: {}", e.getMessage());
        }
    }

    /**
     * Envía un email HTML al admin usando AWS SES.
     */
    private void send(String subject, String htmlBody) {
        SendEmailRequest request = SendEmailRequest.builder()
                .source(fromEmail)
                .destination(Destination.builder().toAddresses(ADMIN_EMAIL).build())
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
