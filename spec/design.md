# Project-X Backend — Reglas de Diseño

## Reglas de Código

### Comentarios

- **SÍ comentarios en español** para explicar: propósito de la clase, lógica compleja, decisiones de diseño.
- Formato Javadoc para clases públicas y métodos de interface.
- Comentarios inline para lógica no obvia.

### Naming

| Elemento     | Convención                                    | Ejemplo                                         |
| ------------ | --------------------------------------------- | ----------------------------------------------- |
| Packages     | lowercase                                     | `com.projectx.backend.domain.models`            |
| Clases       | PascalCase                                    | `CreateOrderUseCaseImpl`                        |
| Interfaces   | PascalCase                                    | `CreateOrderUseCase`                            |
| Métodos      | camelCase                                     | `findByTenantId()`                              |
| Variables    | camelCase                                     | `tenantId`                                      |
| Constantes   | UPPER_SNAKE_CASE                              | `MAX_PAGE_SIZE`                                 |
| Enums        | PascalCase (tipo), UPPER_SNAKE_CASE (valores) | `OrderStatus.PENDING`                           |
| DTOs         | PascalCase + sufijo                           | `CreateProductRequestDto`, `ProductResponseDto` |
| Mappers      | PascalCase + Mapper                           | `ProductMapper`                                 |
| Repositories | PascalCase + Repository                       | `DynamoDbProductRepository`                     |

### Estructura de Clases

```java
/**
 * Descripción de la clase y su propósito.
 */
@Singleton
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {

    // Dependencias inyectadas
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Constructor con inyección de dependencias.
     */
    @Inject
    public CreateOrderUseCaseImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    /**
     * Crea una nueva orden a partir del carrito del comprador.
     *
     * @param request datos del checkout
     * @return la orden creada
     * @throws BusinessRuleException si el stock es insuficiente
     */
    @Override
    public Order apply(CreateOrderRequest request) {
        // Validar que el carrito no esté vacío
        if (request.items().isEmpty()) {
            throw new BadRequestException("El carrito está vacío");
        }

        // Verificar stock de cada producto
        for (var item : request.items()) {
            var product = productRepository.findById(request.tenantId(), item.productId())
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + item.productId()));

            if (product.stock() < item.quantity()) {
                throw new BusinessRuleException("Stock insuficiente para: " + product.name());
            }
        }

        // Crear la orden
        var order = Order.create(request);
        orderRepository.save(order);

        return order;
    }
}
```

### Records para DTOs y Value Objects

```java
/**
 * Request para crear un producto.
 */
public record CreateProductRequestDto(
    String name,
    String description,
    BigDecimal price,
    List<String> images,
    String categoryId,
    Integer stock
) {}

/**
 * Response de un producto.
 */
public record ProductResponseDto(
    String productId,
    String name,
    String description,
    BigDecimal price,
    BigDecimal compareAtPrice,
    List<String> images,
    String categoryId,
    String categoryName,
    Integer stock,
    String status,
    Instant createdAt,
    Instant updatedAt
) {}
```

### Interfaces de Use Cases

```java
/**
 * Caso de uso: obtener la configuración de un tenant.
 */
@FunctionalInterface
public interface GetTenantConfigUseCase {
    /**
     * @param tenantId identificador del tenant
     * @return configuración completa del tenant
     * @throws NotFoundException si el tenant no existe
     */
    Tenant apply(String tenantId);
}
```

### Interfaces de Ports (Repositories)

```java
/**
 * Puerto de salida para persistencia de productos.
 */
public interface ProductRepository {
    /**
     * Busca un producto por tenant y ID.
     */
    Optional<Product> findById(String tenantId, String productId);

    /**
     * Lista productos de un tenant con paginación y filtros.
     */
    Page<Product> findByTenant(String tenantId, ProductFilter filter);

    /**
     * Guarda un producto.
     */
    void save(Product product);

    /**
     * Elimina un producto (soft delete).
     */
    void delete(String tenantId, String productId);
}
```

## Reglas de Arquitectura

### Domain Layer

