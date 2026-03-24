package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.ContactMessage;
import com.projectx.backend.domain.ports.in.SendContactMessageUseCase;
import com.projectx.backend.domain.ports.out.AdminNotificationService;
import com.projectx.backend.domain.ports.out.PlatformRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Implementación del caso de uso para recibir mensajes de contacto
 * desde la landing page de la plataforma.
 * Valida los campos y persiste el mensaje en la base de datos.
 */
@Singleton
public class SendContactMessageUseCaseImpl implements SendContactMessageUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendContactMessageUseCaseImpl.class);

    private final PlatformRepository platformRepository;
    private final AdminNotificationService adminNotificationService;

    @Inject
    public SendContactMessageUseCaseImpl(PlatformRepository platformRepository,
            AdminNotificationService adminNotificationService) {
        this.platformRepository = platformRepository;
        this.adminNotificationService = adminNotificationService;
    }

    @Override
    public ContactMessage execute(String name, String email, String phone, String subject, String message, String ip) {
        // Validar campos requeridos
        if (name == null || name.trim().length() < 2) {
            throw new BadRequestException("El nombre debe tener al menos 2 caracteres");
        }
        if (email == null || !email.contains("@")) {
            throw new BadRequestException("Email inválido");
        }
        if (message == null || message.trim().length() < 10) {
            throw new BadRequestException("El mensaje debe tener al menos 10 caracteres");
        }
        if (name.length() > 100 || email.length() > 200 || message.length() > 1000) {
            throw new BadRequestException("Campos exceden el largo máximo permitido");
        }
        if (subject != null && subject.length() > 200) {
            throw new BadRequestException("Asunto excede el largo máximo permitido");
        }

        // Sanitizar inputs básicos
        String sanitizedName = name.trim();
        String sanitizedEmail = email.trim().toLowerCase();
        String sanitizedPhone = phone != null ? phone.trim() : null;
        String sanitizedSubject = subject != null ? subject.trim() : "Sin asunto";
        String sanitizedMessage = message.trim();

        ContactMessage contactMessage = new ContactMessage(
                UUID.randomUUID().toString(),
                sanitizedName,
                sanitizedEmail,
                sanitizedPhone,
                sanitizedSubject,
                sanitizedMessage,
                ip,
                Instant.now());

        platformRepository.saveContactMessage(contactMessage);
        log.info("Mensaje de contacto recibido de: {} <{}>", sanitizedName, sanitizedEmail);

        // F045: Notificar al admin por email
        try {
            adminNotificationService.notifyContactForm(contactMessage);
        } catch (Exception e) {
            log.warn("Error al notificar al admin sobre mensaje de contacto: {}", e.getMessage());
        }

        return contactMessage;
    }
}
