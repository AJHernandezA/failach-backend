package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.PlatformInfo;

/**
 * Puerto de entrada para obtener las estadísticas públicas de la plataforma.
 * Usado en la landing page para mostrar números de confianza.
 */
public interface GetPlatformInfoUseCase {
    PlatformInfo execute();
}