- **CERO dependencias externas.** No importar Javalin, Jackson, AWS SDK, Guice.
- Solo Java estándar: `java.util`, `java.time`, `java.math`.
- Entidades son records inmutables o clases con factory methods.
- Excepciones extienden de `RuntimeException`.

### Application Layer

- Puede importar: Domain, Guice (`@Inject`, `@Singleton`).
- NO puede importar: Javalin, Jackson, AWS SDK.
- Cada use case es una clase con un solo método público (`apply`).

### Infrastructure Layer

- Puede importar todo: Domain, Application, Javalin, Jackson, AWS SDK.
- Controllers NO tienen lógica de negocio. Solo extraen params y delegan.
- Handlers orquestan: obtener user, llamar use case, mapear respuesta.
- Mappers convierten entre Domain entities y DTOs.

## Manejo de Errores

```java
// Excepción base del dominio
public abstract class DomainException extends RuntimeException {
    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}

// Excepciones específicas
public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}

// Manejo global en Javalin
app.exception(NotFoundException.class, (e, ctx) -> {
    ctx.status(404).json(Map.of("error", Map.of("code", e.getCode(), "message", e.getMessage())));
});
```

## Paginación

```java
/**
 * Contenedor de resultados paginados.
 */
public record Page<T>(
    List<T> items,
    int page,
    int size,
    long total
) {
    public boolean hasNext() {
        return (long) (page + 1) * size < total;
    }
}
```

Query params estándar: `?page=0&size=20`

- `page`: 0-indexed, default 0.
- `size`: default 20, max 100.

## Logging

- SLF4J con Logback.
- Nivel INFO para operaciones normales.
- Nivel WARN para situaciones recuperables.
- Nivel ERROR para excepciones inesperadas.
- **NUNCA loguear datos sensibles** (passwords, tokens, tarjetas, datos bancarios).

```java
private static final Logger log = LoggerFactory.getLogger(CreateOrderUseCaseImpl.class);

// Bien
log.info("Orden creada: orderId={}, tenantId={}", order.orderId(), order.tenantId());

// MAL - nunca loguear datos sensibles
// log.info("Orden creada con tarjeta: {}", cardNumber);
```

## Testing

### Stack de Testing (todo gratuito/open-source)

| Herramienta           | Propósito                     | Capa                 |
| --------------------- | ----------------------------- | -------------------- |
| **JUnit 5**           | Framework de testing          | Todas las capas      |
| **Mockito 5**         | Mocking de dependencias       | Unit tests           |
| **AssertJ**           | Assertions fluidas y legibles | Todas las capas      |
| **Javalin TestTools** | Tests HTTP de integración     | Controllers/Handlers |
| **JaCoCo**            | Reporte de coverage           | CI/CD                |

### Qué testear

| Prioridad | Qué                                  | Ejemplo                                                      | Herramienta                       |
| --------- | ------------------------------------ | ------------------------------------------------------------ | --------------------------------- |
| **P0**    | Use cases (application layer)        | `CreateOrderUseCaseImpl`, `AddToCartUseCaseImpl`             | JUnit 5 + Mockito                 |
| **P0**    | Lógica de dominio                    | `Order.create()`, `Cart.addItem()`, validaciones en entities | JUnit 5                           |
| **P0**    | Excepciones de dominio               | `NotFoundException`, `BusinessRuleException`                 | JUnit 5                           |
| **P1**    | Mappers                              | `ProductMapper.toDto()`, `OrderMapper.toEntity()`            | JUnit 5                           |
| **P1**    | Handlers / Controllers (integración) | `POST /api/v1/tenants/:id/orders` end-to-end                 | Javalin TestTools                 |
| **P1**    | Middleware                           | `TenantFilter`, `AuthFilter`                                 | Javalin TestTools                 |
| **P2**    | Repositorios DynamoDB                | `DynamoDbProductRepository.findById()`                       | JUnit 5 + DynamoDB Local (Docker) |

### Qué NO testear en MVP

- Código generado (Guice wiring, Jackson serialization estándar).
- Getters/setters triviales de records.
- Configuración de Gradle o plugins.

### Estructura de Archivos de Test

