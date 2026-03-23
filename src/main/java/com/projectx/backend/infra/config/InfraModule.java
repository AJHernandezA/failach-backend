package com.projectx.backend.infra.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.projectx.backend.domain.ports.out.CartRepository;
import com.projectx.backend.domain.ports.out.CategoryRepository;
import com.projectx.backend.domain.ports.out.EmailService;
import com.projectx.backend.domain.ports.out.LegalContentRepository;
import com.projectx.backend.domain.ports.out.OrderRepository;
import com.projectx.backend.domain.ports.out.PaymentService;
import com.projectx.backend.domain.ports.out.ProductRepository;
import com.projectx.backend.domain.ports.out.TenantRepository;
import com.projectx.backend.infra.adapters.out.email.LogEmailService;
import com.projectx.backend.infra.adapters.out.email.SesEmailService;
import com.projectx.backend.infra.adapters.out.payment.WompiPaymentService;
import com.projectx.backend.infra.adapters.out.persistence.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.ses.SesClient;

import java.net.URI;

/**
 * Módulo Guice para binding de infraestructura.
 * Aquí se registran los clientes de AWS, repositorios y servicios externos.
 * Se irán agregando bindings a medida que se implementen las features.
 */
public class InfraModule extends AbstractModule {

    private final AppConfig appConfig;

    public InfraModule(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    protected void configure() {
        // Registrar AppConfig como singleton
        bind(AppConfig.class).toInstance(appConfig);

        // F002: Tenant Repository
        // En desarrollo sin DynamoDB Local, usar InMemory. En producción, usar
        // DynamoDB.
        String dynamoEndpoint = appConfig.getDynamoDbEndpoint();
        if (dynamoEndpoint != null && !dynamoEndpoint.isBlank()) {
            bind(TenantRepository.class).to(DynamoDbTenantRepository.class).in(Singleton.class);
        } else {
            bind(TenantRepository.class).to(InMemoryTenantRepository.class).in(Singleton.class);
        }

        // F003: Product & Category Repositories
        bind(ProductRepository.class).to(InMemoryProductRepository.class).in(Singleton.class);
        bind(CategoryRepository.class).to(InMemoryCategoryRepository.class).in(Singleton.class);

        // F004: Cart Repository
        bind(CartRepository.class).to(InMemoryCartRepository.class).in(Singleton.class);

        // F005: Order Repository
        bind(OrderRepository.class).to(InMemoryOrderRepository.class).in(Singleton.class);

        // F006: Wompi Payment Service
        bind(PaymentService.class).to(WompiPaymentService.class).in(Singleton.class);

        // F009: Email Service — LogEmailService en desarrollo, SesEmailService con SES
        // habilitado
        if (appConfig.isEmailEnabled()) {
            bind(EmailService.class).to(SesEmailService.class).in(Singleton.class);
        } else {
            bind(EmailService.class).to(LogEmailService.class).in(Singleton.class);
        }

        // F018: Legal Content Repository — InMemory en desarrollo, DynamoDB en
        // producción
        String dynamoEndpointForLegal = appConfig.getDynamoDbEndpoint();
        if (dynamoEndpointForLegal != null && !dynamoEndpointForLegal.isBlank()) {
            bind(LegalContentRepository.class).to(DynamoDbLegalContentRepository.class).in(Singleton.class);
        } else {
            bind(LegalContentRepository.class).to(InMemoryLegalContentRepository.class).in(Singleton.class);
        }

        // F032: Platform Repository
        bind(com.projectx.backend.domain.ports.out.PlatformRepository.class)
                .to(InMemoryPlatformRepository.class).in(Singleton.class);

        // F033: Auth Service — MockAuthService en desarrollo, CognitoAuthService en
        // producción
        if (appConfig.isAuthMockEnabled()) {
            bind(com.projectx.backend.domain.ports.out.AuthService.class)
                    .to(com.projectx.backend.infra.adapters.out.auth.MockAuthService.class).in(Singleton.class);
        } else {
            // TODO: Bind CognitoAuthService cuando se configure Cognito en producción
            bind(com.projectx.backend.domain.ports.out.AuthService.class)
                    .to(com.projectx.backend.infra.adapters.out.auth.MockAuthService.class).in(Singleton.class);
        }
    }

    /**
     * Provee el cliente de DynamoDB configurado.
     * Solo se crea cuando hay un endpoint configurado (DynamoDB Local o
     * producción).
     * En desarrollo sin endpoint, no se usa — los repos son InMemory.
     */
    @Provides
    @Singleton
    public DynamoDbClient dynamoDbClient() {
        String endpoint = appConfig.getDynamoDbEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            // En desarrollo sin DynamoDB, retornar un cliente dummy que no se usará
            // porque todos los repos están en InMemory.
            // Guice requiere que el provider retorne algo no-null.
            return DynamoDbClient.builder()
                    .region(Region.of(appConfig.getAwsRegion()))
                    .endpointOverride(URI.create("http://localhost:8000"))
                    .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                            software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("dummy", "dummy")))
                    .build();
        }
        return DynamoDbClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .endpointOverride(URI.create(endpoint))
                .build();
    }

    /**
     * Provee el cliente de SES configurado.
     * Solo se usa cuando email.enabled=true.
     * En desarrollo, el EmailService es LogEmailService y no usa este cliente.
     */
    @Provides
    @Singleton
    public SesClient sesClient() {
        if (!appConfig.isEmailEnabled()) {
            // Retornar cliente dummy con endpoint local — LogEmailService no lo usa.
            // Se necesita endpoint override para evitar resolución DNS a AWS real.
            return SesClient.builder()
                    .region(Region.of(appConfig.getAwsRegion()))
                    .endpointOverride(URI.create("http://localhost:8025"))
                    .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                            software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("dummy", "dummy")))
                    .build();
        }
        return SesClient.builder()
                .region(Region.of(appConfig.getAwsRegion()))
                .build();
    }
}
