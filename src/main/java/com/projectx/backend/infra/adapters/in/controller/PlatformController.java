package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.ContactMessage;
import com.projectx.backend.domain.models.PlatformInfo;
import com.projectx.backend.domain.models.StorePreview;
import com.projectx.backend.domain.ports.in.GetPlatformInfoUseCase;
import com.projectx.backend.domain.ports.in.ListPublicStoresUseCase;
import com.projectx.backend.domain.ports.in.SendContactMessageUseCase;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Controller para los endpoints públicos de la plataforma.
 * Sirve la landing page con estadísticas, lista de tiendas y formulario de contacto.
 * Estos endpoints NO requieren autenticación ni X-Tenant-Id.
 */
public class PlatformController {

    private static final Logger log = LoggerFactory.getLogger(PlatformController.class);

    private final GetPlatformInfoUseCase getPlatformInfo;
    private final ListPublicStoresUseCase listPublicStores;
    private final SendContactMessageUseCase sendContactMessage;

    @Inject
    public PlatformController(
            GetPlatformInfoUseCase getPlatformInfo,
            ListPublicStoresUseCase listPublicStores,
            SendContactMessageUseCase sendContactMessage) {
        this.getPlatformInfo = getPlatformInfo;
        this.listPublicStores = listPublicStores;
        this.sendContactMessage = sendContactMessage;
    }

    /**
     * Registra las rutas de la plataforma en Javalin.
     */
    public void register(Javalin app) {
        String prefix = ApiConstants.API_PREFIX + "/platform";

        // GET /api/v1/platform/info — Estadísticas públicas de la plataforma
        app.get(prefix + "/info", this::handleGetInfo);

        // GET /api/v1/platform/stores — Lista de tiendas activas
        app.get(prefix + "/stores", this::handleListStores);

        // GET /api/v1/platform/stores/:tenantId — Perfil público de una tienda
        app.get(prefix + "/stores/{tenantId}", this::handleGetStoreProfile);

        // POST /api/v1/platform/contact — Enviar mensaje de contacto
        app.post(prefix + "/contact", this::handleContact);

        log.info("Platform endpoints registrados: GET {}/info, GET {}/stores, POST {}/contact", prefix, prefix, prefix);
    }

    /**
     * GET /api/v1/platform/info
     * Retorna estadísticas públicas: total tiendas, productos, pedidos.
     */
    private void handleGetInfo(Context ctx) {
        PlatformInfo info = getPlatformInfo.execute();
        ctx.json(Map.of("data", info));
    }

    /**
     * GET /api/v1/platform/stores
     * Lista tiendas activas con filtros opcionales.
     */
    private void handleListStores(Context ctx) {
        String category = ctx.queryParam("category");
        String search = ctx.queryParam("search");
        String sort = ctx.queryParam("sort");

        List<StorePreview> stores = listPublicStores.execute(category, search, sort);
        ctx.json(Map.of("data", stores));
    }

    /**
     * GET /api/v1/platform/stores/:tenantId
     * Perfil público de una tienda. Reutiliza ListPublicStores y filtra por tenantId.
     */
    private void handleGetStoreProfile(Context ctx) {
        String tenantId = ctx.pathParam("tenantId");
        List<StorePreview> stores = listPublicStores.execute(null, null, null);

        StorePreview found = stores.stream()
                .filter(s -> s.tenantId().equals(tenantId))
                .findFirst()
                .orElse(null);

        if (found == null) {
            ctx.status(404).json(Map.of("error", Map.of("code", "NOT_FOUND", "message", "Tienda no encontrada")));
            return;
        }

        ctx.json(Map.of("data", found));
    }

    /**
     * POST /api/v1/platform/contact
     * Recibe mensaje del formulario de contacto de la landing page.
     */
    private void handleContact(Context ctx) {
        var body = ctx.bodyAsClass(ContactRequest.class);
        String ip = ctx.ip();

        ContactMessage message = sendContactMessage.execute(
                body.name(), body.email(), body.phone(), body.message(), ip
        );

        ctx.status(201).json(Map.of("data", Map.of("id", message.id(), "message", "Mensaje enviado exitosamente")));
    }

    /**
     * DTO para el request del formulario de contacto.
     */
    private record ContactRequest(String name, String email, String phone, String message) {}
}
