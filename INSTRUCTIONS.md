# Backend — Guía Completa de Desarrollo Local

## Requisitos

- **Java 21+** (verificar con `java -version`)
- **Gradle 8+** (incluido via `gradlew.bat`, no necesitas instalarlo)

## Inicio Rápido

```bash
# 1. Ir al directorio del backend
cd project-x/backend

# 2. Compilar el proyecto
.\gradlew.bat build -x test

# 3. Ejecutar el servidor
.\gradlew.bat run
```

El servidor arranca en **http://localhost:7070**.

## Verificar que Funciona

```bash
# Health check (no requiere headers)
curl http://localhost:7070/api/v1/health
# Respuesta: {"status":"UP","timestamp":"..."}

# Obtener config del tenant "idoneo" (requiere X-Tenant-Id)
curl -H "X-Tenant-Id: idoneo" http://localhost:7070/api/v1/tenants/idoneo/config

# Listar productos del tenant "idoneo"
curl -H "X-Tenant-Id: idoneo" http://localhost:7070/api/v1/tenants/idoneo/products

# Listar categorías
curl -H "X-Tenant-Id: idoneo" http://localhost:7070/api/v1/tenants/idoneo/categories

# Obtener data de homepage (storefront)
curl -H "X-Tenant-Id: idoneo" http://localhost:7070/api/v1/tenants/idoneo/storefront
```

## Swagger UI (Documentación Interactiva)

Una vez el backend esté corriendo:

- **Swagger UI**: http://localhost:7070/swagger
- **OpenAPI YAML**: http://localhost:7070/openapi.yaml

Desde Swagger UI puedes probar **todos** los endpoints directamente desde el navegador.

## Datos de Prueba (Seed)

Al arrancar en modo desarrollo (sin DynamoDB configurado), el backend usa repositorios **in-memory** y carga datos seed automáticamente:

### Tenants disponibles

| tenantId | Nombre     | Descripción                      |
| -------- | ---------- | -------------------------------- |
| `idoneo` | IDONEO     | Productos naturales y saludables |
| `chicha` | CHICHA     | Accesorios y moda artesanal      |
| `tech`   | Tech Store | Tecnología y electrónicos        |

### Productos de ejemplo (tenant `idoneo`)

| ID            | Nombre                  | Precio  | Categoría        |
| ------------- | ----------------------- | ------- | ---------------- |
| prod-idoneo-1 | Granola Artesanal       | $18,500 | Alimentos        |
| prod-idoneo-2 | Miel de Abejas Orgánica | $25,000 | Alimentos        |
| prod-idoneo-3 | Jugo Verde Detox        | $12,000 | Bebidas          |
| prod-idoneo-4 | Kombucha Artesanal      | $14,000 | Bebidas          |
| prod-idoneo-5 | Jabón de Avena          | $8,500  | Cuidado Personal |
| prod-idoneo-6 | Aceite de Coco Virgen   | $22,000 | Cuidado Personal |

Cada tenant tiene sus propios productos y categorías. Ver `ProductSeeder.java` y `TenantSeeder.java`.

## Configuración (application.properties)

El archivo está en `src/main/resources/application.properties`:

| Propiedad               | Default                 | Descripción                                      |
| ----------------------- | ----------------------- | ------------------------------------------------ |
| `server.port`           | `7070`                  | Puerto del servidor                              |
| `cors.allowedOrigins`   | `http://localhost:3000` | Orígenes CORS (separados por coma)               |
| `auth.mock`             | `true`                  | Usar mock de autenticación (no necesita Cognito) |
| `aws.region`            | `us-east-1`             | Región AWS                                       |
| `aws.dynamodb.endpoint` | (vacío)                 | Si está vacío, usa repos in-memory               |
| `email.enabled`         | `false`                 | Si es false, los emails se loguean por consola   |

Puedes sobrescribir cualquier valor con **variables de entorno** en formato UPPER_SNAKE_CASE:

```bash
set SERVER_PORT=8080
set AUTH_MOCK=true
.\gradlew.bat run
```

