# F002 — Tenant Configuration API (Backend)

## Propósito

Implementar la entidad Tenant con toda su configuración, el repositorio DynamoDB y los endpoints para obtener y actualizar la configuración de un tenant.

## Endpoints

| Método | Ruta                               | Descripción                              | Auth                   |
| ------ | ---------------------------------- | ---------------------------------------- | ---------------------- |
| `GET`  | `/api/v1/tenants/:tenantId/config` | Obtener configuración pública del tenant | No                     |
| `PUT`  | `/api/v1/tenants/:tenantId/config` | Actualizar configuración del tenant      | SUPER_ADMIN / MERCHANT |

## Domain Layer

### Entidades

**Tenant** (record)

- `tenantId`: String — ID único (slug)
- `name`: String — Nombre del negocio
- `description`: String — Descripción corta
- `logoUrl`: String — URL del logo
- `bannerUrl`: String — URL del banner
- `colors`: TenantColors — Colores del tema
- `font`: String — Tipografía (Google Fonts)
- `socialMedia`: List<SocialLink> — Redes sociales
- `cities`: List<String> — Ciudades de cobertura
- `whatsapp`: String — Número de WhatsApp (+57...)
- `email`: String — Email de contacto
- `phone`: String — Teléfono
- `address`: String — Dirección física
- `schedule`: String — Horarios
- `bankInfo`: BankInfo — Datos bancarios
- `isActive`: Boolean — Si la tienda está activa
- `createdAt`: Instant
- `updatedAt`: Instant

**TenantColors** (record): `primary`, `secondary`, `accent`, `background`, `text` (todos String hex)

**SocialLink** (record): `platform`, `url`, `isActive`

**BankInfo** (record): `bankName`, `accountType`, `accountNumber`, `accountHolder`, `documentType`, `documentNumber`

### Ports

**TenantRepository**

- `Optional<Tenant> findById(String tenantId)`
- `void save(Tenant tenant)`
- `List<Tenant> findAll()` — Solo para SUPER_ADMIN

### Use Cases

| Interface                   | Input                 | Output   |
| --------------------------- | --------------------- | -------- |
| `GetTenantConfigUseCase`    | `String tenantId`     | `Tenant` |
| `UpdateTenantConfigUseCase` | `UpdateTenantRequest` | `Tenant` |

### Exceptions

| Excepción                 | HTTP | Cuándo               |
| ------------------------- | ---- | -------------------- |
| `TenantNotFoundException` | 404  | Tenant no existe     |
| `TenantInactiveException` | 403  | Tenant está inactivo |

## Application Layer

### GetTenantConfigUseCaseImpl

1. Buscar tenant por ID en el repositorio.
2. Si no existe → `TenantNotFoundException`.
3. Si está inactivo → `TenantInactiveException`.
4. Retornar el tenant.

### UpdateTenantConfigUseCaseImpl

1. Validar que el tenant existe.
2. Validar campos requeridos (name, cities no vacíos).
3. Actualizar campos y setear `updatedAt = Instant.now()`.
4. Guardar en repositorio.
5. Retornar el tenant actualizado.

## Infrastructure Layer

### DynamoDB Access Patterns

| Operación        | PK                               | SK       |
| ---------------- | -------------------------------- | -------- |
| Get config       | `TENANT#<tenantId>`              | `CONFIG` |
| Save config      | `TENANT#<tenantId>`              | `CONFIG` |
| List all tenants | Scan (filtrar por SK = `CONFIG`) | —        |

### DTOs

**TenantResponseDto**: Todos los campos de Tenant mapeados.
**UpdateTenantRequestDto**: Campos editables (name, description, colors, socialMedia, cities, etc.)

## Validaciones

| Campo            | Regla                        | Error                                         |
| ---------------- | ---------------------------- | --------------------------------------------- |
| `name`           | No vacío, max 100 chars      | "El nombre es requerido"                      |
| `cities`         | Al menos 1 ciudad            | "Debe haber al menos una ciudad de cobertura" |
| `colors.primary` | Formato hex válido (#XXXXXX) | "Color primario debe ser formato hex"         |
| `whatsapp`       | Formato +57XXXXXXXXXX        | "Número de WhatsApp inválido"                 |

## Criterios de Aceptación

- [x] `GET /api/v1/tenants/idoneo/config` retorna la config completa.
- [x] `GET /api/v1/tenants/inexistente/config` retorna `404`.
- [x] `PUT /api/v1/tenants/idoneo/config` actualiza la config (endpoint listo, auth en F011).
- [ ] `PUT` sin auth retorna `401`. _(pendiente: se protege en F011 Auth)_
- [x] Validación de campos retorna `400` con mensaje descriptivo.
- [x] Tenant inactivo retorna `403` con mensaje "Tienda no disponible".

## Testing

### Unitarios

- `GetTenantConfigUseCaseImplTest` — tenant existe, no existe, inactivo
- `UpdateTenantConfigUseCaseImplTest` — actualización exitosa, validaciones

### Integración

- `TenantControllerIntegrationTest` — GET/PUT endpoints completos

## Notas

- Se necesita seed de datos iniciales para los 3 tenants (IDONEO, CHICHA, Tech).
- El seed se puede hacer con un script o con un endpoint temporal POST de creación.
- Los datos bancarios solo se retornan en el GET completo. Considerar si crear un endpoint público que excluya bankInfo vs uno protegido que lo incluya.
