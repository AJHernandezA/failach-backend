package com.projectx.backend.domain.models;

import java.time.Instant;

/**
 * Mensaje de contacto recibido desde el formulario de la landing page.
 * Se almacena en DynamoDB para revisión posterior.
 */
public record ContactMessage(
                String id,
                String name,
                String email,
                String phone,
                String subject,
                String message,
                String ip,
                Instant createdAt) {
}
