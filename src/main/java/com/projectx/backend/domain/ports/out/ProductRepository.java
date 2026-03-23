package com.projectx.backend.domain.ports.out;

import com.projectx.backend.domain.models.Page;
import com.projectx.backend.domain.models.Product;
import com.projectx.backend.domain.models.ProductFilter;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de productos.
 */
public interface ProductRepository {

    Optional<Product> findById(String tenantId, String productId);

    Page<Product> findByTenant(String tenantId, ProductFilter filter);

    List<Product> findByCategory(String tenantId, String categoryId);

    void save(Product product);

    void delete(String tenantId, String productId);

    /**
     * Cuenta el total de productos activos de un tenant.
     *
     * @param tenantId identificador del tenant
     * @return cantidad de productos activos
     */
    int countByTenantId(String tenantId);
}
