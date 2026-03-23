package com.projectx.backend.infra.adapters.out.persistence;

import com.google.inject.Inject;
import com.projectx.backend.domain.models.LegalContent;
import com.projectx.backend.domain.ports.out.LegalContentRepository;
import com.projectx.backend.infra.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación del repositorio de contenido legal usando DynamoDB.
 * Patrón single-table: PK = TENANT#<tenantId>, SK = LEGAL#<type>.
 */
public class DynamoDbLegalContentRepository implements LegalContentRepository {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbLegalContentRepository.class);

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    @Inject
    public DynamoDbLegalContentRepository(DynamoDbClient dynamoDb, AppConfig appConfig) {
        this.dynamoDb = dynamoDb;
        this.tableName = appConfig.getDynamoDbTable();
    }

    @Override
    public Optional<LegalContent> findByType(String tenantId, String type) {
        log.debug("Buscando contenido legal: tenant={}, type={}", tenantId, type);

        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(
                        "PK", AttributeValue.builder().s("TENANT#" + tenantId).build(),
                        "SK", AttributeValue.builder().s("LEGAL#" + type).build()
                ))
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapFromDynamo(response.item(), tenantId, type));
    }

    @Override
    public void save(LegalContent content) {
        log.debug("Guardando contenido legal: tenant={}, type={}", content.tenantId(), content.type());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("PK", AttributeValue.builder().s("TENANT#" + content.tenantId()).build());
        item.put("SK", AttributeValue.builder().s("LEGAL#" + content.type()).build());
        item.put("title", AttributeValue.builder().s(content.title()).build());
        item.put("content", AttributeValue.builder().s(content.content()).build());
        item.put("updatedAt", AttributeValue.builder().s(content.updatedAt().toString()).build());

        dynamoDb.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build());
    }

    /**
     * Mapea un item de DynamoDB a LegalContent.
     */
    private LegalContent mapFromDynamo(Map<String, AttributeValue> item, String tenantId, String type) {
        return new LegalContent(
                tenantId,
                type,
                getStr(item, "title"),
                getStr(item, "content"),
                item.containsKey("updatedAt") ? Instant.parse(item.get("updatedAt").s()) : Instant.now()
        );
    }

    private String getStr(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) ? item.get(key).s() : "";
    }
}
