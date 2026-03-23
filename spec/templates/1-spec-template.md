# [CÓDIGO] — [Título] (Backend)

## Propósito

[Descripción de lo que se implementa en el backend para esta feature]

## Endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/...` | [Descripción] | [No/SUPER_ADMIN/MERCHANT] |

## Domain Layer

### Entidades (models/)

| Entidad | Tipo | Campos principales |
|---------|------|-------------------|
| `[Nombre]` | record/class | [campos] |

### Enums (enums/)

| Enum | Valores |
|------|---------|
| `[Nombre]` | `VAL1`, `VAL2`, `VAL3` |

### Ports (ports/)

| Interface | Métodos |
|-----------|---------|
| `[NombreRepository]` | `findById()`, `save()`, `delete()` |

### Use Cases (usecases/)

| Interface | Input | Output | Descripción |
|-----------|-------|--------|-------------|
| `[NombreUseCase]` | `[Tipo]` | `[Tipo]` | [Descripción] |

### Exceptions

| Excepción | HTTP | Cuándo |
|-----------|------|--------|
| `[NombreException]` | [código] | [Cuándo se lanza] |

## Application Layer

### Use Case Implementations

| Clase | Dependencias (@Inject) | Lógica principal |
|-------|----------------------|------------------|
| `[NombreUseCaseImpl]` | `[Repo1]`, `[Repo2]` | [Descripción de la lógica] |

## Infrastructure Layer

### DTOs (dto/)

| DTO | Tipo | Campos |
|-----|------|--------|
| `[NombreRequestDto]` | record | [campos] |
| `[NombreResponseDto]` | record | [campos] |

### Mappers (mapper/)

| Mapper | Métodos |
|--------|---------|
| `[NombreMapper]` | `toDto(Entity)`, `toEntity(Dto)` |

### Repository Implementation (dynamodb/)

| Clase | Port | Access Patterns |
|-------|------|----------------|
| `[DynamoDbNombreRepository]` | `[NombreRepository]` | PK: `TENANT#<id>`, SK: `[ENTITY]#<id>` |

### Controller / Handler

| Clase | Rutas registradas |
|-------|------------------|
| `[NombreController]` | `GET /api/v1/...`, `POST /api/v1/...` |
| `[NombreHandler]` | Delegación al use case |

## Validaciones y Reglas de Negocio

1. [Regla 1]
2. [Regla 2]

## DynamoDB Access Patterns

| Operación | PK | SK | Descripción |
|-----------|----|----|-------------|
| [operación] | `TENANT#<id>` | `[ENTITY]#<id>` | [Descripción] |

## Criterios de Aceptación

- [ ] [Criterio 1]
- [ ] [Criterio 2]

## Testing

### Unitarios
- [ ] `[UseCaseImplTest]` — [qué se testea]

### Integración
- [ ] `[ControllerIntegrationTest]` — [qué se testea]

## Notas

- [Nota 1]
