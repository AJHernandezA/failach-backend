# F021 — Product Gallery (Backend)

## Propósito

Soporte backend para múltiples imágenes por producto. Asegurar que el modelo de producto soporta un array de URLs de imágenes y que los endpoints de CRUD manejan este campo correctamente.

## Cambios en Domain Layer

### Product (modificar)

Verificar/agregar campo:

- `images` (List<String>) — Lista ordenada de URLs de imágenes del producto. La primera es la imagen principal.
- Mantener backward compatibility: si existe `imageUrl` (campo legacy), mapearlo como `images[0]`.

### Validación

- Mínimo 1 imagen por producto.
- Máximo 10 imágenes por producto.
- Cada URL debe ser una URL válida (S3/CloudFront).

## Endpoints Afectados

| Método | Ruta                                              | Cambio                                           |
| ------ | ------------------------------------------------- | ------------------------------------------------ |
| `POST` | `/api/v1/tenants/:tenantId/products`              | Acepta `images: String[]` en el body             |
| `PUT`  | `/api/v1/tenants/:tenantId/products/:id`          | Acepta `images: String[]` en el body             |
| `GET`  | `/api/v1/tenants/:tenantId/products/:id`          | Retorna `images: String[]`                       |
| `GET`  | `/api/v1/tenants/:tenantId/products`              | Cada producto incluye `images: String[]`         |

## DynamoDB Access Patterns

Sin cambios en PK/SK. El campo `images` se almacena como lista (L) de strings en DynamoDB.

| Operación       | PK            | SK             | Cambio                          |
| --------------- | ------------- | -------------- | ------------------------------- |
| Get product     | `TENANT#<id>` | `PRODUCT#<id>` | Incluye atributo `images` (List) |
| Save product    | `TENANT#<id>` | `PRODUCT#<id>` | Guarda `images` como List<S>    |

## Response (ejemplo)

```json
{
  "data": {
    "id": "prod-001",
    "name": "Bowl Energético",
    "images": [
      "https://cdn.projectx.com/idoneo/products/bowl-1.webp",
      "https://cdn.projectx.com/idoneo/products/bowl-2.webp",
      "https://cdn.projectx.com/idoneo/products/bowl-3.webp"
    ],
    "price": 28500,
    "categoryId": "cat-001"
  }
}
```

## Criterios de Aceptación

- [ ] POST producto acepta array de imágenes (1-10).
- [ ] PUT producto permite actualizar array de imágenes.
- [ ] GET producto retorna array `images`.
- [ ] Validación: mínimo 1, máximo 10 imágenes.
- [ ] Backward compatible: productos con `imageUrl` legacy se mapean a `images[0]`.

## Notas

- Si el modelo actual ya usa `images: List<String>`, este feature solo requiere verificar y agregar validación.
- Si usa `imageUrl: String`, se necesita migración: `imageUrl` → `images: [imageUrl]`.
- La subida de imágenes a S3 es parte de un flujo separado (admin panel, Phase 2). Por ahora las URLs se configuran manualmente.
