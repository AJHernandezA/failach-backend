package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.constants.ApiConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Filtro de detección de bots basado en scoring de sospecha.
 * Evalúa señales como User-Agent, headers faltantes, honeypot y timing de
 * formularios.
 * Score >= 5: bloquear (429). Score 3-4: permitir pero loguear. Score 0-2:
 * normal.
 * No afecta la experiencia del usuario legítimo.
 */
public class BotDetectionFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(BotDetectionFilter.class);

    /** Umbral para bloquear el request */
    private static final int BLOCK_THRESHOLD = 5;

    /** Umbral para loguear como sospechoso */
    private static final int WARN_THRESHOLD = 3;

    /** Tiempo mínimo esperado para llenar un formulario (ms) */
    private static final long MIN_FORM_TIME_MS = 2000;

    /** Fragmentos de User-Agent conocidos como bots */
    private static final Set<String> BOT_USER_AGENTS = Set.of(
            "bot", "crawler", "spider", "scraper", "curl", "wget",
            "python-requests", "httpclient", "java/", "go-http",
            "scrapy", "phantomjs", "headless", "selenium");

    /** Rutas excluidas de la detección */
    private static final Set<String> EXCLUDED_PATHS = Set.of(
            ApiConstants.API_PREFIX + "/health");

    /** Prefijos excluidos */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            ApiConstants.API_PREFIX + "/webhooks/");

    private final ObjectMapper objectMapper;

    public BotDetectionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("BotDetectionFilter iniciado. Bloqueo >= {} puntos, Advertencia >= {} puntos", BLOCK_THRESHOLD,
                WARN_THRESHOLD);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        // No aplicar a preflight CORS (OPTIONS no envía headers como User-Agent,
        // Accept-Language)
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            return;
        }

        String path = ctx.path();

        // No aplicar a rutas excluidas
        if (isExcluded(path)) {
            return;
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // 1. Evaluar User-Agent
        String userAgent = ctx.header("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            score += 2;
            reasons.append("ua_empty ");
        } else {
            String uaLower = userAgent.toLowerCase();
            for (String botToken : BOT_USER_AGENTS) {
                if (uaLower.contains(botToken)) {
                    score += 3;
                    reasons.append("ua_bot(").append(botToken).append(") ");
                    break;
                }
            }
        }

        // 2. Evaluar Accept-Language
        String acceptLanguage = ctx.header("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            score += 1;
            reasons.append("no_lang ");
        }

        // 3. Evaluar Referer en POST (un browser real siempre envía Referer en forms)
        String method = ctx.method().name();
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            String referer = ctx.header("Referer");
            if (referer == null || referer.isBlank()) {
                score += 1;
                reasons.append("no_referer ");
            }

            // 4. Evaluar honeypot y form timing del body (solo en POST con JSON)
            String contentType = ctx.header("Content-Type");
            if (contentType != null && contentType.contains("application/json")) {
                try {
                    String body = ctx.body();
                    if (!body.isBlank()) {
                        JsonNode json = objectMapper.readTree(body);

                        // Honeypot: campo "hp" debe estar vacío
                        JsonNode hpNode = json.get("hp");
                        if (hpNode != null && !hpNode.asText().isBlank()) {
                            score += 5;
                            reasons.append("honeypot_filled ");
                        }

                        // Form timing: campo "ft" en milisegundos
                        JsonNode ftNode = json.get("ft");
                        if (ftNode != null && ftNode.isNumber()) {
                            long formTime = ftNode.asLong();
                            if (formTime > 0 && formTime < MIN_FORM_TIME_MS) {
                                score += 2;
                                reasons.append("fast_form(").append(formTime).append("ms) ");
                            }
                        }
                    }
                } catch (Exception e) {
                    // Si no se puede parsear, no sumar puntos por esto
                    log.debug("No se pudo analizar body para bot detection: {}", e.getMessage());
                }
            }
        }

        // Evaluar resultado
        String ip = extractIp(ctx);

        if (score >= BLOCK_THRESHOLD) {
            log.error("BOT BLOQUEADO: IP={} path={} score={} razones=[{}]", ip, path, score, reasons.toString().trim());
            ctx.status(429).json(Map.of(
                    "error", Map.of(
                            "code", "SUSPICIOUS_REQUEST",
                            "message", "Request bloqueado por actividad sospechosa.")));
            ctx.skipRemainingHandlers();
            return;
        }

        if (score >= WARN_THRESHOLD) {
            log.warn("Request sospechoso: IP={} path={} score={} razones=[{}]", ip, path, score,
                    reasons.toString().trim());
        }
    }

    /**
     * Extrae la IP del cliente, considerando proxies.
     */
    private String extractIp(Context ctx) {
        String forwarded = ctx.header("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return ctx.ip();
    }

    /**
     * Verifica si la ruta está excluida de la detección.
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
}
