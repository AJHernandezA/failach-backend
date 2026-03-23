package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.StorePreview;

import java.util.List;

/**
 * Puerto de entrada para listar las tiendas activas de la plataforma.
 * Solo retorna tiendas con estado activo y datos no sensibles.
 */
public interface ListPublicStoresUseCase {
    List<StorePreview> execute(String category, String search, String sort);
}