## Autenticación Mock (Desarrollo)

Con `auth.mock=true`, los endpoints protegidos aceptan headers de mock:

```bash
# Crear un producto como MERCHANT del tenant "idoneo"
curl -X POST http://localhost:7070/api/v1/tenants/idoneo/products \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: idoneo" \
  -H "X-Mock-Role: MERCHANT" \
  -H "X-Mock-Email: admin@idoneo.com" \
  -H "X-Mock-TenantId: idoneo" \
  -d '{"name":"Nuevo Producto","price":15000,"categoryId":"cat-idoneo-1","stock":10}'

# Listar órdenes como admin
curl http://localhost:7070/api/v1/tenants/idoneo/orders \
  -H "X-Tenant-Id: idoneo" \
  -H "X-Mock-Role: SUPER_ADMIN" \
  -H "X-Mock-Email: admin@projectx.com" \
  -H "X-Mock-TenantId: idoneo"
```

### Roles disponibles

- `SUPER_ADMIN` — Acceso total a todos los tenants
- `MERCHANT` — Acceso a su propio tenant
- `CUSTOMER` — Cliente (acceso básico)

## Flujo Completo: Crear Orden con Carrito

```bash
TENANT="idoneo"
BASE="http://localhost:7070/api/v1/tenants/$TENANT"
HEADERS="-H 'X-Tenant-Id: $TENANT' -H 'Content-Type: application/json'"

# 1. Agregar producto al carrito (guarda cookie sessionId)
curl -c cookies.txt -X POST "$BASE/cart/items" \
  -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -d '{"productId":"prod-idoneo-1","quantity":2}'

# 2. Ver carrito
curl -b cookies.txt "$BASE/cart" -H "X-Tenant-Id: $TENANT"

# 3. Crear orden desde el carrito
curl -b cookies.txt -X POST "$BASE/orders" \
  -H "X-Tenant-Id: $TENANT" -H "Content-Type: application/json" \
  -d '{
    "customerName": "Juan Pérez",
    "customerEmail": "juan@test.com",
    "customerPhone": "+573001234567",
    "shippingAddress": "Calle 123 #45-67",
    "city": "Bogotá",
    "paymentMethod": "BANK_TRANSFER",
    "deliveryMethod": "SHIPPING"
  }'

# 4. Consultar orden por código (reemplazar ORDER_CODE con el orderCode de la respuesta)
curl "$BASE/orders/ORDER_CODE" -H "X-Tenant-Id: $TENANT"
```

## Tests

```bash
# Ejecutar todos los tests
.\gradlew.bat test

# Ejecutar un test específico
.\gradlew.bat test --tests "com.projectx.backend.application.usecases.*"
```

## Estructura del Proyecto

```
src/main/java/com/projectx/backend/
├── Main.java                          → Punto de entrada
├── application/
│   ├── dto/                           → Request/Response DTOs
│   └── usecases/                      → Implementación de use cases
├── domain/
│   ├── constants/                     → ApiConstants
│   ├── exceptions/                    → Excepciones de dominio
│   ├── models/                        → Modelos de dominio (records)
│   └── ports/
│       ├── in/                        → Interfaces de use cases
│       └── out/                       → Interfaces de repositorios
└── infra/
    ├── adapters/
    │   ├── in/controller/             → Controllers (HTTP endpoints)
    │   └── out/
    │       ├── email/                 → SES / Log email
    │       ├── payment/               → Wompi integration
    │       └── persistence/           → InMemory / DynamoDB repos
    ├── config/                        → Guice modules, AppConfig
    ├── middleware/                     → Filters (Auth, Tenant, CORS, Security)
    └── seed/                          → Datos iniciales de desarrollo
```

## Endpoints Completos

### Públicos (no requieren auth)

