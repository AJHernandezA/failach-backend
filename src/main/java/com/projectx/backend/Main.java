package com.projectx.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.infra.adapters.in.controller.AuthController;
import com.projectx.backend.infra.adapters.in.controller.CartController;
import com.projectx.backend.infra.adapters.in.controller.CategoryController;
import com.projectx.backend.infra.adapters.in.controller.OrderController;
import com.projectx.backend.infra.adapters.in.controller.ManualPaymentController;
import com.projectx.backend.infra.adapters.in.controller.WompiController;
import com.projectx.backend.infra.adapters.in.controller.HealthController;
import com.projectx.backend.infra.adapters.in.controller.ProductController;
import com.projectx.backend.infra.adapters.in.controller.LegalController;
import com.projectx.backend.infra.adapters.in.controller.SearchController;
import com.projectx.backend.infra.adapters.in.controller.SitemapController;
import com.projectx.backend.infra.adapters.in.controller.AdminDashboardController;
import com.projectx.backend.infra.adapters.in.controller.AdminOrderController;
import com.projectx.backend.infra.adapters.in.controller.AdminProductController;
import com.projectx.backend.infra.adapters.in.controller.PlatformController;
import com.projectx.backend.infra.adapters.in.controller.StorefrontController;
import com.projectx.backend.infra.adapters.in.controller.SwaggerController;
import com.projectx.backend.infra.adapters.in.controller.TenantController;
import com.projectx.backend.infra.seed.ProductSeeder;
import com.projectx.backend.infra.seed.TenantSeeder;
import com.projectx.backend.infra.config.AppConfig;
import com.projectx.backend.infra.config.ExceptionHandlerConfig;
import com.projectx.backend.infra.config.InfraModule;
import com.projectx.backend.infra.config.UseCaseModule;
import com.projectx.backend.infra.middleware.AuthFilter;
import com.projectx.backend.infra.middleware.BotDetectionFilter;
import com.projectx.backend.infra.middleware.MockAuthFilter;
import com.projectx.backend.infra.middleware.RateLimiterFilter;
import com.projectx.backend.infra.middleware.SecurityHeadersFilter;
import com.projectx.backend.infra.middleware.TenantFilter;
import io.javalin.http.Handler;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada de la aplicación Project-X Backend.
 * Configura Javalin, Guice, middleware, controllers y exception handlers.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Cargar configuración
        AppConfig appConfig = new AppConfig();

        // Configurar Guice (inyección de dependencias)
        Injector injector = Guice.createInjector(
                new UseCaseModule(),
                new InfraModule(appConfig));

        // Configurar Jackson para serialización JSON
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Crear instancia de Javalin
        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(objectMapper, false));
            config.http.defaultContentType = "application/json";
            config.http.maxRequestSize = 1_048_576L; // 1MB — protección contra payloads abusivos

            // Configurar CORS
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(rule -> {
                    for (String origin : appConfig.getCorsAllowedOrigins()) {
                        rule.allowHost(origin.trim());
                    }
                    rule.allowCredentials = true;
                });
            });
        });

        // F019: Registrar middleware de rate limiting (primero que todo)
        app.before(new RateLimiterFilter());

        // Registrar middleware multi-tenant (se ejecuta antes de cada request)
        app.before(new TenantFilter());

        // F019: Registrar middleware de detección de bots
        app.before(new BotDetectionFilter(objectMapper));

        // Configurar filtro de autenticación (mock en desarrollo, JWT en producción)
        Handler authFilter;
        if (appConfig.isAuthMockEnabled()) {
            log.info("[AUTH] Modo MOCK habilitado. Usar headers X-Mock-Role, X-Mock-Email, X-Mock-TenantId");
            authFilter = new MockAuthFilter();
        } else {
            String poolId = appConfig.getCognitoUserPoolId();
            String region = appConfig.getAwsRegion();
            log.info("[AUTH] Modo JWT/Cognito. UserPool: {}", poolId);
            authFilter = new AuthFilter(region, poolId);
        }

        // Aplicar auth filter a rutas protegidas
        app.before(ApiConstants.API_PREFIX + "/auth/me", ctx -> {
            if (!"OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/config", ctx -> {
            if ("PUT".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        // F003: Proteger POST/PUT/DELETE de productos y POST de categorías
        app.before(ApiConstants.API_PREFIX + "/tenants/*/products", ctx -> {
            String method = ctx.method().name();
            if ("POST".equalsIgnoreCase(method)) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/products/*", ctx -> {
            String method = ctx.method().name();
            if ("PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/categories", ctx -> {
            if ("POST".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        // F011: Proteger endpoints admin de órdenes (GET lista, PUT
        // estado/confirm/cancel)
        app.before(ApiConstants.API_PREFIX + "/tenants/*/orders", ctx -> {
            if ("GET".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/orders/*/status", ctx -> {
            if (!"OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/orders/*/confirm-payment", ctx -> {
            if (!"OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        app.before(ApiConstants.API_PREFIX + "/tenants/*/orders/*/cancel", ctx -> {
            if (!"OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });
        // F018: Proteger PUT de contenido legal
        app.before(ApiConstants.API_PREFIX + "/tenants/*/legal/*", ctx -> {
            if ("PUT".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });

        // F036/F037: Proteger TODOS los endpoints /admin/* (requieren auth siempre)
        app.before(ApiConstants.API_PREFIX + "/admin/*", ctx -> {
            if (!"OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                authFilter.handle(ctx);
            }
        });

        // F019: Registrar headers de seguridad en cada response
        app.after(new SecurityHeadersFilter());

        // Registrar manejo global de excepciones
        ExceptionHandlerConfig.configure(app);

        // Registrar controllers
        HealthController.register(app);
        injector.getInstance(AuthController.class).register(app);
        injector.getInstance(TenantController.class).register(app);
        injector.getInstance(SearchController.class).register(app);
        injector.getInstance(ProductController.class).register(app);
        injector.getInstance(CategoryController.class).register(app);
        injector.getInstance(CartController.class).register(app);
        injector.getInstance(OrderController.class).register(app);
        injector.getInstance(WompiController.class).register(app);
        injector.getInstance(ManualPaymentController.class).register(app);
        injector.getInstance(StorefrontController.class).register(app);
        injector.getInstance(SitemapController.class).register(app);
        injector.getInstance(LegalController.class).register(app);
        injector.getInstance(AdminProductController.class).register(app);
        injector.getInstance(AdminOrderController.class).register(app);
        injector.getInstance(AdminDashboardController.class).register(app);
        injector.getInstance(PlatformController.class).register(app);
        SwaggerController.register(app);

        // Seed de datos iniciales (solo en desarrollo)
        try {
            injector.getInstance(TenantSeeder.class).seed();
            injector.getInstance(ProductSeeder.class).seed();
        } catch (Exception e) {
            log.warn("No se pudo ejecutar seed: {}", e.getMessage());
        }

        // Iniciar servidor
        int port = appConfig.getServerPort();
        app.start(port);
        log.info("=== Project-X Backend iniciado en puerto {} ===", port);
        log.info("Health check: http://localhost:{}{}/health", port, ApiConstants.API_PREFIX);
    }
}
