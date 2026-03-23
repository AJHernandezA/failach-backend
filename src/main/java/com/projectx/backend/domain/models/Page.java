package com.projectx.backend.domain.models;

import java.util.List;

/**
 * Contenedor genérico de resultados paginados.
 * Usado por todos los repositorios que retornan listas paginadas.
 *
 * @param items lista de elementos de la página actual
 * @param page  número de página (0-indexed)
 * @param size  tamaño de la página solicitada
 * @param total cantidad total de elementos en la colección
 */
public record Page<T>(
        List<T> items,
        int page,
        int size,
        long total
) {

    /**
     * Indica si existe una página siguiente.
     */
    public boolean hasNext() {
        return (long) (page + 1) * size < total;
    }
}
