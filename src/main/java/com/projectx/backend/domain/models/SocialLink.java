package com.projectx.backend.domain.models;

/**
 * Enlace a red social de un tenant.
 *
 * @param platform nombre de la plataforma (instagram, facebook, tiktok, etc.)
 * @param url      URL completa del perfil
 * @param isActive si el enlace está visible en la tienda
 */
public record SocialLink(
        String platform,
        String url,
        boolean isActive
) {}
