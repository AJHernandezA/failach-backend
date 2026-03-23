package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.models.TenantColors;
import com.projectx.backend.domain.ports.in.GetTenantConfigUseCase;
import com.projectx.backend.domain.ports.out.LegalContentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLegalContentUseCaseImplTest {

    @Mock
    private LegalContentRepository legalContentRepository;
    @Mock
    private GetTenantConfigUseCase getTenantConfig;

    private GetLegalContentUseCaseImpl useCase;

    private static final String TENANT = "idoneo";

    @BeforeEach
    void setUp() {
        useCase = new GetLegalContentUseCaseImpl(legalContentRepository, getTenantConfig);
    }

    @Test
    void debeRetornarContenidoCustomSiExiste() {
        LegalContent custom = new LegalContent(TENANT, "terms", "Mis Términos", "Contenido custom", Instant.now());
        when(legalContentRepository.findByType(TENANT, "terms")).thenReturn(Optional.of(custom));

        LegalContent result = useCase.execute(TENANT, "terms");

        assertEquals("Mis Términos", result.title());
        assertEquals("Contenido custom", result.content());
    }

    @Test
    void debeRetornarTemplatePorDefectoSiNoHayCustom() {
        when(legalContentRepository.findByType(TENANT, "terms")).thenReturn(Optional.empty());

        Tenant tenant = new Tenant(TENANT, "Idoneo", "desc", "logo.png", null, "banner.png",
                new TenantColors("#000", "#666", "#ff6600", "#fff", "#1a1a1a"),
                "Inter", List.of(), List.of(), "573001234567", "test@idoneo.com",
                "3001234567", "Calle 1 #2-3", "Lun-Vie 9-6", null, true,
                null, null, null, null, Instant.now(), Instant.now());
        when(getTenantConfig.execute(TENANT)).thenReturn(tenant);

        LegalContent result = useCase.execute(TENANT, "terms");

        assertEquals("Términos y Condiciones", result.title());
        // El contenido debe incluir el nombre del tenant (placeholder reemplazado)
        assertTrue(result.content().contains("Idoneo"));
    }

    @Test
    void debeFallarConTipoInvalido() {
        assertThrows(BadRequestException.class, () -> useCase.execute(TENANT, "invalid_type"));
    }

    @Test
    void debeRetornarPrivacidad() {
        when(legalContentRepository.findByType(TENANT, "privacy")).thenReturn(Optional.empty());

        Tenant tenant = new Tenant(TENANT, "Idoneo", "desc", "logo.png", null, "banner.png",
                new TenantColors("#000", "#666", "#ff6600", "#fff", "#1a1a1a"),
                "Inter", List.of(), List.of(), "573001234567", "test@idoneo.com",
                "3001234567", "Calle 1 #2-3", "Lun-Vie 9-6", null, true,
                null, null, null, null, Instant.now(), Instant.now());
        when(getTenantConfig.execute(TENANT)).thenReturn(tenant);

        LegalContent result = useCase.execute(TENANT, "privacy");

        assertEquals("Política de Privacidad", result.title());
        assertTrue(result.content().contains("Idoneo"));
    }

    @Test
    void debeRetornarDevoluciones() {
        when(legalContentRepository.findByType(TENANT, "returns")).thenReturn(Optional.empty());

        Tenant tenant = new Tenant(TENANT, "Idoneo", "desc", "logo.png", null, "banner.png",
                new TenantColors("#000", "#666", "#ff6600", "#fff", "#1a1a1a"),
                "Inter", List.of(), List.of(), "573001234567", "test@idoneo.com",
                "3001234567", "Calle 1 #2-3", "Lun-Vie 9-6", null, true,
                null, null, null, null, Instant.now(), Instant.now());
        when(getTenantConfig.execute(TENANT)).thenReturn(tenant);

        LegalContent result = useCase.execute(TENANT, "returns");

        assertEquals("Política de Devoluciones", result.title());
        assertTrue(result.content().contains("Idoneo"));
    }
}
