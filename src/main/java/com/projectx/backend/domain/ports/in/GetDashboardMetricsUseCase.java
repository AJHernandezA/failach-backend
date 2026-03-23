package com.projectx.backend.domain.ports.in;

import java.util.Map;

/**
 * Puerto de entrada para obtener métricas del dashboard de administración.
 * Retorna KPIs, pedidos recientes y productos más vendidos.
 */
public interface GetDashboardMetricsUseCase {
    Map<String, Object> execute(String tenantId, String from, String to);
}
