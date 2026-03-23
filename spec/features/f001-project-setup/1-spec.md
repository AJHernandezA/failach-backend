# F001 — Project Setup (Backend)

## Propósito

Crear el proyecto Java 21 con Gradle, Javalin, Guice y toda la infraestructura base: configuración del servidor, middleware multi-tenant, CORS, manejo global de excepciones y health check endpoint.

## Endpoints

| Método | Ruta             | Descripción               | Auth |
| ------ | ---------------- | ------------------------- | ---- |
| `GET`  | `/api/v1/health` | Health check del servicio | No   |

## Domain Layer

### Exceptions

| Excepción               | HTTP | Cuándo                   |
| ----------------------- | ---- | ------------------------ |
| `DomainException`       | —    | Clase base abstracta     |
| `BadRequestException`   | 400  | Input inválido           |
| `UnauthorizedException` | 401  | No autenticado           |
| `ForbiddenException`    | 403  | Sin permisos             |
| `NotFoundException`     | 404  | Recurso no encontrado    |
| `ConflictException`     | 409  | Recurso duplicado        |
| `BusinessRuleException` | 422  | Regla de negocio violada |

### Constants

| Constante           | Valor     | Descripción                  |
| ------------------- | --------- | ---------------------------- |
| `DEFAULT_PAGE_SIZE` | 20        | Tamaño de página por defecto |
| `MAX_PAGE_SIZE`     | 100       | Tamaño máximo de página      |
| `API_PREFIX`        | `/api/v1` | Prefijo de todas las rutas   |

## Infrastructure Layer

### Main.java

- Crea instancia de Javalin.
- Configura Guice Injector con módulos.
- Registra controllers.
- Registra exception handlers globales.
- Registra middleware (TenantFilter, CorsFilter).
- Inicia servidor en puerto configurable (default 7070).

### TenantFilter (middleware/)

- Intercepta TODOS los requests excepto `/api/v1/health` y `/api/v1/webhooks/*`.
- Lee header `X-Tenant-Id`.
- Si no existe: responde `400 { "error": { "code": "MISSING_TENANT", "message": "X-Tenant-Id header is required" } }`.
- Si existe: setea `ctx.attribute("tenantId", tenantId)`.

### CorsFilter (middleware/)

- Lee orígenes permitidos de configuración.
- Agrega headers CORS: `Access-Control-Allow-Origin`, `Allow-Methods`, `Allow-Headers`.
- Maneja preflight OPTIONS requests.

### ExceptionHandler (config/)

- Registra handlers para cada tipo de `DomainException`.
- Mapea excepción → código HTTP + JSON de error.
- Para excepciones inesperadas: log error + responder `500` sin exponer stack trace.

### AppConfig (config/)

- Lee `application.properties` o variables de entorno.
- Expone configuración como singleton inyectable.

### Guice Modules

- `UseCaseModule` — bind de use case interfaces a implementaciones.
- `InfraModule` — bind de ports a implementaciones (DynamoDB, SES, etc.).
- `ControllerModule` — bind de controllers.

## Dependencias (build.gradle)

```groovy
dependencies {
    // Web framework
    implementation 'io.javalin:javalin:6.3.0'
    implementation 'io.javalin:javalin-rendering:6.3.0'

    // DI
    implementation 'com.google.inject:guice:7.0.0'

    // JSON
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.17.0'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0'
    implementation 'com.fasterxml.jackson.module:jackson-module-parameter-names:2.17.0'

    // AWS SDK v2
    implementation platform('software.amazon.awssdk:bom:2.26.0')
    implementation 'software.amazon.awssdk:dynamodb'
    implementation 'software.amazon.awssdk:dynamodb-enhanced'
    implementation 'software.amazon.awssdk:ses'
    implementation 'software.amazon.awssdk:s3'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'

    // Logging
    implementation 'org.slf4j:slf4j-api:2.0.12'
    implementation 'ch.qos.logback:logback-classic:1.5.3'

    // Testing
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
    testImplementation 'org.mockito:mockito-core:5.11.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'
}
```

## Configuración

### application.properties

```properties
server.port=7070
server.host=0.0.0.0
cors.allowedOrigins=http://localhost:3000
```

### build.gradle

```groovy
plugins {
    id 'java'
    id 'application'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = 'com.projectx.backend.Main'
}
```

## Criterios de Aceptación

- [x] `./gradlew run` arranca servidor en localhost:7070 sin errores.
- [x] `./gradlew build` compila sin errores.
- [x] `./gradlew test` ejecuta tests sin fallos.
- [x] `GET /api/v1/health` retorna `200 { "status": "UP", "timestamp": "..." }`.
- [x] Request sin `X-Tenant-Id` a cualquier ruta (excepto health) retorna `400`.
- [x] Request con `X-Tenant-Id: idoneo` a health retorna `200` (health no lo requiere pero no falla si lo tiene).
- [x] CORS permite `http://localhost:3000`.
- [x] Excepción inesperada retorna `500` sin stack trace.
- [x] `.gitignore` configurado (ignora build/, .gradle/, .idea/).
- [x] `README.md` con instrucciones de setup.
- [x] `Dockerfile` para deploy futuro.

## Notas

- Java 21 permite virtual threads. Javalin 6 los soporta nativamente.
- Activar virtual threads: `Javalin.create(config -> config.useVirtualThreads = true)`.
- ShadowJar genera un fat JAR para deploy en App Runner.
- DynamoDB Local se configura apuntando el SDK a `http://localhost:8000`.
