package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.ContactMessage;
import com.projectx.backend.domain.ports.out.PlatformRepository;
import com.projectx.backend.domain.ports.out.ProductRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del repositorio de la plataforma.
 * Almacena mensajes de contacto y calcula estadísticas agregadas
 * consultando los repositorios de tenants, productos y pedidos.
 */
public class InMemoryPlatformRepository implements PlatformRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryPlatformRepository.class);

    private final Map<String, ContactMessage> contactMessages = new ConcurrentHashMap<>();
    private final TenantRepository tenantRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Inject
    public InMemoryPlatformRepository(
            TenantRepository tenantRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository) {
        this.tenantRepository = tenantRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void saveContactMessage(ContactMessage message) {
        contactMessages.put(message.id(), message);
        log.info("[InMemory] Mensaje de contacto guardado: {} de {}", message.id(), message.email());
    }

    @Override
    public int countTotalOrders() {
        // Sumar pedidos de todos los tenants activos
        return tenantRepository.findAll().stream()
                .filter(t -> t.isActive())
                .mapToInt(t -> {
                    try {
                        var filter = new com.projectx.backend.domain.models.OrderFilter(null, null, 0, 1);
                        var page = orderRepository.findByTenant(t.tenantId(), filter);
                        return (int) page.total();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
    }

    @Override
    public int countTotalProducts() {
        // Sumar productos de todos los tenants activos
        return tenantRepository.findAll().stream()
                .filter(t -> t.isActive())
                .mapToInt(t -> productRepository.countByTenantId(t.tenantId()))
                .sum();
    }
}
