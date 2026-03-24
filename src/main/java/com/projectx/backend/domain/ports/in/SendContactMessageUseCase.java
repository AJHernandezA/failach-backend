package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.ContactMessage;

/**
 * Puerto de entrada para recibir y almacenar mensajes de contacto
 * enviados desde el formulario de la landing page.
 */
public interface SendContactMessageUseCase {
    ContactMessage execute(String name, String email, String phone, String subject, String message, String ip);
}
