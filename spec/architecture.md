# Project-X Backend — Arquitectura

## Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                          │
│                                                                  │
│  ┌──────────────────────┐     ┌───────────────────────────────┐ │
│  │   Adapters IN         │     │   Adapters OUT                │ │
│  │                       │     │                               │ │
│  │  Controller (Javalin) │     │  DynamoDbTenantRepository     │ │
│  │  Handler              │     │  DynamoDbProductRepository    │ │
│  │  DTOs (Request/Resp)  │     │  DynamoDbOrderRepository      │ │
│  │  Mappers              │     │  DynamoDbCartRepository       │ │
│  │                       │     │  SesEmailService              │ │
│  │                       │     │  S3StorageService             │ │
│  │                       │     │  WompiPaymentService          │ │
│  └──────────┬────────────┘     └──────────────┬────────────────┘ │
│             │                                  │                  │
│  ┌──────────┘                                  │                  │
│  │  Middleware: TenantFilter, AuthFilter, CORS │                  │
│  │  Config: GuiceModules, AppConfig            │                  │
│  └─────────────────────────────────────────────┘                  │
└────────────────────────┬────────────────────────────────────┬─────┘
                         │ implements                          │ implements
                         ▼                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                     APPLICATION LAYER                            │
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Use Case Implementations (@Singleton, @Inject)            │  │
│  │                                                            │  │
│  │  GetTenantConfigUseCaseImpl                                │  │
│  │  CreateProductUseCaseImpl                                  │  │
│  │  AddToCartUseCaseImpl                                      │  │
│  │  CreateOrderUseCaseImpl                                    │  │
│  │  ProcessWompiWebhookUseCaseImpl                            │  │
│  │  ...                                                       │  │
│  └───────────────────────────────────────────────────────────┘  │
└────────────────────────┬────────────────────────────────────────┘
                         │ implements
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DOMAIN LAYER (PURE)                         │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │   Models     │  │   Ports      │  │   Use Cases (interfaces)│ │
│  │              │  │  (interfaces)│  │                         │ │
│  │  Tenant      │  │              │  │  GetTenantConfigUseCase │ │
│  │  Product     │  │  TenantRepo  │  │  CreateProductUseCase   │ │
│  │  Order       │  │  ProductRepo │  │  AddToCartUseCase       │ │
│  │  Cart        │  │  OrderRepo   │  │  CreateOrderUseCase     │ │
│  │  Customer    │  │  CartRepo    │  │  ...                    │ │
│  │  Category    │  │  EmailService│  │                         │ │
│  │              │  │  StorageSvc  │  │                         │ │
│  │              │  │  PaymentSvc  │  │                         │ │
│  └─────────────┘  └──────────────┘  └────────────────────────┘ │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│  │   Enums      │  │  Exceptions  │  │   Constants             │ │
│  │              │  │              │  │                         │ │
│  │  OrderStatus │  │  NotFound    │  │  MAX_PAGE_SIZE          │ │
│  │  PaymentMeth │  │  BadRequest  │  │  DEFAULT_PAGE_SIZE      │ │
│  │  UserRole    │  │  Unauthorized│  │  ...                    │ │
│  └─────────────┘  └──────────────┘  └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Flujo de un Request

```
HTTP Request
    │
    ▼
Javalin Router
    │
    ├─ TenantFilter: extrae X-Tenant-Id → ctx.attribute("tenantId")
    ├─ AuthFilter: valida JWT → ctx.attribute("user") (si endpoint protegido)
    ├─ CorsFilter: valida origen
    │
    ▼
Controller (define rutas y extrae parámetros)
    │
    ▼
Handler (delega al use case, mapea respuesta a DTO)
    │
    ▼
UseCase Implementation (lógica de negocio)
    │
    ├─ Valida input
    ├─ Aplica reglas de negocio
    ├─ Llama a ports (repository, services)
    │
    ▼
Repository Implementation (DynamoDB)
    │
    ├─ Construye query con PK/SK
    ├─ Ejecuta query
    ├─ Mapea DynamoDB Item → Domain Entity
    │
    ▼
Response: Domain Entity → DTO → JSON → HTTP Response
```

## DynamoDB — Single Table Design

### Tabla Principal: `ProjectX`

| PK (Partition Key) | SK (Sort Key) | Tipo | Descripción |
|---------------------|---------------|------|-------------|
| `TENANT#idoneo` | `CONFIG` | Tenant | Config del tenant |
| `TENANT#idoneo` | `CATEGORY#cat001` | Category | Categoría del tenant |
| `TENANT#idoneo` | `PRODUCT#prod001` | Product | Producto |
| `TENANT#idoneo` | `PRODUCT#CAT#cat001#prod001` | Product (GSI) | Producto por categoría |
| `TENANT#idoneo` | `CART#session123` | Cart | Carrito de sesión |
| `TENANT#idoneo` | `ORDER#ord001` | Order | Orden |
| `TENANT#idoneo` | `ORDER#STATUS#PENDING#ord001` | Order (GSI) | Orden por estado |
| `TENANT#idoneo` | `LEGAL#terms` | Legal | Contenido legal |

### GSIs

| GSI | PK | SK | Uso |
|-----|----|----|-----|
| `GSI1` | `GSI1PK` | `GSI1SK` | Buscar orden por orderCode |
| `GSI2` | `GSI2PK` | `GSI2SK` | Buscar órdenes por email del comprador |

## Guice Dependency Injection

### Módulos

```java
// Módulo de dominio: bind use cases
public class UseCaseModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GetTenantConfigUseCase.class).to(GetTenantConfigUseCaseImpl.class).in(Singleton.class);
        bind(CreateProductUseCase.class).to(CreateProductUseCaseImpl.class).in(Singleton.class);
        // ...
    }
}

// Módulo de infraestructura: bind repositories y servicios
public class InfraModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(TenantRepository.class).to(DynamoDbTenantRepository.class).in(Singleton.class);
        bind(ProductRepository.class).to(DynamoDbProductRepository.class).in(Singleton.class);
        // ...
    }
}
```

## Configuración

### application.properties
```properties
server.port=7070
server.host=0.0.0.0

aws.region=us-east-1
aws.dynamodb.table=ProjectX
aws.s3.bucket=projectx-assets
aws.ses.from=noreply@projectx.com

cognito.userPoolId=us-east-1_XXXXXX
cognito.clientId=XXXXXX
cognito.jwksUrl=https://cognito-idp.us-east-1.amazonaws.com/us-east-1_XXXXXX/.well-known/jwks.json

wompi.publicKey=pub_test_XXXXX
wompi.eventsSecret=test_events_XXXXX

cors.allowedOrigins=http://localhost:3000,https://*.projectx.com
```

## Health Check

```
GET /api/v1/health

Response: 200
{
  "status": "UP",
  "timestamp": "2026-03-12T12:00:00Z"
}
```

No requiere `X-Tenant-Id` ni auth.
