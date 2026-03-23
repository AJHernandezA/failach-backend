package com.projectx.backend.infra.adapters.out.auth;

import com.projectx.backend.domain.exceptions.BadRequestException;
import com.projectx.backend.domain.exceptions.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para el servicio de autenticación mock.
 */
class MockAuthServiceTest {

    private MockAuthService authService;

    @BeforeEach
    void setUp() {
        authService = new MockAuthService();
    }

    @Test
    void register_nuevoUsuario_retornaSub() {
        String sub = authService.register("nuevo@test.com", "Password123!", "Test User", "+573001234567", "mi-tienda",
                "MERCHANT");
        assertNotNull(sub);
        assertFalse(sub.isBlank());
    }

    @Test
    void register_emailDuplicado_lanzaExcepcion() {
        assertThrows(BadRequestException.class, () -> authService.register("adolfohernandezaponte@hotmail.com",
                "Password123!", "Otro", "+573001234567", "otra", "MERCHANT"));
    }

    @Test
    void confirmEmail_codigoValido_noLanzaExcepcion() {
        authService.register("test@confirm.com", "Pass123!", "Test", "+573001234567", "test-tenant", "MERCHANT");
        assertDoesNotThrow(() -> authService.confirmEmail("test@confirm.com", "123456"));
    }

    @Test
    void confirmEmail_codigoInvalido_lanzaExcepcion() {
        authService.register("test2@confirm.com", "Pass123!", "Test", "+573001234567", "test-tenant", "MERCHANT");
        assertThrows(BadRequestException.class, () -> authService.confirmEmail("test2@confirm.com", "123"));
    }

    @Test
    void login_credencialesCorrectas_retornaTokens() {
        // Los usuarios pre-creados ya están confirmados
        Map<String, Object> result = authService.login("admin", "12345adolfo");

        assertNotNull(result.get("accessToken"));
        assertNotNull(result.get("idToken"));
        assertNotNull(result.get("refreshToken"));
        assertEquals(3600, result.get("expiresIn"));
        assertNotNull(result.get("user"));
    }

    @Test
    void login_credencialesInvalidas_lanzaExcepcion() {
        assertThrows(UnauthorizedException.class, () -> authService.login("admin", "wrongpassword"));
    }

    @Test
    void login_emailNoConfirmado_lanzaExcepcion() {
        authService.register("noconfirm@test.com", "Pass123!", "Test", "+573001234567", "test", "MERCHANT");
        assertThrows(BadRequestException.class, () -> authService.login("noconfirm@test.com", "Pass123!"));
    }

    @Test
    void login_despuesDeConfirmar_funciona() {
        authService.register("confirm-then-login@test.com", "Pass123!", "Test", "+573001234567", "test", "MERCHANT");
        authService.confirmEmail("confirm-then-login@test.com", "123456");
        Map<String, Object> result = authService.login("confirm-then-login@test.com", "Pass123!");
        assertNotNull(result.get("accessToken"));
    }

    @Test
    void resetPassword_funciona() {
        authService.forgotPassword("adolfohernandezaponte@hotmail.com");
        assertDoesNotThrow(
                () -> authService.resetPassword("adolfohernandezaponte@hotmail.com", "654321", "NewPass123!"));

        // Verificar que el login funciona con la nueva contraseña
        Map<String, Object> result = authService.login("adolfohernandezaponte@hotmail.com", "NewPass123!");
        assertNotNull(result.get("accessToken"));
    }

    @Test
    void refreshToken_retornaNuevoToken() {
        Map<String, Object> result = authService.refreshToken("mock-refresh-token");
        assertNotNull(result.get("accessToken"));
        assertEquals(3600, result.get("expiresIn"));
    }

    @Test
    void logout_noLanzaExcepcion() {
        assertDoesNotThrow(() -> authService.logout("mock-access-token-123"));
    }
}
