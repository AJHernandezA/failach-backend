# F041 — Seller Profile & Store Settings (Backend Spec)

## Endpoints de Perfil

### `GET /api/v1/admin/profile`

Obtener perfil del usuario autenticado.

**Auth**: Requerida.

**Response** (`200 OK`):
```json
{
  "sub": "cognito-uuid",
  "email": "juan@negocio.com",
  "name": "Juan Vendedor",
  "phone": "+573001234567",
  "role": "MERCHANT",
  "tenantId": "mi-tienda-online",
  "businessName": "Mi Tienda Online",
  "avatarUrl": null,
  "createdAt": "2026-01-15T10:00:00Z"
}
```

### `PUT /api/v1/admin/profile`

Actualizar perfil (nombre, teléfono, avatar).

**Auth**: Requerida.

**Request body**:
```json
{
  "name": "Juan Vendedor Actualizado",
  "phone": "+573009876543",
  "avatarUrl": "https://cdn.../avatar.jpg"
}
```

### `PUT /api/v1/admin/profile/password`

Cambiar contraseña.

**Auth**: Requerida.

**Request body**:
```json
{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword456!"
}
```

**Implementación**: Llama a Cognito `changePassword()`.

## Endpoints de Configuración de Tienda

### `GET /api/v1/admin/settings`

Obtener configuración completa de la tienda.

**Auth**: Requerida.

**Query param**: `tenantId` (solo SUPER_ADMIN; MERCHANT auto-filtra).

**Response** (`200 OK`):
```json
{
  "tenantId": "idoneo",
  "general": {
    "businessName": "IDONEO",
    "description": "Comida saludable y deliciosa...",
    "category": "Restaurante Saludable",
    "slogan": "Alimenta tu cuerpo, nutre tu alma"
  },
  "visual": {
    "logoUrl": "https://cdn.../logo.png",
    "bannerUrl": "https://cdn.../banner.jpg",
    "primaryColor": "#2563eb",
    "secondaryColor": "#7c3aed",
    "faviconUrl": null
  },
  "contact": {
    "phone": "+573145429669",
    "email": "contacto@idoneo.co",
    "address": "Calle 123, Barranquilla",
    "whatsapp": "+573145429669",
    "schedule": {
      "monday": "8:00 - 20:00",
      "tuesday": "8:00 - 20:00",
      "wednesday": "8:00 - 20:00",
      "thursday": "8:00 - 20:00",
      "friday": "8:00 - 20:00",
      "saturday": "9:00 - 18:00",
      "sunday": "Cerrado"
    }
  },
  "socialMedia": {
    "instagram": "https://instagram.com/idoneo_co",
    "facebook": null,
    "twitter": null,
    "tiktok": null,
    "youtube": null,
    "linkedin": null,
    "pinterest": null,
    "telegram": null
  },
  "coverage": {
    "cities": ["Barranquilla", "Soledad"],
    "shippingCost": 5000,
    "freeShippingThreshold": 40000
  },
  "banking": {
    "bankName": "Bancolombia",
    "accountType": "Ahorros",
    "accountNumber": "****4567",
    "accountHolder": "IDONEO SAS"
  },
  "legal": {
    "terms": "Términos y condiciones de IDONEO...",
    "privacy": "Política de privacidad...",
    "returns": "Política de devoluciones..."
  },
  "advanced": {
    "status": "ACTIVE",
    "timezone": "America/Bogota",
    "currency": "COP"
  }
}
```

### `PUT /api/v1/admin/settings`

Actualizar configuración completa.

### `PUT /api/v1/admin/settings/visual`

Actualizar solo la parte visual.

**Request body**:
```json
{
  "logoUrl": "https://cdn.../new-logo.png",
  "primaryColor": "#16a34a",
  "secondaryColor": "#7c3aed"
}
```

### `PUT /api/v1/admin/settings/contact`

Actualizar contacto, horarios y redes sociales.

### `PUT /api/v1/admin/settings/coverage`

Actualizar ciudades de cobertura y envío.

### `PUT /api/v1/admin/settings/banking`

Actualizar datos bancarios.

