package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.ContactMessage;

/**
 * Puerto de salida para enviar notificaciones por email al administrador de la plataforma.
 * Notifica eventos clave: formulario de contacto, registro de cuentas, actualización de tiendas.
 */
public interface AdminNotificationService {

    /**
     * Notifica al admin cuando se recibe un mensaje del formulario de contacto.
     *
     * @param message datos del mensaje de contacto
     */
    void notifyContactForm(ContactMessage message);

    /**
     * Notifica al admin cuando un nuevo vendedor se registra.
     *
     * @param email correo del vendedor registrado
     * @param name nombre del vendedor
     * @param tenantSlug slug/subdomain asignado
     */
    void notifyAccountCreated(String email, String name, String tenantSlug);

    /**
     * Notifica al admin cuando se actualiza la configuración de una tienda.
     *
     * @param tenantId identificador del tenant
     * @param businessName nombre del negocio
     */
    void notifyStoreUpdated(String tenantId, String businessName);

    /**
     * Envía una notificación genérica al admin.
     *
     * @param subject asunto del email
     * @param body cuerpo del mensaje
     */
    void notify(String subject, String body);
}
