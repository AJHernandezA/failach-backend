package com.projectx.backend.infra.adapters.out.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.projectx.backend.domain.models.*;
import com.projectx.backend.domain.ports.out.TenantRepository;
import com.projectx.backend.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Implementación del repositorio de tenants usando DynamoDB.
 * Patrón single-table: PK = TENANT#<tenantId>, SK = CONFIG.
 */
public class DynamoDbTenantRepository implements TenantRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbTenantRepository.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    @Inject
    public DynamoDbTenantRepository(DynamoDbClient dynamoDb, AppConfig appConfig) {
        this.dynamoDb = dynamoDb;
        this.tableName = appConfig.getDynamoDbTable();
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        log.debug("Buscando tenant en DynamoDB: {}", tenantId);

        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s("TENANT#" + tenantId).build(),
                        "SK", AttributeValue.builder().s("CONFIG").build()))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToTenant(response.item()));
    }

    @Override
    public void save(Tenant tenant) {
        log.debug("Guardando tenant en DynamoDB: {}", tenant.tenantId());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s("TENANT#" + tenant.tenantId()).build());
        item.put("SK", AttributeValue.builder().s("CONFIG").build());
        item.put("tenantId", AttributeValue.builder().s(tenant.tenantId()).build());
        item.put("name", AttributeValue.builder().s(tenant.name()).build());
        item.put("isActive", AttributeValue.builder().bool(tenant.isActive()).build());
        item.put("createdAt", AttributeValue.builder().s(tenant.createdAt().toString()).build());
        item.put("updatedAt", AttributeValue.builder().s(tenant.updatedAt().toString()).build());

        // Campos opcionales (solo agregar si no son null)
        putIfNotNull(item, "description", tenant.description());
        putIfNotNull(item, "logoUrl", tenant.logoUrl());
        putIfNotNull(item, "faviconUrl", tenant.faviconUrl());
        putIfNotNull(item, "bannerUrl", tenant.bannerUrl());
        putIfNotNull(item, "font", tenant.font());
        putIfNotNull(item, "whatsapp", tenant.whatsapp());
        putIfNotNull(item, "email", tenant.email());
        putIfNotNull(item, "phone", tenant.phone());
        putIfNotNull(item, "address", tenant.address());
        putIfNotNull(item, "schedule", tenant.schedule());

        // Colores como mapa
        if (tenant.colors() != null) {
            item.put("colors", AttributeValue.builder().m(Map.of(
                    "primary", AttributeValue.builder().s(nullSafe(tenant.colors().primary())).build(),
                    "secondary", AttributeValue.builder().s(nullSafe(tenant.colors().secondary())).build(),
                    "accent", AttributeValue.builder().s(nullSafe(tenant.colors().accent())).build(),
                    "background", AttributeValue.builder().s(nullSafe(tenant.colors().background())).build(),
                    "text", AttributeValue.builder().s(nullSafe(tenant.colors().text())).build())).build());
        }

        // Redes sociales como JSON string
        if (tenant.socialMedia() != null && !tenant.socialMedia().isEmpty()) {
            try {
                item.put("socialMedia", AttributeValue.builder()
                        .s(mapper.writeValueAsString(tenant.socialMedia())).build());
            } catch (Exception e) {
                log.warn("Error serializando socialMedia", e);
            }
        }

        // Ciudades como lista de strings
        if (tenant.cities() != null && !tenant.cities().isEmpty()) {
            item.put("cities", AttributeValue.builder()
                    .l(tenant.cities().stream()
                            .map(c -> AttributeValue.builder().s(c).build())
                            .toList())
                    .build());
        }

        // F020: thankYouMessage
        putIfNotNull(item, "thankYouMessage", tenant.thankYouMessage());

        // F023: analyticsId
        putIfNotNull(item, "analyticsId", tenant.analyticsId());

        // F031: shippingConfig como mapa
        if (tenant.shippingConfig() != null) {
            Map<String, AttributeValue> scMap = new HashMap<>();
            scMap.put("defaultShippingCost",
                    AttributeValue.builder().n(tenant.shippingConfig().defaultShippingCost().toPlainString()).build());
            if (tenant.shippingConfig().freeShippingThreshold() != null) {
                scMap.put("freeShippingThreshold", AttributeValue.builder()
                        .n(tenant.shippingConfig().freeShippingThreshold().toPlainString()).build());
            }
            scMap.put("pickupEnabled", AttributeValue.builder().bool(tenant.shippingConfig().pickupEnabled()).build());
            scMap.put("pickupDiscountEnabled",
                    AttributeValue.builder().bool(tenant.shippingConfig().pickupDiscountEnabled()).build());
            putIfNotNullToMap(scMap, "shippingLabel", tenant.shippingConfig().shippingLabel());
            item.put("shippingConfig", AttributeValue.builder().m(scMap).build());
        }

        // F031: manualPaymentDiscount como JSON string
        if (tenant.manualPaymentDiscount() != null) {
            try {
                item.put("manualPaymentDiscount", AttributeValue.builder()
                        .s(mapper.writeValueAsString(tenant.manualPaymentDiscount())).build());
            } catch (Exception e) {
                log.warn("Error serializando manualPaymentDiscount", e);
            }
        }

        // Info bancaria como mapa
        if (tenant.bankInfo() != null) {
            Map<String, AttributeValue> bankMap = new HashMap<>();
            putIfNotNullToMap(bankMap, "bankName", tenant.bankInfo().bankName());
            putIfNotNullToMap(bankMap, "accountType", tenant.bankInfo().accountType());
            putIfNotNullToMap(bankMap, "accountNumber", tenant.bankInfo().accountNumber());
            putIfNotNullToMap(bankMap, "accountHolder", tenant.bankInfo().accountHolder());
            putIfNotNullToMap(bankMap, "documentType", tenant.bankInfo().documentType());
            putIfNotNullToMap(bankMap, "documentNumber", tenant.bankInfo().documentNumber());
            if (!bankMap.isEmpty()) {
                item.put("bankInfo", AttributeValue.builder().m(bankMap).build());
            }
        }

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());

        log.info("Tenant guardado: {}", tenant.tenantId());
    }

    @Override
    public List<Tenant> findAll() {
        log.debug("Listando todos los tenants");

        ScanResponse response = dynamoDb.scan(ScanRequest.builder()
                .tableName(tableName)
                .filterExpression("SK = :sk")
                .expressionAttributeValues(Map.of(
                        ":sk", AttributeValue.builder().s("CONFIG").build()))
                .build());

        return response.items().stream()
                .map(this::mapToTenant)
                .toList();
    }

    /**
     * Mapea un item de DynamoDB a la entidad Tenant del dominio.
     */
    private Tenant mapToTenant(Map<String, AttributeValue> item) {
        // Mapear colores
        TenantColors colors = null;
        if (item.containsKey("colors") && item.get("colors").m() != null) {
            Map<String, AttributeValue> colorsMap = item.get("colors").m();
            colors = new TenantColors(
                    getStr(colorsMap, "primary"),
                    getStr(colorsMap, "secondary"),
                    getStr(colorsMap, "accent"),
                    getStr(colorsMap, "background"),
                    getStr(colorsMap, "text"));
        }

        // Mapear redes sociales
        List<SocialLink> socialMedia = List.of();
        if (item.containsKey("socialMedia") && item.get("socialMedia").s() != null) {
            try {
                socialMedia = mapper.readValue(
                        item.get("socialMedia").s(),
                        new TypeReference<List<SocialLink>>() {
                        });
            } catch (Exception e) {
                log.warn("Error deserializando socialMedia", e);
            }
        }

        // Mapear ciudades
        List<String> cities = List.of();
        if (item.containsKey("cities") && item.get("cities").l() != null) {
            cities = item.get("cities").l().stream()
                    .map(AttributeValue::s)
                    .toList();
        }

        // Mapear info bancaria
        BankInfo bankInfo = null;
        if (item.containsKey("bankInfo") && item.get("bankInfo").m() != null) {
            Map<String, AttributeValue> bankMap = item.get("bankInfo").m();
            bankInfo = new BankInfo(
                    getStr(bankMap, "bankName"),
                    getStr(bankMap, "accountType"),
                    getStr(bankMap, "accountNumber"),
                    getStr(bankMap, "accountHolder"),
                    getStr(bankMap, "documentType"),
                    getStr(bankMap, "documentNumber"));
        }

        // F031: Mapear shippingConfig
        ShippingConfig shippingConfig = null;
        if (item.containsKey("shippingConfig") && item.get("shippingConfig").m() != null) {
            Map<String, AttributeValue> scMap = item.get("shippingConfig").m();
            shippingConfig = new ShippingConfig(
                    scMap.containsKey("defaultShippingCost") ? new BigDecimal(scMap.get("defaultShippingCost").n())
                            : BigDecimal.ZERO,
                    scMap.containsKey("freeShippingThreshold") ? new BigDecimal(scMap.get("freeShippingThreshold").n())
                            : null,
                    scMap.containsKey("pickupEnabled") && Boolean.TRUE.equals(scMap.get("pickupEnabled").bool()),
                    scMap.containsKey("pickupDiscountEnabled")
                            && Boolean.TRUE.equals(scMap.get("pickupDiscountEnabled").bool()),
                    scMap.containsKey("shippingLabel") ? scMap.get("shippingLabel").s() : "Envío");
        }

        // F031: Mapear manualPaymentDiscount
        ManualPaymentDiscountConfig manualPaymentDiscount = null;
        if (item.containsKey("manualPaymentDiscount") && item.get("manualPaymentDiscount").s() != null) {
            try {
                manualPaymentDiscount = mapper.readValue(
                        item.get("manualPaymentDiscount").s(), ManualPaymentDiscountConfig.class);
            } catch (Exception e) {
                log.warn("Error deserializando manualPaymentDiscount", e);
            }
        }

        return new Tenant(
                getStr(item, "tenantId"),
                getStr(item, "name"),
                getStr(item, "description"),
                getStr(item, "logoUrl"),
                getStrOrNull(item, "faviconUrl"),
                getStr(item, "bannerUrl"),
                colors,
                getStr(item, "font"),
                socialMedia,
                cities,
                getStr(item, "whatsapp"),
                getStr(item, "email"),
                getStr(item, "phone"),
                getStr(item, "address"),
                getStr(item, "schedule"),
                bankInfo,
                item.containsKey("isActive") && item.get("isActive").bool() != null
                        ? item.get("isActive").bool()
                        : true,
                getStrOrNull(item, "thankYouMessage"),
                getStrOrNull(item, "analyticsId"),
                shippingConfig,
                manualPaymentDiscount,
                item.containsKey("createdAt")
                        ? Instant.parse(item.get("createdAt").s())
                        : Instant.now(),
                item.containsKey("updatedAt")
                        ? Instant.parse(item.get("updatedAt").s())
                        : Instant.now());
    }

    // --- Helpers ---

    private String getStr(Map<String, AttributeValue> map, String key) {
        AttributeValue val = map.get(key);
        return (val != null && val.s() != null) ? val.s() : "";
    }

    private String getStrOrNull(Map<String, AttributeValue> map, String key) {
        AttributeValue val = map.get(key);
        return (val != null && val.s() != null && !val.s().isEmpty()) ? val.s() : null;
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private void putIfNotNull(Map<String, AttributeValue> item, String key, String value) {
        if (value != null && !value.isBlank()) {
            item.put(key, AttributeValue.builder().s(value).build());
        }
    }

    private void putIfNotNullToMap(Map<String, AttributeValue> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, AttributeValue.builder().s(value).build());
        }
    }
}
