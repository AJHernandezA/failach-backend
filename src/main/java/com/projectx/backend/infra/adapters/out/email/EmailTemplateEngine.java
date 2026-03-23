package com.projectx.backend.infra.adapters.out.email;

import com.projectx.backend.domain.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Motor de templates de email.
 * Carga templates HTML desde resources y reemplaza placeholders.
 */
public class EmailTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateEngine.class);
    private static final String TEMPLATES_PATH = "templates/email/";

    /**
     * Carga un template HTML desde resources y reemplaza los placeholders.
     */
    public String render(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return template;
    }

    /**
     * Construye las variables comunes para todos los emails de un tenant y orden.
     */
    public Map<String, String> buildOrderVariables(Tenant tenant, Order order) {
        String primaryColor = (tenant.colors() != null && tenant.colors().primary() != null)
                ? tenant.colors().primary()
                : "#1a1a1a";
        String logoUrl = tenant.logoUrl() != null ? tenant.logoUrl() : "";
        String whatsappUrl = tenant.whatsapp() != null
                ? "https://wa.me/" + tenant.whatsapp().replace("+", "")
                : "";

        return new java.util.HashMap<>(Map.of(
                "tenantName", safe(tenant.name()),
                "tenantLogo", logoUrl,
                "primaryColor", primaryColor,
                "orderCode", safe(order.orderCode()),
                "customerName", safe(order.customer().fullName()),
                "total", formatPrice(order.total()),
                "subtotal", formatPrice(order.subtotal()),
                "shippingCost", formatPrice(order.shippingCost()),
                "trackingUrl", "https://" + tenant.tenantId() + ".projectx.com/order/" + order.orderCode(),
                "whatsappUrl", whatsappUrl));
    }

    /**
     * Genera el HTML de los items de la orden para insertar en el template.
     */
    public String renderItems(Order order) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.items()) {
            sb.append("<tr>");
            sb.append("<td style=\"padding:8px 0;border-bottom:1px solid #eee;\">");
            sb.append(safe(item.productName()));
            if (item.variantName() != null && !item.variantName().isBlank()) {
                sb.append(" <span style=\"color:#888;font-size:12px;\">(").append(safe(item.variantName()))
                        .append(")</span>");
            }
            sb.append("</td>");
            sb.append("<td style=\"padding:8px 0;border-bottom:1px solid #eee;text-align:center;\">")
                    .append(item.quantity()).append("</td>");
            sb.append("<td style=\"padding:8px 0;border-bottom:1px solid #eee;text-align:right;\">")
                    .append(formatPrice(item.price().multiply(BigDecimal.valueOf(item.quantity())))).append("</td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    /**
     * Formatea un precio en formato colombiano.
     */
    public static String formatPrice(BigDecimal price) {
        if (price == null)
            return "$0";
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));
        nf.setMaximumFractionDigits(0);
        return nf.format(price);
    }

    /**
     * Carga un template HTML desde el classpath.
     */
    private String loadTemplate(String templateName) {
        String path = TEMPLATES_PATH + templateName;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                log.error("Template no encontrado: {}", path);
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error cargando template: {}", path, e);
            return "";
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }
}