```
backend/
├── src/
│   ├── main/java/com/projectx/backend/
│   │   └── ...                              → Código de producción
│   └── test/
│       ├── java/com/projectx/backend/
│       │   ├── unit/                        → Tests unitarios
│       │   │   ├── domain/
│       │   │   │   ├── models/
│       │   │   │   │   └── OrderTest.java
│       │   │   │   └── exceptions/
│       │   │   │       └── DomainExceptionTest.java
│       │   │   ├── application/
│       │   │   │   └── usecases/
│       │   │   │       ├── tenant/
│       │   │   │       │   └── GetTenantConfigUseCaseImplTest.java
│       │   │   │       ├── product/
│       │   │   │       │   └── CreateProductUseCaseImplTest.java
│       │   │   │       ├── cart/
│       │   │   │       │   └── AddToCartUseCaseImplTest.java
│       │   │   │       └── order/
│       │   │   │           └── CreateOrderUseCaseImplTest.java
│       │   │   └── infra/
│       │   │       └── mapper/
│       │   │           ├── ProductMapperTest.java
│       │   │           └── OrderMapperTest.java
│       │   ├── integration/                 → Tests de integración
│       │   │   ├── controller/
│       │   │   │   ├── TenantControllerIntegrationTest.java
│       │   │   │   ├── ProductControllerIntegrationTest.java
│       │   │   │   ├── CartControllerIntegrationTest.java
│       │   │   │   └── OrderControllerIntegrationTest.java
│       │   │   └── middleware/
│       │   │       ├── TenantFilterIntegrationTest.java
│       │   │       └── AuthFilterIntegrationTest.java
│       │   └── helpers/                     → Utilidades compartidas para tests
│       │       ├── TestDataFactory.java     → Builders de objetos de test
│       │       └── IntegrationTestBase.java → Clase base para tests de integración
│       └── resources/
│           └── application-test.properties  → Config override para tests
```

### Convenciones de Test

**Naming:**

- Clases: `[ClaseBajoTest]Test.java` (unitarios), `[ClaseBajoTest]IntegrationTest.java` (integración).
- Métodos: `should[Comportamiento]When[Condición]()` en inglés.
- **Comentarios en español** (misma regla que producción): `// Given`, `// When`, `// Then` se mantienen en inglés como excepción por ser patrón universal.

**Patrón Given-When-Then:**

```java
@Test
void shouldThrowNotFoundWhenTenantDoesNotExist() {
    // Given - configurar el estado inicial
    var tenantId = "nonexistent";
    when(tenantRepository.findById(tenantId))
        .thenReturn(Optional.empty());

    // When & Then - ejecutar y verificar
    assertThatThrownBy(() -> useCase.apply(tenantId))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("nonexistent");
}
```

### Unit Tests — Use Cases

```java
/**
 * Tests unitarios para el caso de uso de crear orden.
 */
@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CreateOrderUseCaseImpl useCase;

    @Test
    void shouldCreateOrderSuccessfully() {
        // Given
        var request = TestDataFactory.createValidOrderRequest();
        when(productRepository.findById(anyString(), anyString()))
            .thenReturn(Optional.of(TestDataFactory.createProductWithStock(10)));

        // When
        var result = useCase.apply(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void shouldThrowWhenCartIsEmpty() {
        // Given
        var request = TestDataFactory.createEmptyCartRequest();

        // When & Then
        assertThatThrownBy(() -> useCase.apply(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("vacío");
    }

    @Test
    void shouldThrowWhenProductOutOfStock() {
        // Given
        var request = TestDataFactory.createOrderRequestWithQuantity(5);
        when(productRepository.findById(anyString(), anyString()))
            .thenReturn(Optional.of(TestDataFactory.createProductWithStock(2)));

        // When & Then
        assertThatThrownBy(() -> useCase.apply(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        // Given
        var request = TestDataFactory.createValidOrderRequest();
        when(productRepository.findById(anyString(), anyString()))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> useCase.apply(request))
            .isInstanceOf(NotFoundException.class);
    }
}
```

### Unit Tests — Domain Models

