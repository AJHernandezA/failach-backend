package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.ContactMessage;

/**
 * Puerto de salida para persistencia de datos de la plataforma.
 * Incluye mensajes de contacto y estadísticas.
 */
public interface PlatformRepository {

    /**
     * Guarda un mensaje de contacto recibido desde la landing page.
     *
     * @param message mensaje a persistir
     */
    void saveContactMessage(ContactMessage message);

    /**
     * Cuenta el total de pedidos completados en toda la plataforma.
     *
     * @return cantidad total de pedidos
     */
    int countTotalOrders();

    /**
     * Cuenta el total de productos activos en toda la plataforma.
     *
     * @return cantidad total de productos
     */
    int countTotalProducts();
}
