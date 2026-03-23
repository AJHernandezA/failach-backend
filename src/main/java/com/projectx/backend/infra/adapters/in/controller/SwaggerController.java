package com.projectx.backend.infra.adapters.in.controller;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Controller para servir la especificación OpenAPI y la interfaz Swagger UI.
 * Acceder a http://localhost:7070/swagger para ver la documentación interactiva.
 */
public class SwaggerController {

    private static final Logger log = LoggerFactory.getLogger(SwaggerController.class);

    private SwaggerController() {
        // No instanciable
    }

    /**
     * Registra las rutas de Swagger en la instancia de Javalin.
     */
    public static void register(Javalin app) {
        // GET /swagger — Swagger UI (HTML)
        app.get("/swagger", ctx -> {
            ctx.contentType(ContentType.HTML);
            ctx.result(SWAGGER_HTML);
        });

        // GET /openapi.yaml — Especificación OpenAPI en YAML
        app.get("/openapi.yaml", ctx -> {
            try (InputStream is = SwaggerController.class.getClassLoader().getResourceAsStream("openapi.yaml")) {
                if (is != null) {
                    String yaml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    ctx.contentType("application/x-yaml");
                    ctx.result(yaml);
                } else {
                    ctx.status(404).result("openapi.yaml no encontrado");
                }
            }
        });

        log.info("Swagger UI disponible en /swagger | OpenAPI spec en /openapi.yaml");
    }

    /**
     * HTML de Swagger UI usando CDN de swagger-ui-dist.
     * No requiere dependencias adicionales.
     */
    private static final String SWAGGER_HTML = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Project-X API — Swagger UI</title>
                <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui.css">
                <style>
                    body { margin: 0; padding: 0; }
                    #swagger-ui { max-width: 1200px; margin: 0 auto; }
                    .topbar { display: none; }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui-bundle.js"></script>
                <script>
                    SwaggerUIBundle({
                        url: '/openapi.yaml',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIBundle.SwaggerUIStandalonePreset
                        ],
                        layout: "BaseLayout",
                        defaultModelsExpandDepth: 1,
                        docExpansion: "list",
                        filter: true,
                        tryItOutEnabled: true
                    });
                </script>
            </body>
            </html>
            """;
}
