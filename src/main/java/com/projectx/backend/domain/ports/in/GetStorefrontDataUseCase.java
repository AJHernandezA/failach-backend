package com.projectx.backend.domain.ports.in;

import com.projectx.backend.domain.models.StorefrontData;

/**
 * Puerto de entrada para obtener toda la data de la homepage en un solo request.
 */
public interface GetStorefrontDataUseCase {
    StorefrontData execute(String tenantId);
}
