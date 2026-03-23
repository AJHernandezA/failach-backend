# Plan de Implementación — [CÓDIGO] (Backend)

## Objetivo
[Descripción breve]

## Archivos a Crear

```
backend/src/main/java/com/projectx/backend/
├── domain/
│   ├── models/[Entidad].java
│   ├── enums/[Enum].java
│   ├── ports/[Repository].java
│   ├── usecases/[UseCase].java
│   └── exceptions/[Exception].java
├── application/
│   └── usecases/[UseCaseImpl].java
└── infra/
    ├── adapters/in/
    │   ├── controller/[Controller].java
    │   ├── handler/[Handler].java
    │   ├── dto/[Dto].java
    │   └── mapper/[Mapper].java
    └── adapters/out/
        └── dynamodb/[RepositoryImpl].java
```

## Archivos de Test

```
backend/src/test/java/com/projectx/backend/
├── unit/[UseCaseImplTest].java
└── integration/[ControllerIntegrationTest].java
```

---

## Paso a Paso

### Fase 1: Domain Layer

#### 1.1 Crear Entidades
- [ ] Crear `[Entidad].java` como record
- [ ] Agregar factory method `create()`
- [ ] Agregar validaciones en constructor

#### 1.2 Crear Enums (si aplica)
- [ ] Crear `[Enum].java` con valores

#### 1.3 Crear Ports
- [ ] Crear `[Repository].java` interface
- [ ] Definir métodos: `findById()`, `save()`, etc.

#### 1.4 Crear Use Case Interface
- [ ] Crear `[UseCase].java` con `@FunctionalInterface`
- [ ] Documentar con Javadoc

#### 1.5 Crear Exceptions (si aplica)
- [ ] Crear `[Exception].java` extendiendo `DomainException`

---

### Fase 2: Application Layer

#### 2.1 Implementar Use Case
- [ ] Crear `[UseCaseImpl].java`
- [ ] Anotar con `@Singleton`
- [ ] Inyectar dependencias con `@Inject`
- [ ] Implementar lógica con comentarios explicatorios
- [ ] Manejar excepciones de dominio

---

### Fase 3: Infrastructure Layer

#### 3.1 Crear DTOs
- [ ] `[RequestDto].java` — campos de entrada
- [ ] `[ResponseDto].java` — campos de salida

#### 3.2 Crear Mapper
- [ ] `[Mapper].java` con métodos estáticos `toDto()` y `toEntity()`

#### 3.3 Implementar Repository (DynamoDB)
- [ ] `DynamoDb[Repository].java`
- [ ] Implementar cada método del port
- [ ] Mapear PK/SK correctamente

#### 3.4 Crear Handler
- [ ] `[Handler].java`
- [ ] Extraer tenantId del context
- [ ] Llamar al use case
- [ ] Mapear response a DTO
- [ ] Manejar errores

#### 3.5 Crear/Modificar Controller
- [ ] Registrar rutas en `[Controller].java`
- [ ] Extraer path params y query params
- [ ] Delegar al handler

#### 3.6 Configurar DI
- [ ] Agregar binding en `UseCaseModule`
- [ ] Agregar binding en `InfraModule`

---

### Fase 4: Testing

#### 4.1 Unit Tests
- [ ] Crear `[UseCaseImplTest].java`
- [ ] Test: happy path
- [ ] Test: validaciones
- [ ] Test: excepciones
- [ ] Mock de todas las dependencias

#### 4.2 Integration Tests
- [ ] Crear `[ControllerIntegrationTest].java`
- [ ] Test: endpoint exitoso
- [ ] Test: endpoint con error
- [ ] Test: sin auth (si aplica)

---

### Fase 5: Validación

- [ ] Compilar sin errores: `./gradlew build`
- [ ] Tests pasan: `./gradlew test`
- [ ] Probar manualmente con curl/Postman
- [ ] Verificar logs en consola
- [ ] Actualizar `5-summary.md`

---

## Notas
- [Nota 1]
