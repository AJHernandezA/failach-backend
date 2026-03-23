package com.projectx.backend.domain.constants;

/**
 * Constantes globales de la API.
 * Centraliza valores reutilizados en todo el proyecto.
 */
public final class ApiConstants {

    private ApiConstants() {
        // No instanciable
    }

    /** Prefijo base de todas las rutas de la API */
    public static final String API_PREFIX = "/api/v1";

    /** Tamaño de página por defecto para consultas paginadas */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Tamaño máximo de página permitido */
    public static final int MAX_PAGE_SIZE = 100;

    /** Nombre del header que contiene el identificador del tenant */
    public static final String TENANT_HEADER = "X-Tenant-Id";

    /** Nombre del atributo en el contexto de Javalin para el tenantId */
    public static final String TENANT_ATTRIBUTE = "tenantId";

    /** Nombre del atributo en el contexto de Javalin para el usuario autenticado */
    public static final String USER_ATTRIBUTE = "authenticatedUser";
}
