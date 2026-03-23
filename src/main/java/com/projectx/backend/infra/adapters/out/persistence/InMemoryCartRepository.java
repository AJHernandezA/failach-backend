package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.Cart;
import com.projectx.backend.domain.ports.out.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria del repositorio de carritos.
 * Para desarrollo sin DynamoDB Local.
 */
public class InMemoryCartRepository implements CartRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryCartRepository.class);

    /** Key: tenantId::sessionId */
    private final Map<String, Cart> store = new ConcurrentHashMap<>();

    private String key(String tenantId, String sessionId) {
        return tenantId + "::" + sessionId;
    }

    @Override
    public Optional<Cart> findById(String tenantId, String sessionId) {
        log.debug("[InMemory] Buscando carrito: {}:{}", tenantId, sessionId);
        Cart cart = store.get(key(tenantId, sessionId));
        // Simular TTL: verificar si el carrito ha expirado
        if (cart != null && cart.ttl() < java.time.Instant.now().getEpochSecond()) {
            log.debug("[InMemory] Carrito expirado, eliminando: {}:{}", tenantId, sessionId);
            store.remove(key(tenantId, sessionId));
            return Optional.empty();
        }
        return Optional.ofNullable(cart);
    }

    @Override
    public void save(Cart cart) {
        log.debug("[InMemory] Guardando carrito: {}:{} con {} ítems", cart.tenantId(), cart.cartId(), cart.items().size());
        store.put(key(cart.tenantId(), cart.cartId()), cart);
    }

    @Override
    public void delete(String tenantId, String sessionId) {
        log.debug("[InMemory] Eliminando carrito: {}:{}", tenantId, sessionId);
        store.remove(key(tenantId, sessionId));
    }
}
