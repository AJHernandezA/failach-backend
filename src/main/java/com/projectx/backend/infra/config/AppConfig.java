package com.projectx.backend.infra.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Configuración de la aplicación.
 * Lee valores de application.properties y permite sobrescribirlos con variables
 * de entorno.
 * Patrón: primero busca variable de entorno, si no existe usa el valor de
 * properties.
 */
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private final Properties properties;

    public AppConfig() {
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Carga el archivo application.properties desde el classpath.
     */
    private void loadProperties() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
                log.info("Configuración cargada desde application.properties");
            } else {
                log.warn("No se encontró application.properties, usando valores por defecto");
            }
        } catch (IOException e) {
            log.error("Error al cargar application.properties", e);
        }
    }

    /**
     * Obtiene un valor de configuración.
     * Prioridad: variable de entorno > application.properties > defaultValue.
     *
     * @param key          clave de la propiedad
     * @param defaultValue valor por defecto si no se encuentra
     * @return el valor de configuración
     */
    public String get(String key, String defaultValue) {
        // Las variables de entorno usan UPPER_SNAKE_CASE: server.port → SERVER_PORT
        String envKey = key.toUpperCase().replace('.', '_').replace('-', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    /** Puerto del servidor HTTP */
    public int getServerPort() {
        return Integer.parseInt(get("server.port", "7070"));
    }

    /** Host del servidor */
    public String getServerHost() {
        return get("server.host", "0.0.0.0");
    }

    /** Orígenes permitidos para CORS (separados por coma) */
    public List<String> getCorsAllowedOrigins() {
        String origins = get("cors.allowedOrigins", "http://localhost:3000");
        return Arrays.asList(origins.split(","));
    }

    /** Región de AWS */
    public String getAwsRegion() {
        return get("aws.region", "us-east-1");
    }

    /** Nombre de la tabla DynamoDB */
    public String getDynamoDbTable() {
        return get("aws.dynamodb.table", "ProjectX");
    }

    /** Endpoint local de DynamoDB (para desarrollo) */
    public String getDynamoDbEndpoint() {
        return get("aws.dynamodb.endpoint", "");
    }

    /** Cognito User Pool ID */
    public String getCognitoUserPoolId() {
        return get("cognito.userPoolId", "");
    }

    /** Cognito Client ID */
    public String getCognitoClientId() {
        return get("cognito.clientId", "");
    }

    /** Si el mock de auth está habilitado (desarrollo) */
    public boolean isAuthMockEnabled() {
        return Boolean.parseBoolean(get("auth.mock", "false"));
    }

    /** Email remitente para SES */
    public String getSesFromEmail() {
        return get("ses.fromEmail", "noreply@projectx.com");
    }

    /** Si el envío de emails está habilitado (requiere SES configurado) */
    public boolean isEmailEnabled() {
        return Boolean.parseBoolean(get("email.enabled", "false"));
    }

    /** Llave pública de Wompi */
    public String getWompiPublicKey() {
        return get("wompi.publicKey", "");
    }

    /** Secreto de integridad de Wompi */
    public String getWompiIntegritySecret() {
        return get("wompi.integritySecret", "");
    }

    /** Secreto de eventos (webhooks) de Wompi */
    public String getWompiEventsSecret() {
        return get("wompi.eventsSecret", "");
    }

    /** Llave privada de Wompi (para crear links de pago) */
    public String getWompiPrivateKey() {
        return get("wompi.privateKey", "");
    }

    /** URL base del API de Wompi */
    public String getWompiApiBaseUrl() {
        return get("wompi.apiBaseUrl", "https://sandbox.wompi.co/v1");
    }

    /**
     * Valida que todas las variables de entorno requeridas para producción estén
     * configuradas.
     * Lanza IllegalStateException si falta alguna (fail-fast como Tiquetera).
     * Solo aplica cuando auth.mock=false (producción).
     */
    public void validateProductionSecrets() {
        if (isAuthMockEnabled()) {
            log.info("[CONFIG] Modo desarrollo (auth.mock=true). No se validan secretos de producción.");
            return;
        }

        log.info("[CONFIG] Modo producción. Validando variables de entorno requeridas...");

        requireEnv("WOMPI_PUBLICKEY", getWompiPublicKey(), "Llave pública de Wompi");
        requireEnv("WOMPI_PRIVATEKEY", getWompiPrivateKey(), "Llave privada de Wompi");
        requireEnv("WOMPI_INTEGRITYSECRET", getWompiIntegritySecret(), "Secreto de integridad de Wompi");
        requireEnv("WOMPI_EVENTSSECRET", getWompiEventsSecret(), "Secreto de eventos/webhooks de Wompi");
        requireEnv("COGNITO_USERPOOLID", getCognitoUserPoolId(), "Cognito User Pool ID");

        log.info("[CONFIG] Todas las variables de entorno requeridas están configuradas.");
    }

    /**
     * Valida que un valor de configuración no esté vacío.
     * Lanza IllegalStateException si falta (patrón fail-fast de Tiquetera).
     */
    private void requireEnv(String envName, String value, String description) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    String.format("Variable de entorno %s (%s) no está configurada. "
                            + "Configúrala en las variables de entorno del servidor.", envName, description));
        }
    }
}
