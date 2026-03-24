package com.projectx.backend.infra.adapters.out.email;

import com.projectx.backend.domain.models.ContactMessage;
import com.projectx.backend.domain.ports.out.AdminNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación de AdminNotificationService para desarrollo.
 * Solo loguea las notificaciones en vez de enviarlas por email.
 */
public class LogAdminNotificationService implements AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogAdminNotificationService.class);

    @Override
    public void notifyContactForm(ContactMessage message) {
        log.info("[ADMIN-NOTIF] Nuevo mensaje de contacto → De: {} <{}> | Tel: {} | Asunto: {} | Mensaje: {}",
                message.name(), message.email(), message.phone(), message.subject(), message.message());
    }

    @Override
    public void notifyAccountCreated(String email, String name, String tenantSlug) {
        log.info("[ADMIN-NOTIF] Nueva cuenta creada → {} <{}> | Tenant: {}",
                name, email, tenantSlug);
    }

    @Override
    public void notifyStoreUpdated(String tenantId, String businessName) {
        log.info("[ADMIN-NOTIF] Tienda actualizada → Tenant: {} | Negocio: {}",
                tenantId, businessName);
    }

    @Override
    public void notify(String subject, String body) {
        log.info("[ADMIN-NOTIF] {} → {}", subject, body);
    }
}
