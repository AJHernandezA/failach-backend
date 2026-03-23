package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.constants.ApiConstants;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Filtro de rate limiting por IP.
 * Usa ventana deslizante en memoria con ConcurrentHashMap.
 * Dos niveles: general (GET) y estricto (POST/PUT/DELETE en rutas críticas).
 * Limpia entries expiradas cada 5 minutos para evitar memory leaks.
 */
public class RateLimiterFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterFilter.class);

    /** Requests máximos por minuto para endpoints de lectura */
    private static final int GENERAL_LIMIT = 60;

    /** Requests máximos por minuto para endpoints de escritura críticos */
    private static final int WRITE_LIMIT = 30;

    /** Ventana de tiempo en milisegundos (1 minuto) */
    private static final long WINDOW_MS = 60_000L;

    /** Intervalo de limpieza de entries expiradas (5 minutos) */
    private static final long CLEANUP_INTERVAL_MS = 300_000L;

    /** Prefijos de rutas de escritura con límite estricto */
    private static final Set<String> STRICT_ROUTE_SUFFIXES = Set.of(
            "/orders",
            "/cart",
            "/auth/login");

    /** Rutas excluidas del rate limiting */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            ApiConstants.API_PREFIX + "/health");

    /** Prefijos de rutas excluidas */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            ApiConstants.API_PREFIX + "/webhooks/");

    /** Registro de timestamps de requests por IP */
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Long>> requestLog = new ConcurrentHashMap<>();

    /** Scheduler para limpieza automática de entries expiradas */
    private final ScheduledExecutorService cleanupScheduler;

    public RateLimiterFilter() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS);
        log.info("RateLimiterFilter iniciado. General: {}/min, Escritura: {}/min", GENERAL_LIMIT, WRITE_LIMIT);

        // Shutdown hook para cerrar el scheduler limpiamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanupScheduler.shutdownNow();
            log.info("RateLimiterFilter cleanup scheduler detenido");
        }));
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // No aplicar a preflight CORS (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            return;
        }

        String path = ctx.path();

        // No aplicar a rutas excluidas
        if (isExcluded(path)) {
            return;
        }

        String ip = extractIp(ctx);
        long now = System.currentTimeMillis();
        String method = ctx.method().name();

        // Determinar límite según tipo de ruta
        int limit = isStrictRoute(path, method) ? WRITE_LIMIT : GENERAL_LIMIT;
        String rateLimitKey = ip + ":" + (limit == WRITE_LIMIT ? "write" : "general");

        // Obtener o crear lista de timestamps para esta IP
        CopyOnWriteArrayList<Long> timestamps = requestLog.computeIfAbsent(rateLimitKey,
                k -> new CopyOnWriteArrayList<>());

        // Limpiar timestamps fuera de la ventana
        long windowStart = now - WINDOW_MS;
        timestamps.removeIf(ts -> ts < windowStart);

        // Verificar límite
        if (timestamps.size() >= limit) {
            log.warn("Rate limit excedido para IP={} en ruta={} ({}/{})", ip, path, timestamps.size(), limit);
            ctx.status(429)
                    .header("Retry-After", "60")
                    .json(Map.of(
                            "error", Map.of(
                                    "code", "RATE_LIMIT_EXCEEDED",
                                    "message", "Demasiados requests. Intente de nuevo en un momento.")));
            ctx.skipRemainingHandlers();
            return;
        }

        // Registrar este request
        timestamps.add(now);
    }

    /**
     * Extrae la IP del cliente, considerando proxies (X-Forwarded-For).
     */
    private String extractIp(Context ctx) {
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return ctx.ip();
    }

    /**
     * Determina si la ruta requiere límite estricto de escritura.
     */
    private boolean isStrictRoute(String path, String method) {
        if ("GET".equalsIgnoreCase(method)) {
            return false;
        }
        for (String suffix : STRICT_ROUTE_SUFFIXES) {
            if (path.contains(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica si la ruta está excluida del rate limiting.
     */
    private boolean isExcluded(String path) {
        if (EXCLUDED_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Limpia entries expiradas del mapa de requests.
     * Se ejecuta automáticamente cada 5 minutos.
     */
    private void cleanup() {
        long windowStart = System.currentTimeMillis() - WINDOW_MS;
        int cleaned = 0;

        Iterator<Map.Entry<String, CopyOnWriteArrayList<Long>>> it = requestLog.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CopyOnWriteArrayList<Long>> entry = it.next();
            entry.getValue().removeIf(ts -> ts < windowStart);
            if (entry.getValue().isEmpty()) {
                it.remove();
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.debug("RateLimiter cleanup: {} entries eliminadas", cleaned);
        }
    }
}