```java
/**
 * Tests de lógica de dominio en la entidad Order.
 */
class OrderTest {

    @Test
    void shouldCalculateTotalCorrectly() {
        // Given
        var items = List.of(
            new OrderItem("p1", "Producto A", BigDecimal.valueOf(10000), 2),
            new OrderItem("p2", "Producto B", BigDecimal.valueOf(5000), 1)
        );

        // When
        var total = Order.calculateTotal(items);

        // Then
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(25000));
    }

    @Test
    void shouldNotAllowNegativeQuantity() {
        assertThatThrownBy(() ->
            new OrderItem("p1", "Test", BigDecimal.valueOf(10000), -1)
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
```

### Unit Tests — Mappers

```java
/**
 * Verificar que los mappers convierten correctamente entre domain y DTO.
 */
class ProductMapperTest {

    @Test
    void shouldMapDomainProductToResponseDto() {
        // Given
        var product = TestDataFactory.createProduct("p1", "Camiseta", 35000);

        // When
        var dto = ProductMapper.toResponseDto(product);

        // Then
        assertThat(dto.productId()).isEqualTo("p1");
        assertThat(dto.name()).isEqualTo("Camiseta");
        assertThat(dto.price()).isEqualByComparingTo(BigDecimal.valueOf(35000));
    }

    @Test
    void shouldMapRequestDtoToDomainProduct() {
        // Given
        var dto = new CreateProductRequestDto("Camiseta", "Desc", BigDecimal.valueOf(35000), List.of(), "cat1", 50);

        // When
        var product = ProductMapper.toDomain("tenant1", dto);

        // Then
        assertThat(product.tenantId()).isEqualTo("tenant1");
        assertThat(product.name()).isEqualTo("Camiseta");
    }
}
```

### Integration Tests — Controllers

```java
/**
 * Test de integración: request HTTP completo a través del stack.
 * Usa Javalin TestTools para levantar servidor en puerto aleatorio.
 */
class ProductControllerIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldReturnProductListForTenant() {
        // Given
        seedProducts("idoneo", 3);

        // When
        var response = httpClient.request("/api/v1/tenants/idoneo/products", builder -> {
            builder.header("X-Tenant-Id", "idoneo");
        });

        // Then
        assertThat(response.code()).isEqualTo(200);

        var body = parseJson(response.body().string());
        assertThat(body.get("data").size()).isEqualTo(3);
        assertThat(body.get("meta").get("total").asInt()).isEqualTo(3);
    }

    @Test
    void shouldReturn404ForNonExistentProduct() {
        var response = httpClient.request("/api/v1/tenants/idoneo/products/nonexistent", builder -> {
            builder.header("X-Tenant-Id", "idoneo");
        });

        assertThat(response.code()).isEqualTo(404);
    }

    @Test
    void shouldReturn400ForInvalidProductData() {
        var response = httpClient.request("/api/v1/tenants/idoneo/products", builder -> {
            builder.header("X-Tenant-Id", "idoneo")
                   .header("Content-Type", "application/json")
                   .post(jsonBody(Map.of("name", "")));  // nombre vacío
        });

        assertThat(response.code()).isEqualTo(400);
    }
}
```

### Integration Tests — Middleware

```java
/**
 * Verificar que TenantFilter rechaza requests sin X-Tenant-Id.
 */
class TenantFilterIntegrationTest extends IntegrationTestBase {

    @Test
    void shouldReturn400WhenTenantHeaderMissing() {
        var response = httpClient.request("/api/v1/tenants/idoneo/products");

        assertThat(response.code()).isEqualTo(400);
    }

    @Test
    void shouldReturn404WhenTenantNotFound() {
        var response = httpClient.request("/api/v1/tenants/fake/products", builder -> {
            builder.header("X-Tenant-Id", "fake");
        });

        assertThat(response.code()).isEqualTo(404);
    }

    @Test
    void shouldPassWhenTenantIsValid() {
        seedTenant("idoneo");

        var response = httpClient.request("/api/v1/health", builder -> {
            builder.header("X-Tenant-Id", "idoneo");
        });

        assertThat(response.code()).isEqualTo(200);
    }
}
```

### TestDataFactory (Helper)

