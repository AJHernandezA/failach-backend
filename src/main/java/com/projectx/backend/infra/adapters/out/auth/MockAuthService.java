package com.projectx.backend.infra.adapters.out.auth;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.UnauthorizedException;
import com.projectx.backend.domain.ports.out.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación mock del servicio de autenticación para desarrollo local.
 * Almacena usuarios en memoria y genera tokens fake.
 * En producción se reemplaza por CognitoAuthService.
 */
public class MockAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(MockAuthService.class);

    /** Almacén de usuarios mock: email -> datos del usuario */
    private final Map<String, MockUser> users = new ConcurrentHashMap<>();
    /** Índice de username → email para búsqueda por username */
    private final Map<String, String> usernameIndex = new ConcurrentHashMap<>();
    /** Índice de businessName → email para verificar unicidad */
    private final Map<String, String> businessNameIndex = new ConcurrentHashMap<>();

    public MockAuthService() {
        // Usuario admin (SUPER_ADMIN) — puede ver todas las tiendas
        addUser(new MockUser(UUID.randomUUID().toString(), "admin", "adolfohernandezaponte@hotmail.com",
                "12345adolfo", "Adolfo Hernandez", "+573145429669", "admin", "Project-X Admin", "SUPER_ADMIN", true));
        // Usuario seller (MERCHANT) — solo ve su tienda idoneo
        addUser(new MockUser(UUID.randomUUID().toString(), "seller", "seller@projectx.com",
                "12345adolfo", "Adolfo Seller", "+573145429669", "idoneo", "IDONEO", "MERCHANT", true));
        // Merchant para chicha
        addUser(new MockUser(UUID.randomUUID().toString(), "chicha_seller", "admin@chicha.com",
                "Chicha123!", "Admin CHICHA", "+573145429669", "chicha", "CHICHA Costeña", "MERCHANT", true));
        log.info("[MockAuth] Servicio de autenticación MOCK inicializado con usuarios: admin, seller, chicha_seller");
    }

    /** Registra un usuario en todos los índices */
    private void addUser(MockUser user) {
        users.put(user.email.toLowerCase(), user);
        usernameIndex.put(user.username.toLowerCase(), user.email.toLowerCase());
        if (user.businessName != null && !user.businessName.isBlank()) {
            businessNameIndex.put(user.businessName.toLowerCase(), user.email.toLowerCase());
        }
    }

    @Override
    public String register(String email, String password, String name, String phone, String tenantId, String role) {
        if (users.containsKey(email.toLowerCase())) {
            throw new BadRequestException("El email ya está registrado");
        }
        String username = tenantId;
        if (usernameIndex.containsKey(username.toLowerCase())) {
            throw new BadRequestException("El nombre de usuario ya está en uso");
        }
        if (businessNameIndex.containsKey(tenantId.toLowerCase())) {
            throw new BadRequestException("El nombre del negocio ya está registrado");
        }

        String sub = UUID.randomUUID().toString();
        MockUser newUser = new MockUser(sub, username, email.toLowerCase(), password, name, phone, tenantId, tenantId,
                role, false);
        addUser(newUser);
        log.info("[MockAuth] Usuario registrado: {} / {} (tenant: {}, role: {})", username, email, tenantId, role);
        return sub;
    }

    @Override
    public void confirmEmail(String email, String code) {
        MockUser user = users.get(email.toLowerCase());
        if (user == null) {
            throw new BadRequestException("Usuario no encontrado");
        }
        if (code == null || code.length() != 6) {
            throw new BadRequestException("Código inválido");
        }
        MockUser confirmed = new MockUser(user.sub, user.username, user.email, user.password, user.name, user.phone,
                user.tenantId, user.businessName, user.role, true);
        users.put(email.toLowerCase(), confirmed);
        log.info("[MockAuth] Email confirmado: {}", email);
    }

    @Override
    public Map<String, Object> login(String email, String password) {
        // Buscar por email directo o por username
        MockUser user = users.get(email.toLowerCase());
        if (user == null) {
            String emailFromUsername = usernameIndex.get(email.toLowerCase());
            if (emailFromUsername != null) {
                user = users.get(emailFromUsername);
            }
        }
        if (user == null || !user.password.equals(password)) {
            throw new UnauthorizedException("Credenciales inválidas");
        }
        if (!user.confirmed) {
            throw new BadRequestException("Email no confirmado. Revisa tu correo.");
        }

        String accessToken = "mock-access-" + user.sub + "-" + System.currentTimeMillis();
        String idToken = "mock-id-" + user.sub;
        String refreshToken = "mock-refresh-" + user.sub;

        log.info("[MockAuth] Login exitoso: {} / {} (role: {})", user.username, user.email, user.role);

        return Map.of(
                "accessToken", accessToken,
                "idToken", idToken,
                "refreshToken", refreshToken,
                "expiresIn", 3600,
                "user", Map.of(
                        "sub", user.sub,
                        "email", user.email,
                        "username", user.username,
                        "name", user.name,
                        "phone", user.phone,
                        "role", user.role,
                        "tenantId", user.tenantId != null ? user.tenantId : "",
                        "businessName", user.businessName != null ? user.businessName : "",
                        "status", "ACTIVE"));
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        // En mock, simplemente generamos un nuevo access token
        String newAccessToken = "mock-access-refreshed-" + System.currentTimeMillis();
        return Map.of("accessToken", newAccessToken, "expiresIn", 3600);
    }

    @Override
    public void forgotPassword(String email) {
        if (!users.containsKey(email.toLowerCase())) {
            // No revelar si el email existe o no (seguridad)
            log.info("[MockAuth] Forgot password solicitado para: {}", email);
            return;
        }
        log.info("[MockAuth] Código de recuperación enviado a: {}", email);
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        MockUser user = users.get(email.toLowerCase());
        if (user == null) {
            throw new BadRequestException("Usuario no encontrado");
        }
        if (code == null || code.length() != 6) {
            throw new BadRequestException("Código inválido");
        }
        users.put(email.toLowerCase(), new MockUser(
                user.sub, user.username, user.email, newPassword, user.name, user.phone, user.tenantId,
                user.businessName, user.role, user.confirmed));
        log.info("[MockAuth] Contraseña cambiada para: {}", email);
    }

    @Override
    public void resendConfirmationCode(String email) {
        log.info("[MockAuth] Código de confirmación reenviado a: {}", email);
    }

    @Override
    public void logout(String accessToken) {
        log.info("[MockAuth] Sesión cerrada para token: {}...",
                accessToken.substring(0, Math.min(20, accessToken.length())));
    }

    /** Estructura interna para representar un usuario mock */
    private record MockUser(String sub, String username, String email, String password, String name, String phone,
            String tenantId, String businessName, String role, boolean confirmed) {
    }
}
