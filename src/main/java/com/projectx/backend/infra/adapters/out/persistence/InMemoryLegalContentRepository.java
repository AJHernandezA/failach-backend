package com.projectx.backend.infra.adapters.out.persistence;

import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.ports.out.LegalContentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación in-memory del repositorio de contenido legal.
 * Para desarrollo local sin DynamoDB.
 * Precarga contenido legal por defecto para cada tipo.
 */
public class InMemoryLegalContentRepository implements LegalContentRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryLegalContentRepository.class);

    /** Almacenamiento: clave = "tenantId#type" */
    private final Map<String, LegalContent> store = new ConcurrentHashMap<>();

    public InMemoryLegalContentRepository() {
        log.info("InMemoryLegalContentRepository inicializado (desarrollo)");
    }

    @Override
    public Optional<LegalContent> findByType(String tenantId, String type) {
        String key = tenantId + "#" + type;
        LegalContent content = store.get(key);
        if (content != null) {
            return Optional.of(content);
        }
        // Retornar contenido por defecto si no existe
        return Optional.of(defaultContent(tenantId, type));
    }

    @Override
    public void save(LegalContent content) {
        String key = content.tenantId() + "#" + content.type();
        store.put(key, content);
        log.debug("Contenido legal guardado: {}", key);
    }

    /**
     * Genera contenido legal por defecto para desarrollo.
     */
    private LegalContent defaultContent(String tenantId, String type) {
        String title;
        String body;
        switch (type) {
            case "terms" -> {
                title = "Términos y Condiciones";
                body = "# Términos y Condiciones\n\nEstos son los términos y condiciones de ejemplo para **" + tenantId + "**.\n\n## 1. Aceptación\nAl utilizar este sitio, aceptas estos términos.\n\n## 2. Uso del servicio\nEl servicio se proporciona \"tal cual\".\n\n## 3. Pagos\nLos pagos se procesan de forma segura.\n\n## 4. Envíos\nLos tiempos de entrega varían según la ciudad.\n\n## 5. Contacto\nPara consultas, usa la información de contacto en el sitio.";
            }
            case "privacy" -> {
                title = "Política de Privacidad";
                body = "# Política de Privacidad\n\nEsta es la política de privacidad de ejemplo para **" + tenantId + "**.\n\n## Datos que recopilamos\n- Nombre y email al realizar pedidos\n- Dirección de envío\n- Teléfono de contacto\n\n## Uso de los datos\nTus datos se usan exclusivamente para procesar pedidos y comunicaciones relacionadas.\n\n## Protección\nTus datos se almacenan de forma segura.";
            }
            case "returns" -> {
                title = "Política de Devoluciones";
                body = "# Política de Devoluciones\n\nEsta es la política de devoluciones de ejemplo para **" + tenantId + "**.\n\n## Plazo\nTienes 30 días para solicitar una devolución.\n\n## Condiciones\n- El producto debe estar en su empaque original\n- No debe tener señales de uso\n\n## Proceso\n1. Contacta por WhatsApp o email\n2. Envía el producto\n3. Reembolso en 5 días hábiles";
            }
            default -> {
                title = type;
                body = "Contenido no disponible para el tipo: " + type;
            }
        }
        return new LegalContent(tenantId, type, title, body, Instant.now());
    }
}
