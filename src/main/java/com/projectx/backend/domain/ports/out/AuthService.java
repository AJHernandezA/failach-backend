package com.projectx.backend.domain.ports.out;

import java.util.Map;

/**
 * Puerto de salida para el servicio de autenticación.
 * La implementación concreta usa AWS Cognito o mock en desarrollo.
 */
public interface AuthService {

    /**
     * Registra un nuevo usuario en el sistema de autenticación.
     *
     * @param email email del usuario (será el username)
     * @param password contraseña
     * @param name nombre completo
     * @param phone teléfono
     * @param tenantId ID del tenant asignado
     * @param role rol del usuario (MERCHANT, SUPER_ADMIN)
     * @return sub (ID único del usuario en Cognito)
     */
    String register(String email, String password, String name, String phone, String tenantId, String role);

    /**
     * Confirma el email del usuario con el código de verificación.
     *
     * @param email email del usuario
     * @param code código de verificación de 6 dígitos
     */
    void confirmEmail(String email, String code);

    /**
     * Inicia sesión y retorna los tokens JWT.
     *
     * @param email email del usuario
     * @param password contraseña
     * @return mapa con accessToken, idToken, refreshToken, expiresIn
     */
    Map<String, Object> login(String email, String password);

    /**
     * Refresca el access token usando el refresh token.
     *
     * @param refreshToken token de refresco
     * @return mapa con nuevo accessToken y expiresIn
     */
    Map<String, Object> refreshToken(String refreshToken);

    /**
     * Solicita código de recuperación de contraseña.
     *
     * @param email email del usuario
     */
    void forgotPassword(String email);

    /**
     * Confirma el cambio de contraseña con código de verificación.
     *
     * @param email email del usuario
     * @param code código de verificación
     * @param newPassword nueva contraseña
     */
    void resetPassword(String email, String code, String newPassword);

    /**
     * Reenvía el código de confirmación de email.
     *
     * @param email email del usuario
     */
    void resendConfirmationCode(String email);

    /**
     * Cierra sesión global del usuario (invalida todos los tokens).
     *
     * @param accessToken token de acceso actual
     */
    void logout(String accessToken);
}
