# Project-X Backend

API REST multi-tenant para plataforma de e-commerce SaaS.

## Stack

- **Java 21** + **Javalin 6** + **Guice 7**
- **DynamoDB** (single-table design)
- **AWS SES** (emails) + **S3** (assets)
- **JWT** (Cognito)

## Requisitos

- Java 21+
- Gradle 8+ (o usar `./gradlew`)

## Setup

```bash
# Compilar
./gradlew build

# Ejecutar
./gradlew run

# Tests
./gradlew test

# Fat JAR para deploy
./gradlew shadowJar
```

El servidor arranca en `http://localhost:7070`.

## Health Check

```bash
curl http://localhost:7070/api/v1/health
```

Response:
```json
{
  "status": "UP",
  "timestamp": "2026-03-12T12:00:00Z"
}
```

## Headers Requeridos

Todos los endpoints (excepto `/health` y `/webhooks/*`) requieren:

| Header | Descripción |
|--------|-------------|
| `X-Tenant-Id` | Identificador del tenant (ej: `idoneo`) |
| `Authorization` | `Bearer <JWT>` (solo endpoints protegidos) |

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `SERVER_PORT` | `7070` | Puerto del servidor |
| `CORS_ALLOWEDORIGINS` | `http://localhost:3000` | Orígenes CORS permitidos |
| `AWS_REGION` | `us-east-1` | Región de AWS |
| `AWS_DYNAMODB_TABLE` | `ProjectX` | Nombre de tabla DynamoDB |
| `AWS_DYNAMODB_ENDPOINT` | — | Endpoint local de DynamoDB |

## Docker

```bash
./gradlew shadowJar
docker build -t project-x-backend .
docker run -p 7070:7070 project-x-backend
```
