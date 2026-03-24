package com.projectx.backend.infra.adapters.in.controller;

import com.google.inject.Inject;
import com.projectx.backend.domain.constants.ApiConstants;
import com.projectx.backend.domain.models.AuthenticatedUser;
import com.projectx.backend.domain.ports.out.AdminNotificationService;
import com.projectx.backend.domain.ports.out.AuthService;
import com.projectx.backend.infra.middleware.RoleEnforcer;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller para endpoints de autenticación.
 * Actúa como proxy entre el frontend y AWS Cognito (o mock en desarrollo).
 * Incluye: register, confirm, login, logout, refresh, forgot/reset password,
 * me.
 */
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AdminNotificationService adminNotificationService;

    @Inject
    public AuthController(AuthService authService, AdminNotificationService adminNotificationService) {
        this.authService = authService;
        this.adminNotificationService = adminNotificationService;
    }

    /**
     * Registra las rutas de autenticación en Javalin.
     */
    public void register(Javalin app) {
        String prefix = ApiConstants.API_PREFIX + "/auth";

        // POST /api/v1/auth/register — Registrar nuevo vendedor
        app.post(prefix + "/register", ctx -> {
            var body = ctx.bodyAsClass(RegisterRequest.class);

            // Usar username del request o generar slug del nombre del negocio como fallback
            String tenantSlug = (body.username() != null && !body.username().isBlank())
                    ? body.username().toLowerCase().trim()
                    : body.businessName().toLowerCase()
                            .replaceAll("[^a-z0-9\\s-]", "")
                            .replaceAll("\\s+", "-")
                            .replaceAll("-+", "-")
                            .trim();

            String sub = authService.register(
                    body.email(), body.password(), body.name(),
                    body.phone(), tenantSlug, "MERCHANT");

            log.info("Nuevo vendedor registrado: {} / {} (tenant: {})", body.username(), body.email(), tenantSlug);

            // F045: Notificar al admin sobre nuevo registro
            try {
                adminNotificationService.notifyAccountCreated(body.email(), body.name(), tenantSlug);
            } catch (Exception e) {
                log.warn("Error al notificar al admin sobre nuevo registro: {}", e.getMessage());
            }

            ctx.status(201).json(Map.of("data", Map.of(
                    "message", "Registro exitoso. Revisa tu email para confirmar tu cuenta.",
                    "email", body.email(),
                    "sub", sub)));
        });

        // POST /api/v1/auth/confirm — Confirmar email con código
        app.post(prefix + "/confirm", ctx -> {
            var body = ctx.bodyAsClass(ConfirmRequest.class);
            authService.confirmEmail(body.email(), body.code());
            ctx.json(Map.of("data", Map.of("message", "Email confirmado exitosamente. Ya puedes iniciar sesión.")));
        });

        // POST /api/v1/auth/login — Iniciar sesión
        app.post(prefix + "/login", ctx -> {
            var body = ctx.bodyAsClass(LoginRequest.class);
            Map<String, Object> result = authService.login(body.email(), body.password());

            // En producción, los tokens se setean en cookies httpOnly
            // En desarrollo, se retornan en el body para simplicidad
            ctx.json(Map.of("data", result));
        });

        // POST /api/v1/auth/logout — Cerrar sesión
        app.post(prefix + "/logout", ctx -> {
            String authHeader = ctx.header("Authorization");
            String token = authHeader != null && authHeader.startsWith("Bearer ")
                    ? authHeader.substring(7)
                    : "";
            authService.logout(token);
            ctx.json(Map.of("data", Map.of("message", "Sesión cerrada")));
        });

        // POST /api/v1/auth/refresh — Refrescar token
        app.post(prefix + "/refresh", ctx -> {
            var body = ctx.bodyAsClass(RefreshRequest.class);
            Map<String, Object> result = authService.refreshToken(body.refreshToken());
            ctx.json(Map.of("data", result));
        });

        // POST /api/v1/auth/forgot-password — Solicitar código de recuperación
        app.post(prefix + "/forgot-password", ctx -> {
            var body = ctx.bodyAsClass(EmailOnlyRequest.class);
            authService.forgotPassword(body.email());
            ctx.json(Map.of("data", Map.of(
                    "message", "Si el email existe, recibirás un código de recuperación.")));
        });

        // POST /api/v1/auth/reset-password — Cambiar contraseña con código
        app.post(prefix + "/reset-password", ctx -> {
            var body = ctx.bodyAsClass(ResetPasswordRequest.class);
            authService.resetPassword(body.email(), body.code(), body.newPassword());
            ctx.json(Map.of("data", Map.of("message", "Contraseña actualizada exitosamente.")));
        });

        // POST /api/v1/auth/resend-code — Reenviar código de verificación
        app.post(prefix + "/resend-code", ctx -> {
            var body = ctx.bodyAsClass(EmailOnlyRequest.class);
            authService.resendConfirmationCode(body.email());
            ctx.json(Map.of("data", Map.of("message", "Código reenviado.")));
        });

        // GET /api/v1/auth/me — Obtener usuario autenticado (requiere auth)
        app.get(prefix + "/me", ctx -> {
            AuthenticatedUser user = RoleEnforcer.getUser(ctx);
            ctx.json(Map.of("data", Map.of(
                    "sub", user.sub(),
                    "email", user.email(),
                    "role", user.role().name(),
                    "tenantId", user.tenantId() != null ? user.tenantId() : "")));
        });

        log.info(
                "Auth endpoints registrados: register, confirm, login, logout, refresh, forgot, reset, resend, me en {}/auth/*",
                ApiConstants.API_PREFIX);
    }

    // DTOs para requests de autenticación
    private record RegisterRequest(String name, String email, String username, String phone, String businessName,
            String password) {
    }

    private record ConfirmRequest(String email, String code) {
    }

    private record LoginRequest(String email, String password) {
    }

    private record RefreshRequest(String refreshToken) {
    }

    private record EmailOnlyRequest(String email) {
    }

    private record ResetPasswordRequest(String email, String code, String newPassword) {
    }
}
