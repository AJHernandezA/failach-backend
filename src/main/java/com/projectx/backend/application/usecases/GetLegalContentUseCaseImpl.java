package com.projectx.backend.application.usecases;

import com.google.inject.Inject;
import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.in.GetLegalContentUseCase;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.out.LegalContentRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

/**
 * Implementación del caso de uso para obtener contenido legal.
 * Si el tenant no tiene contenido custom, retorna template por defecto
 * con placeholders reemplazados.
 */
public class GetLegalContentUseCaseImpl implements GetLegalContentUseCase {

    private static final Set<String> VALID_TYPES = Set.of("terms", "privacy", "returns");

    private static final String TITLES_TERMS = "Términos y Condiciones";
    private static final String TITLES_PRIVACY = "Política de Privacidad";
    private static final String TITLES_RETURNS = "Política de Devoluciones";

    private final LegalContentRepository legalContentRepository;
    private final GetTenantConfigUseCase getTenantConfig;

    @Inject
    public GetLegalContentUseCaseImpl(LegalContentRepository legalContentRepository,
                                       GetTenantConfigUseCase getTenantConfig) {
        this.legalContentRepository = legalContentRepository;
        this.getTenantConfig = getTenantConfig;
    }

    @Override
    public LegalContent execute(String tenantId, String type) {
        if (!VALID_TYPES.contains(type)) {
            throw new BadRequestException("Tipo de contenido legal inválido: " + type + ". Válidos: terms, privacy, returns");
        }

        // Buscar contenido custom del tenant
        return legalContentRepository.findByType(tenantId, type)
                .orElseGet(() -> buildDefaultContent(tenantId, type));
    }

    /**
     * Construye contenido legal por defecto a partir de templates.
     */
    private LegalContent buildDefaultContent(String tenantId, String type) {
        Tenant tenant = getTenantConfig.execute(tenantId);
        String template = loadTemplate(type);

        // Reemplazar placeholders
        String content = template
                .replace("{{tenantName}}", tenant.name() != null ? tenant.name() : tenantId)
                .replace("{{tenantEmail}}", tenant.email() != null ? tenant.email() : "contacto@tienda.com")
                .replace("{{tenantAddress}}", tenant.address() != null ? tenant.address() : "Dirección no configurada")
                .replace("{{tenantPhone}}", tenant.phone() != null ? tenant.phone() : "Teléfono no configurado");

        String title = switch (type) {
            case "terms" -> TITLES_TERMS;
            case "privacy" -> TITLES_PRIVACY;
            case "returns" -> TITLES_RETURNS;
            default -> type;
        };

        return new LegalContent(tenantId, type, title, content, Instant.now());
    }

    /**
     * Carga template por defecto desde resources.
     */
    private String loadTemplate(String type) {
        String resourcePath = "/templates/legal/default-" + type + ".md";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return "# Contenido no disponible\n\nEl contenido legal de tipo **" + type + "** no está configurado aún.";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "# Contenido no disponible\n\nError al cargar el template por defecto.";
        }
    }
}