**Request body**:
```json
{
  "bankName": "Bancolombia",
  "accountType": "Ahorros",
  "accountNumber": "12345674567",
  "accountHolder": "IDONEO SAS",
  "documentType": "NIT",
  "documentNumber": "900123456-7"
}
```

**Seguridad**: Los datos bancarios se almacenan completos en DynamoDB pero al retornar al FE se enmascaran (solo últimos 4 dígitos de la cuenta).

### `PUT /api/v1/admin/settings/legal`

Actualizar textos legales.

## Endpoints de Gestión de Tiendas (SUPER_ADMIN)

### `GET /api/v1/admin/stores`

Listar todas las tiendas.

**Auth**: Requerida (SUPER_ADMIN only).

**Response** (`200 OK`):
```json
[
  {
    "tenantId": "idoneo",
    "businessName": "IDONEO",
    "logoUrl": "https://cdn.../logo.png",
    "category": "Restaurante Saludable",
    "status": "ACTIVE",
    "ownerEmail": "admin@idoneo.co",
    "totalProducts": 15,
    "totalOrders": 45,
    "createdAt": "2026-01-01T00:00:00Z"
  }
]
```

### `PUT /api/v1/admin/stores/:tenantId/status`

Cambiar estado de una tienda.

**Auth**: Requerida (SUPER_ADMIN only).

**Request body**:
```json
{
  "status": "SUSPENDED",
  "reason": "Violación de términos"
}
```

### `POST /api/v1/admin/stores/:tenantId/approve`

Aprobar tienda pendiente.

**Auth**: Requerida (SUPER_ADMIN only).

**Implementación**:
1. Cambiar status del tenant de PENDING a ACTIVE.
2. Cambiar status del usuario owner de PENDING a ACTIVE.
3. Enviar email de notificación al vendedor (F043).

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   ├── AdminProfileController.java
│   ├── AdminSettingsController.java
│   └── AdminStoreController.java
├── persistence/
│   ├── DynamoUserRepository.java
│   └── DynamoSettingsRepository.java
application/
├── usecase/
│   ├── GetProfileUseCase.java
│   ├── UpdateProfileUseCase.java
│   ├── ChangePasswordUseCase.java
│   ├── GetStoreSettingsUseCase.java
│   ├── UpdateStoreSettingsUseCase.java
│   ├── ListStoresUseCase.java
│   ├── UpdateStoreStatusUseCase.java
│   └── ApproveStoreUseCase.java
domain/
├── model/
│   ├── StoreSettings.java
│   ├── BankingInfo.java
│   └── StoreStatus.java      // ACTIVE, PENDING, SUSPENDED
├── port/
│   ├── UserRepository.java
│   ├── SettingsRepository.java
│   └── AuthService.java      // Para cambio de contraseña
```

## DynamoDB

Los datos de settings del tenant ya están en `TENANT#<tenantId> | CONFIG`. Se extiende con:

| PK | SK | Datos |
|----|-----|-------|
| `TENANT#<tenantId>` | `CONFIG` | Todos los campos de settings (ya existe, se extiende) |
| `TENANT#<tenantId>` | `BANKING` | bankName, accountType, accountNumber, accountHolder, documentType, documentNumber |

Los datos bancarios se almacenan en un SK separado para mayor control de acceso.

## Validaciones

| Campo | Regla |
|-------|-------|
| Colores | Formato hex válido (#RRGGBB) |
| URLs redes sociales | URL válida o vacío |
| Teléfono | Formato colombiano (+57...) |
| Horarios | Formato "HH:mm - HH:mm" o "Cerrado" |
| Datos bancarios | Todos requeridos si se actualizan |

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- Los cambios de configuración se reflejan inmediatamente en la tienda pública (los endpoints públicos leen la misma config).
- Los datos bancarios se enmascaran al retornarlos al FE — solo el SUPER_ADMIN puede ver los datos completos.
- Al cambiar el logo o banner, las URLs antiguas en S3 no se borran automáticamente (considerar cleanup job en el futuro).
- El cambio de contraseña pasa por Cognito — el backend actúa como proxy.
- Al suspender una tienda, su subdominio muestra un mensaje "Tienda temporalmente no disponible".