```java
/**
 * Fábrica centralizada de datos de test.
 * Evita duplicar builders de objetos en cada test.
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    public static Product createProduct(String id, String name, long price) {
        return new Product(
            "test-tenant", id, name, "Descripción de " + name,
            BigDecimal.valueOf(price), null, List.of(), "cat1", "General",
            50, "ACTIVE", Instant.now(), Instant.now()
        );
    }

    public static Product createProductWithStock(int stock) {
        return createProduct("p1", "Producto Test", 10000)
            .withStock(stock);
    }

    public static CreateOrderRequest createValidOrderRequest() {
        return new CreateOrderRequest(
            "test-tenant",
            List.of(new OrderItemRequest("p1", 2)),
            new CustomerInfo("Juan", "juan@test.com", "3001234567"),
            "Bogotá",
            "Calle 123",
            PaymentMethod.WOMPI
        );
    }

    public static CreateOrderRequest createEmptyCartRequest() {
        return new CreateOrderRequest(
            "test-tenant", List.of(),
            new CustomerInfo("Juan", "juan@test.com", "3001234567"),
            "Bogotá", "Calle 123", PaymentMethod.WOMPI
        );
    }
}
```

### Coverage

- **Meta MVP**: 80% en `domain/` y `application/` layers.
- No se exige coverage en `infra/config/`, Guice modules, ni `Main.java`.
- Plugin: **JaCoCo** (gratuito, integrado con Gradle).

```groovy
// build.gradle
plugins {
    id 'jacoco'
}

jacocoTestReport {
    reports {
        html.required = true
        xml.required = true  // para CI
    }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, excludes: [
                '**/infra/config/**',
                '**/Main.class',
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = 'BUNDLE'
            limit {
                minimum = 0.80
            }
        }
    }
}
```

### Comandos Gradle

```bash
./gradlew test                    # Ejecutar todos los tests
./gradlew test --tests "*Unit*"   # Solo unitarios
./gradlew test --tests "*Integration*"  # Solo integración
./gradlew jacocoTestReport        # Generar reporte de coverage
./gradlew jacocoTestCoverageVerification  # Verificar mínimo 80%
```

---

## Seguridad (prácticas sin costo)

### Principios

1. **Nunca confiar en datos del frontend.** Toda validación se hace en backend, incluso si el frontend ya la hizo.
2. **Aislamiento multi-tenant estricto.** Cada query incluye `tenantId`. Nunca se accede a datos de otro tenant.
3. **Mínimo privilegio.** Cada endpoint tiene su nivel de autorización mínimo.
4. **Fallar de forma segura.** En caso de duda, denegar acceso. Nunca exponer detalles internos en errores.

### Validación de Input

Toda entrada del usuario se valida antes de llegar al use case. La validación ocurre en la capa de **infraestructura** (handlers/DTOs):

| Regla                              | Ejemplo                                | Cómo                                            |
| ---------------------------------- | -------------------------------------- | ----------------------------------------------- |
| Strings no vacíos ni solo espacios | `name`, `email`, `address`             | `str.isBlank()` check                           |
| Longitud máxima                    | `name` max 200, `description` max 2000 | Validar en handler                              |
| Email válido                       | `customer.email`                       | Regex básico o `jakarta.mail.InternetAddress`   |
| Teléfono colombiano                | `phone`                                | Regex: `^3[0-9]{9}$`                            |
| IDs válidos                        | `productId`, `tenantId`                | Regex: `^[a-zA-Z0-9-_]+$` (no chars especiales) |
| Precio positivo                    | `price`                                | `BigDecimal.compareTo(ZERO) > 0`                |
| Cantidad en rango                  | `quantity`                             | `1 <= quantity <= 99`                           |
| Paginación                         | `page`, `size`                         | `page >= 0`, `1 <= size <= 100`                 |
| Enum válido                        | `paymentMethod`, `orderStatus`         | Parse con `try/catch` → `BadRequestException`   |

