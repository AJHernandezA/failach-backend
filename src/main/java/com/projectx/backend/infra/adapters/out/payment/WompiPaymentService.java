package com.projectx.backend.infra.adapters.out.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.projectx.backend.domain.models.Order;
import com.projectx.backend.domain.models.PaymentInitData;
import com.projectx.backend.domain.models.PaymentLink;
import com.projectx.backend.domain.ports.out.PaymentService;
import com.projectx.backend.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de pagos con Wompi.
 * Usa las llaves configuradas en application.properties / variables de entorno.
 */
public class WompiPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(WompiPaymentService.class);
    private static final String CHECKOUT_BASE = "https://checkout.wompi.co/l/";

    private final String publicKey;
    private final String integritySecret;
    private final String eventsSecret;
    private final String privateKey;
    private final String apiBaseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public WompiPaymentService(AppConfig appConfig) {
        this.publicKey = appConfig.getWompiPublicKey();
        this.integritySecret = appConfig.getWompiIntegritySecret();
        this.eventsSecret = appConfig.getWompiEventsSecret();
        this.privateKey = appConfig.getWompiPrivateKey();
        this.apiBaseUrl = appConfig.getWompiApiBaseUrl();
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        log.info("WompiPaymentService inicializado con publicKey={}",
                publicKey.substring(0, Math.min(12, publicKey.length())) + "...");
    }

    @Override
    public PaymentInitData initiate(String tenantId, Order order, String redirectUrl) {
        String reference = "PX-" + tenantId + "-" + order.orderCode();
        long amountInCents = order.total().longValue() * 100;
        String currency = "COP";

        String signature = calculateIntegritySignature(reference, amountInCents, currency);

        log.info("Wompi payment iniciado: ref={}, monto={} centavos", reference, amountInCents);

        return new PaymentInitData(
                reference,
                amountInCents,
                currency,
                signature,
                publicKey,
                redirectUrl);
    }

    @Override
    public boolean verifyWebhookSignature(String transactionId, String status, long amountInCents,
            String receivedSignature) {
        // Firma del webhook: SHA256(transactionId + status + amountInCents +
        // events_secret)
        String toSign = transactionId + status + amountInCents + eventsSecret;
        String expected = sha256(toSign);
        boolean valid = expected.equals(receivedSignature);
        if (!valid) {
            log.warn("Firma de webhook inválida. Esperado: {}, Recibido: {}", expected, receivedSignature);
        }
        return valid;
    }

    @Override
    public String calculateIntegritySignature(String reference, long amountInCents, String currency) {
        // SHA256(referencia + monto_centavos + moneda + integrity_secret)
        String toSign = reference + amountInCents + currency + integritySecret;
        return sha256(toSign);
    }

    @Override
    public PaymentLink createPaymentLink(String name, String description, boolean singleUse,
            Long amountInCents, String imageUrl, String redirectUrl) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", name);
            body.put("description", description);
            body.put("single_use", singleUse);
            body.put("collect_shipping", false);
            body.put("currency", "COP");

            if (amountInCents != null && amountInCents > 0) {
                body.put("amount_in_cents", amountInCents);
            }
            if (imageUrl != null && !imageUrl.isBlank()) {
                body.put("image_url", imageUrl);
            }
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                body.put("redirect_url", redirectUrl);
            }

            String json = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/payment_links"))
                    .header("Authorization", "Bearer " + privateKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Error creando link de pago en Wompi: status={}, body={}", response.statusCode(),
                        response.body());
                throw new RuntimeException("Error al crear link de pago: " + response.body());
            }

            JsonNode data = objectMapper.readTree(response.body()).get("data");
            return parsePaymentLink(data);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error de comunicación con Wompi al crear link de pago", e);
            throw new RuntimeException("Error de comunicación con Wompi", e);
        }
    }

    @Override
    public List<PaymentLink> listPaymentLinks() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/payment_links"))
                    .header("Authorization", "Bearer " + privateKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Error listando links de pago: status={}", response.statusCode());
                return List.of();
            }

            JsonNode dataArray = objectMapper.readTree(response.body()).get("data");
            List<PaymentLink> links = new ArrayList<>();
            if (dataArray != null && dataArray.isArray()) {
                for (JsonNode node : dataArray) {
                    links.add(parsePaymentLink(node));
                }
            }
            return links;

        } catch (Exception e) {
            log.error("Error de comunicación con Wompi al listar links de pago", e);
            return List.of();
        }
    }

    @Override
    public PaymentLink getPaymentLink(String linkId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/payment_links/" + linkId))
                    .header("Authorization", "Bearer " + privateKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.error("Error obteniendo link de pago {}: status={}", linkId, response.statusCode());
                return null;
            }

            JsonNode data = objectMapper.readTree(response.body()).get("data");
            return parsePaymentLink(data);

        } catch (Exception e) {
            log.error("Error de comunicación con Wompi al obtener link de pago {}", linkId, e);
            return null;
        }
    }

    /**
     * Parsea un nodo JSON de Wompi a PaymentLink.
     */
    private PaymentLink parsePaymentLink(JsonNode node) {
        String id = node.has("id") ? node.get("id").asText() : "";
        String linkName = node.has("name") ? node.get("name").asText() : "";
        String desc = node.has("description") ? node.get("description").asText() : "";
        boolean single = node.has("single_use") && node.get("single_use").asBoolean();
        Long amount = node.has("amount_in_cents") && !node.get("amount_in_cents").isNull()
                ? node.get("amount_in_cents").asLong()
                : null;
        String currency = node.has("currency") ? node.get("currency").asText() : "COP";
        String imgUrl = node.has("image_url") && !node.get("image_url").isNull()
                ? node.get("image_url").asText()
                : null;
        boolean active = !node.has("active") || node.get("active").asBoolean(true);

        return new PaymentLink(id, linkName, desc, single, amount, currency, imgUrl, CHECKOUT_BASE + id, active);
    }

    /**
     * Calcula SHA256 hex de un string.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando SHA256", e);
        }
    }
}