| Método | Endpoint                                                       | Descripción         |
| ------ | -------------------------------------------------------------- | ------------------- |
| GET    | `/api/v1/health`                                               | Health check        |
| GET    | `/api/v1/tenants/{tenantId}/config`                            | Config del tenant   |
| GET    | `/api/v1/tenants/{tenantId}/products`                          | Listar productos    |
| GET    | `/api/v1/tenants/{tenantId}/products/{productId}`              | Detalle producto    |
| GET    | `/api/v1/tenants/{tenantId}/products/search`                   | Buscar productos    |
| GET    | `/api/v1/tenants/{tenantId}/categories`                        | Listar categorías   |
| GET    | `/api/v1/tenants/{tenantId}/cart`                              | Obtener carrito     |
| POST   | `/api/v1/tenants/{tenantId}/cart/items`                        | Agregar al carrito  |
| PUT    | `/api/v1/tenants/{tenantId}/cart/items/{productId}`            | Actualizar cantidad |
| DELETE | `/api/v1/tenants/{tenantId}/cart/items/{productId}`            | Quitar del carrito  |
| DELETE | `/api/v1/tenants/{tenantId}/cart`                              | Vaciar carrito      |
| POST   | `/api/v1/tenants/{tenantId}/orders`                            | Crear orden         |
| GET    | `/api/v1/tenants/{tenantId}/orders/{orderCode}`                | Tracking de orden   |
| GET    | `/api/v1/tenants/{tenantId}/orders/{orderCode}/payment-status` | Estado de pago      |
| POST   | `/api/v1/tenants/{tenantId}/payments/wompi/init`               | Iniciar pago Wompi  |
| POST   | `/api/v1/webhooks/wompi`                                       | Webhook Wompi       |
| GET    | `/api/v1/tenants/{tenantId}/storefront`                        | Homepage data       |
| GET    | `/api/v1/tenants/{tenantId}/legal/{type}`                      | Contenido legal     |
| GET    | `/api/v1/tenants/{tenantId}/sitemap-data`                      | Datos sitemap       |

### Protegidos (requieren auth mock o JWT)

| Método | Endpoint                                                        | Roles                 | Descripción           |
| ------ | --------------------------------------------------------------- | --------------------- | --------------------- |
| PUT    | `/api/v1/tenants/{tenantId}/config`                             | SUPER_ADMIN, MERCHANT | Actualizar config     |
| POST   | `/api/v1/tenants/{tenantId}/products`                           | SUPER_ADMIN, MERCHANT | Crear producto        |
| PUT    | `/api/v1/tenants/{tenantId}/products/{productId}`               | SUPER_ADMIN, MERCHANT | Actualizar producto   |
| DELETE | `/api/v1/tenants/{tenantId}/products/{productId}`               | SUPER_ADMIN, MERCHANT | Eliminar producto     |
| POST   | `/api/v1/tenants/{tenantId}/categories`                         | SUPER_ADMIN, MERCHANT | Crear categoría       |
| GET    | `/api/v1/tenants/{tenantId}/orders`                             | Autenticado           | Listar órdenes        |
| PUT    | `/api/v1/tenants/{tenantId}/orders/{orderCode}/status`          | Autenticado           | Cambiar estado        |
| PUT    | `/api/v1/tenants/{tenantId}/orders/{orderCode}/confirm-payment` | Autenticado           | Confirmar pago manual |
| PUT    | `/api/v1/tenants/{tenantId}/orders/{orderCode}/cancel`          | Autenticado           | Cancelar orden        |
| PUT    | `/api/v1/tenants/{tenantId}/legal/{type}`                       | Autenticado           | Actualizar legal      |
| GET    | `/api/v1/auth/me`                                               | Autenticado           | Obtener usuario       |

## Regla Multi-Tenant Importante

**Todos los cambios en la tienda principal se propagan a todas las tiendas tenant.** La plataforma es multi-tenant y todos los tenants comparten el mismo codebase. Cualquier feature, fix o mejora hecha para la tienda principal debe funcionar idénticamente para todos los tenants. Los endpoints, lógica de negocio y middleware deben ser agnósticos al tenant y basarse únicamente en el `tenantId` extraído de la ruta o header `X-Tenant-Id`. Siempre verificar que las CORS origins incluyan los subdominios de los nuevos tenants.