```java
/**
 * Validar datos de entrada en el handler antes de delegar al use case.
 * Nunca pasar datos sin validar al domain layer.
 */
public class InputValidator {

    private static final Pattern SAFE_ID = Pattern.compile("^[a-zA-Z0-9\\-_]{1,64}$");
    private static final Pattern CO_PHONE = Pattern.compile("^3[0-9]{9}$");
    private static final int MAX_NAME_LENGTH = 200;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " es obligatorio");
        }
        return value.strip();
    }

    public static String requireSafeId(String value, String fieldName) {
        requireNonBlank(value, fieldName);
        if (!SAFE_ID.matcher(value).matches()) {
            throw new BadRequestException(fieldName + " contiene caracteres no válidos");
        }
        return value;
    }

    public static String requireValidPhone(String value) {
        requireNonBlank(value, "teléfono");
        if (!CO_PHONE.matcher(value).matches()) {
            throw new BadRequestException("Teléfono inválido: debe ser un celular colombiano");
        }
        return value;
    }

    public static int requirePageSize(int size) {
        if (size < 1 || size > 100) {
            throw new BadRequestException("size debe estar entre 1 y 100");
        }
        return size;
    }
}
```

### Protección contra Inyecciones

- **DynamoDB es inmune a SQL injection** por diseño (usa API, no queries textuales).
- **Nunca concatenar input de usuario** en expresiones DynamoDB. Usar siempre `expressionAttributeValues`:

```java
// ✅ Correcto - parametrizado
var request = QueryRequest.builder()
    .tableName(tableName)
    .keyConditionExpression("PK = :pk AND begins_with(SK, :sk)")
    .expressionAttributeValues(Map.of(
        ":pk", AttributeValue.builder().s("TENANT#" + tenantId).build(),
        ":sk", AttributeValue.builder().s("PRODUCT#").build()
    ))
    .build();

// ❌ Incorrecto - nunca hacer esto
// .keyConditionExpression("PK = 'TENANT#" + tenantId + "'")
```

- **Log injection**: sanitizar valores antes de loguear. No incluir input del usuario directo en `log.info()` sin limitar longitud:

```java
// ✅ Correcto
log.info("Producto creado: id={}, tenant={}", product.productId(), tenantId);

// ❌ Incorrecto - el nombre podría contener chars de control
// log.info("Producto creado: " + userProvidedName);
```

### Aislamiento Multi-Tenant

- **Cada query a DynamoDB DEBE incluir el `tenantId`** como parte de la partition key.
- **El `tenantId` viene del middleware (`TenantFilter`)**, no del body del request ni de query params del usuario.
- **Nunca permitir que un usuario pase su propio `tenantId`** en el body:

```java
// ✅ Correcto - tenantId del contexto de middleware
var tenantId = ctx.attribute("tenantId");
var products = productRepository.findByTenant(tenantId, filter);

// ❌ Incorrecto - tenantId del body del usuario
// var tenantId = requestBody.tenantId();  // ¡El usuario podría poner el de otro tenant!
```

### Autenticación y Autorización

- **Endpoints públicos** (catálogo, config de tenant, health): no requieren auth.
- **Endpoints protegidos** (crear producto, actualizar orden, admin): requieren JWT válido + rol adecuado.
- **AuthFilter** valida JWT de Cognito:
  1. Extrae `Authorization: Bearer <token>` del header.
  2. Valida firma, expiración y audiencia del JWT.
  3. Extrae claims: `sub`, `tenantId`, `role`.
  4. Setea `ctx.attribute("user", userInfo)`.
- **Autorización por rol** en cada handler protegido:

```java
/**
 * Verificar que el usuario tiene el rol requerido.
 */
public static void requireRole(Context ctx, UserRole requiredRole) {
    var user = ctx.attribute("user");
    if (user == null) {
        throw new UnauthorizedException("Autenticación requerida");
    }
    if (!user.hasRole(requiredRole)) {
        throw new ForbiddenException("Permisos insuficientes");
    }
}
```

### Manejo Seguro de Errores

- **Nunca exponer stack traces en respuestas HTTP.** El handler global de excepciones retorna mensajes genéricos para errores 500:

```java
// Errores de dominio: mensaje seguro al usuario
app.exception(NotFoundException.class, (e, ctx) -> {
    ctx.status(404).json(Map.of("error", Map.of(
        "code", e.getCode(),
        "message", e.getMessage()
    )));
});

// Errores inesperados: NUNCA exponer detalles internos
app.exception(Exception.class, (e, ctx) -> {
    log.error("Error inesperado en {} {}: {}", ctx.method(), ctx.path(), e.getMessage(), e);
    ctx.status(500).json(Map.of("error", Map.of(
        "code", "INTERNAL_ERROR",
        "message", "Error interno del servidor"  // genérico, sin detalles
    )));
});
```

