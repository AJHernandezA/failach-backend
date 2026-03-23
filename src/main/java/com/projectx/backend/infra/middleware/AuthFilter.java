package com.projectx.backend.infra.middleware;

import com.projectx.backend.domain.models.AuthenticatedUser;
import com.projectx.backend.domain.models.UserRole;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Filtro de autenticación que valida JWT de AWS Cognito.
 * Descarga y cachea las public keys (JWKS) para verificar la firma del token.
 * Extrae claims del JWT y crea un AuthenticatedUser en el contexto.
 */
public class AuthFilter implements Handler {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String userPoolId;
    private final String region;
    private final String issuer;
    private final String jwksUrl;

    /** Cache de public keys por kid (key ID) */
    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private long lastKeyFetch = 0;
    private static final long KEY_CACHE_TTL_MS = 3600_000; // 1 hora

    public AuthFilter(String region, String userPoolId) {
        this.region = region;
        this.userPoolId = userPoolId;
        this.issuer = "https://cognito-idp." + region + ".amazonaws.com/" + userPoolId;
        this.jwksUrl = issuer + "/.well-known/jwks.json";
    }

    @Override
    public void handle(Context ctx) throws Exception {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Token de autenticación requerido");
        }

        String token = authHeader.substring(7);

        try {
            // Parsear el header del JWT para obtener el kid
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new UnauthorizedResponse("Token inválido");
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            @SuppressWarnings("unchecked")
            Map<String, Object> header = mapper.readValue(headerJson, Map.class);
            String kid = (String) header.get("kid");

            // Obtener la public key para este kid
            PublicKey publicKey = getPublicKey(kid);
            if (publicKey == null) {
                throw new UnauthorizedResponse("No se encontró la llave pública para verificar el token");
            }

            // Validar y parsear el JWT
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Extraer datos del usuario
            String sub = claims.getSubject();
            String email = claims.get("email", String.class);
            String roleStr = claims.get("custom:role", String.class);
            String tenantId = claims.get("custom:tenantId", String.class);

            UserRole role;
            try {
                role = UserRole.valueOf(roleStr != null ? roleStr.toUpperCase() : "BUYER");
            } catch (IllegalArgumentException e) {
                role = UserRole.BUYER;
            }

            AuthenticatedUser user = new AuthenticatedUser(sub, email, role, tenantId);
            ctx.attribute("user", user);

            log.debug("Usuario autenticado: {} ({}), rol: {}, tenant: {}",
                    email, sub, role, tenantId);

        } catch (ExpiredJwtException e) {
            throw new UnauthorizedResponse("Token expirado");
        } catch (SignatureException e) {
            throw new UnauthorizedResponse("Firma de token inválida");
        } catch (JwtException e) {
            throw new UnauthorizedResponse("Token inválido: " + e.getMessage());
        } catch (UnauthorizedResponse e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando JWT", e);
            throw new UnauthorizedResponse("Error de autenticación");
        }
    }

    /**
     * Obtiene la public key para un kid dado, con cache.
     */
    @SuppressWarnings("unchecked")
    private PublicKey getPublicKey(String kid) throws Exception {
        // Verificar cache
        if (keyCache.containsKey(kid) && (System.currentTimeMillis() - lastKeyFetch) < KEY_CACHE_TTL_MS) {
            return keyCache.get(kid);
        }

        // Descargar JWKS
        log.info("Descargando JWKS desde: {}", jwksUrl);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwksUrl))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> jwks = mapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

        keyCache.clear();
        for (Map<String, Object> key : keys) {
            String keyKid = (String) key.get("kid");
            String n = (String) key.get("n");
            String e = (String) key.get("e");

            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, nBytes),
                    new BigInteger(1, eBytes)
            );
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            keyCache.put(keyKid, publicKey);
        }

        lastKeyFetch = System.currentTimeMillis();
        return keyCache.get(kid);
    }
}
