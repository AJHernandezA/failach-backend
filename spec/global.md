# Project-X Backend — Especificación Global

## Visión

El backend de Project-X es una API REST multi-tenant construida con Java 21, Javalin y arquitectura hexagonal. Sirve a todas las tiendas desde un solo despliegue, aislando datos por tenant mediante partition keys en DynamoDB.

## Stack Tecnológico

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| **Java** | 21 | Lenguaje principal (virtual threads, records, sealed classes) |
| **Javalin** | 6.x | Framework web ligero para REST APIs |
| **Guice** | 7.x | Inyección de dependencias |
| **Jackson** | 2.x | Serialización/deserialización JSON |
| **AWS SDK v2** | 2.26+ | DynamoDB, SES, S3, Cognito |
| **jjwt** | 0.12+ | JWT tokens (validación de Cognito JWT) |
| **JUnit 5** | 5.10+ | Framework de testing |
| **Mockito** | 5.x | Framework de mocking |
| **Gradle** | 8.x | Build tool |
| **ShadowJar** | 8.x | Fat JAR para deploy |

## Reglas Fundamentales

1. **SÍ comentarios explicatorios en español.** Cada clase, método complejo y decisión de diseño debe tener comentario.
2. **Arquitectura hexagonal estricta.** Domain no depende de nada externo.
3. **Un use case por clase.** Single Responsibility.
4. **Records para DTOs y value objects.** Inmutabilidad por defecto.
5. **Interfaces para ports y use cases.** Inversión de dependencias.
6. **Validación doble.** Nunca confiar en datos del frontend.
7. **Multi-tenant en cada query.** Siempre filtrar por `tenantId`.

## Estructura del Paquete Base

```
com.projectx.backend
├── Main.java                          → Punto de entrada
├── domain/                            → Capa de dominio (PURA)
│   ├── models/                        → Entidades y value objects
│   ├── enums/                         → Enumeraciones de dominio
│   ├── ports/                         → Interfaces de repositorios y servicios externos
│   ├── usecases/                      → Interfaces de use cases
│   ├── exceptions/                    → Excepciones de dominio
│   └── constants/                     → Constantes de dominio
├── application/                       → Capa de aplicación
│   ├── usecases/                      → Implementaciones de use cases
│   │   ├── tenant/
│   │   ├── product/
│   │   ├── cart/
│   │   ├── order/
│   │   └── payment/
│   └── services/                      → Servicios de aplicación
└── infra/                             → Capa de infraestructura
    ├── adapters/
    │   ├── in/                        → Adaptadores de entrada
    │   │   ├── controller/            → Controllers (rutas Javalin)
    │   │   ├── handler/               → Handlers (delegación)
    │   │   ├── dto/                   → DTOs request/response
    │   │   └── mapper/                → Mappers domain ↔ DTO
    │   └── out/                       → Adaptadores de salida
    │       ├── dynamodb/              → Repositorios DynamoDB
    │       ├── ses/                   → Servicio de email
    │       ├── s3/                    → Servicio de storage
    │       └── wompi/                 → Servicio de pagos
    ├── auth/                          → Cognito + JWT validation
    ├── config/                        → Guice modules, app config
    └── middleware/                     → TenantFilter, CorsFilter, RateLimiter
```

## Multi-Tenant: Cómo Funciona

1. Cada request llega con header `X-Tenant-Id`.
2. `TenantFilter` (middleware Javalin) intercepta el request.
3. Valida que el header existe y que el tenant está activo.
4. Inyecta el `tenantId` en el contexto de Javalin (`ctx.attribute("tenantId", tenantId)`).
5. Cada controller/handler lee el `tenantId` del contexto.
6. Cada query a DynamoDB incluye `TENANT#<tenantId>` como partition key.
7. Resultado: **aislamiento total de datos entre tenants.**

## Formato de Respuesta API

### Éxito
```json
{
  "data": { ... },
  "meta": { "page": 0, "size": 20, "total": 100 }
}
```

### Error
```json
{
  "error": {
    "code": "PRODUCT_NOT_FOUND",
    "message": "Product with id 'abc123' not found"
  }
}
```

## Manejo de Excepciones

| Excepción | HTTP | Cuándo |
|-----------|------|--------|
| `BadRequestException` | 400 | Validación de input fallida |
| `UnauthorizedException` | 401 | No autenticado |
| `ForbiddenException` | 403 | Sin permisos |
| `NotFoundException` | 404 | Recurso no existe |
| `ConflictException` | 409 | Recurso ya existe |
| `BusinessRuleException` | 422 | Regla de negocio violada |
| `TooManyRequestsException` | 429 | Rate limit excedido |
| `Exception` (genérica) | 500 | Error inesperado (loguear, no exponer) |

## Orden de Implementación

1. **F001** → Setup Gradle, Main.java, TenantFilter, health check, CORS
2. **F002** → Tenant entity, TenantRepository, GET/PUT config
3. **F011** → Auth filter, JWT validation, roles
4. **F003** → Product CRUD, categories, paginación
5. **F004** → Cart API, session management
6. **F005** → Order creation, stock validation
7. **F006** → Wompi integration, webhook
8. **F007** → Manual payment confirmation
9. **F008** → Order status updates, status history
10. **F009** → SES email templates, sending
11. **F010** → WhatsApp message generation
12. **F012** → Storefront aggregation endpoint
13. **F014** → City validation
14. **F015** → Social media (parte de tenant config, no endpoint extra)
15. **F016** → SEO data endpoint (sitemap data)
16. **F018** → Legal content endpoint

## Testing

- **Unit tests**: Cada use case implementation tiene su test con mocks.
- **Integration tests**: Cada controller tiene test end-to-end con Javalin TestTools.
- **Naming**: `[Clase]Test.java` para unitarios, `[Clase]IntegrationTest.java` para integración.
- **Coverage**: Mínimo 80% en domain y application layers.