### Rate Limiting (F019 — en código, sin costo)

Implementado en `RateLimiterFilter.java` con ventana deslizante y `ConcurrentHashMap`:

- **General (GET)**: 60 requests/minuto por IP.
- **Escritura (POST/PUT/DELETE en `/orders`, `/cart`, `/auth/login`)**: 10 requests/minuto por IP.
- Cleanup automático cada 5 minutos via `ScheduledExecutorService`.
- Response 429 con header `Retry-After: 60`.
- Excluye `/health` y `/webhooks/`.

### Bot Detection (F019 — en código, sin costo)

Implementado en `BotDetectionFilter.java` con scoring de sospecha:

| Señal                        | Puntos |
| ---------------------------- | ------ |
| User-Agent vacío             | +2     |
| User-Agent conocido como bot | +3     |
| Sin Accept-Language          | +1     |
| Sin Referer en POST          | +1     |
| Honeypot field con valor     | +5     |
| Form timing < 2s             | +2     |

- **Score >= 5**: Bloquear (429).
- **Score 3-4**: Permitir, loguear WARN.
- **Score 0-2**: Normal.

### CORS (en código)

```java
/**
 * Configurar CORS para aceptar solo orígenes de nuestros subdominios.
 */
app.before(ctx -> {
    var origin = ctx.header("Origin");
    if (origin != null && isAllowedOrigin(origin)) {
        ctx.header("Access-Control-Allow-Origin", origin);
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Tenant-Id");
        ctx.header("Access-Control-Max-Age", "86400");
    }
});

private boolean isAllowedOrigin(String origin) {
    return origin.equals("http://localhost:3000")
        || origin.matches("https://[a-zA-Z0-9-]+\\.projectx\\.com");
}
```

### Headers de Seguridad en Respuestas (F019)

Implementado en `SecurityHeadersFilter.java` — se registra como `app.after(...)`:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 0` (legacy, desactivado — CSP es la protección correcta)
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`
- `Cache-Control: no-store` solo en rutas sensibles (`/auth/`, `/orders`, `/cart`, `/payments`)

### Logging Seguro

- **NUNCA loguear**: passwords, tokens JWT, números de tarjeta, datos bancarios, secrets.
- **SÍ loguear**: tenantId, userId, operación realizada, errores (sin stack trace al usuario).
- **Formato estructurado**: cada log incluye `tenantId` y `requestId` para trazabilidad.

```java
// ✅ Bien
log.info("Orden creada: orderId={}, tenantId={}, total={}", order.orderId(), tenantId, order.total());

// ❌ MAL - datos sensibles
// log.info("Login exitoso: email={}, password={}", email, password);
// log.info("Pago procesado: tarjeta={}", cardNumber);
```

### Dependencias

- **`./gradlew dependencyUpdates`** — verificar dependencias desactualizadas (plugin [ben-manes/gradle-versions-plugin](https://github.com/ben-manes/gradle-versions-plugin), gratuito).
- **OWASP Dependency-Check** (gratuito) — escanear vulnerabilidades conocidas en dependencias:

```groovy
// build.gradle
plugins {
    id 'org.owasp.dependencycheck' version '9.0.9'
}

dependencyCheck {
    failBuildOnCVSS = 7  // fallar build si hay vulnerabilidad >= 7 (high)
    formats = ['HTML', 'JSON']
}
```

```bash
./gradlew dependencyCheckAnalyze  # Escanear vulnerabilidades
./gradlew dependencyUpdates       # Ver dependencias desactualizadas
```

### Checklist de Seguridad por Feature

Al implementar cada feature, verificar:

- [ ] Input validado en handler (no en domain ni use case).
- [ ] `tenantId` viene del middleware, no del request body.
- [ ] Endpoints protegidos tienen `AuthFilter` + verificación de rol.
- [ ] Errores no exponen información interna.
- [ ] Datos sensibles no se loguean.
- [ ] Nuevas dependencias revisadas con `dependencyCheckAnalyze`.
