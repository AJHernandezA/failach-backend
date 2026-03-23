package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.application.dto.UpdateTenantRequest;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.TenantNotFoundException;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.in.UpdateTenantConfigUseCase;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Implementación del caso de uso para actualizar la configuración de un tenant.
 * Valida los datos de entrada, actualiza los campos y persiste en el
 * repositorio.
 */
public class UpdateTenantConfigUseCaseImpl implements UpdateTenantConfigUseCase {

    private static final Logger log = LoggerFactory.getLogger(UpdateTenantConfigUseCaseImpl.class);

    /** Patrón para validar colores hex: #XXXXXX */
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");

    /** Patrón para validar WhatsApp colombiano: +57 seguido de 10 dígitos */
    private static final Pattern WHATSAPP_PATTERN = Pattern.compile("^\\+57\\d{10}$");

    private final TenantRepository tenantRepository;

    @Inject
    public UpdateTenantConfigUseCaseImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public Tenant execute(String tenantId, UpdateTenantRequest request) {
        log.debug("Actualizando configuración del tenant: {}", tenantId);

        // Verificar que el tenant existe
        Tenant existing = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));

        // Validar campos
        validate(request);

        // Construir tenant actualizado conservando campos inmutables
        Tenant updated = new Tenant(
                existing.tenantId(),
                request.name(),
                request.description(),
                request.logoUrl(),
                request.faviconUrl(),
                request.bannerUrl(),
                request.colors(),
                request.font(),
                request.socialMedia(),
                request.cities(),
                request.whatsapp(),
                request.email(),
                request.phone(),
                request.address(),
                request.schedule(),
                request.bankInfo(),
                existing.isActive(),
                request.thankYouMessage(),
                request.analyticsId(),
                request.shippingConfig(),
                request.manualPaymentDiscount(),
                existing.createdAt(),
                Instant.now());

        // Persistir cambios
        tenantRepository.save(updated);
        log.info("Configuración actualizada para tenant: {}", tenantId);

        return updated;
    }

    /**
     * Valida los campos del request según las reglas del dominio.
     */
    private void validate(UpdateTenantRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("El nombre es requerido");
        }
        if (request.name().length() > 100) {
            throw new BadRequestException("El nombre no puede tener más de 100 caracteres");
        }
        if (request.cities() == null || request.cities().isEmpty()) {
            throw new BadRequestException("Debe haber al menos una ciudad de cobertura");
        }
        if (request.colors() != null && request.colors().primary() != null
                && !HEX_COLOR.matcher(request.colors().primary()).matches()) {
            throw new BadRequestException("Color primario debe ser formato hex (#XXXXXX)");
        }
        if (request.whatsapp() != null && !request.whatsapp().isBlank()
                && !WHATSAPP_PATTERN.matcher(request.whatsapp()).matches()) {
            throw new BadRequestException("Número de WhatsApp inválido. Formato esperado: +57XXXXXXXXXX");
        }
    }
}
