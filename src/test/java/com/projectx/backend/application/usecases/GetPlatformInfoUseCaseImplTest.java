package com.projectx.backend.application.usecases;

import com.projectx.backend.domain.models.PlatformInfo;
import com.projectx.backend.domain.models.Tenant;
import com.projectx.backend.domain.ports.out.PlatformRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests para el caso de uso de obtener información pública de la plataforma.
 */
class GetPlatformInfoUseCaseImplTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PlatformRepository platformRepository;

    private GetPlatformInfoUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetPlatformInfoUseCaseImpl(tenantRepository, platformRepository);
    }

    @Test
    void execute_retornaEstadisticasCorrectas() {
        // Arrange: 2 tenants activos, 1 inactivo
        Tenant activeTenant1 = new Tenant("idoneo", "IDONEO", "Comida saludable", null, null, null, null, null, List.of(), List.of(), null, null, null, null, null, null, true, null, null, null, null, Instant.now(), Instant.now());
        Tenant activeTenant2 = new Tenant("chicha", "CHICHA", "Chicha costeña", null, null, null, null, null, List.of(), List.of(), null, null, null, null, null, null, true, null, null, null, null, Instant.now(), Instant.now());
        Tenant inactiveTenant = new Tenant("inactive", "Inactive", "Inactiva", null, null, null, null, null, List.of(), List.of(), null, null, null, null, null, null, false, null, null, null, null, Instant.now(), Instant.now());

        when(tenantRepository.findAll()).thenReturn(List.of(activeTenant1, activeTenant2, inactiveTenant));
        when(platformRepository.countTotalProducts()).thenReturn(45);
        when(platformRepository.countTotalOrders()).thenReturn(120);

        // Act
        PlatformInfo info = useCase.execute();

        // Assert
        assertEquals(2, info.totalStores());
        assertEquals(45, info.totalProducts());
        assertEquals(120, info.totalOrders());
    }

    @Test
    void execute_sinTenants_retornaCeros() {
        when(tenantRepository.findAll()).thenReturn(List.of());
        when(platformRepository.countTotalProducts()).thenReturn(0);
        when(platformRepository.countTotalOrders()).thenReturn(0);

        PlatformInfo info = useCase.execute();

        assertEquals(0, info.totalStores());
        assertEquals(0, info.totalProducts());
        assertEquals(0, info.totalOrders());
    }

    @Test
    void execute_usaCache_noRepiteQueries() {
        Tenant tenant = new Tenant("idoneo", "IDONEO", "Comida", null, null, null, null, null, List.of(), List.of(), null, null, null, null, null, null, true, null, null, null, null, Instant.now(), Instant.now());
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(platformRepository.countTotalProducts()).thenReturn(10);
        when(platformRepository.countTotalOrders()).thenReturn(5);

        // Primera llamada: ejecuta las queries
        useCase.execute();
        // Segunda llamada: debe usar cache
        useCase.execute();

        // Solo se debería haber llamado una vez a cada repositorio
        verify(tenantRepository, times(1)).findAll();
        verify(platformRepository, times(1)).countTotalProducts();
        verify(platformRepository, times(1)).countTotalOrders();
    }
}
