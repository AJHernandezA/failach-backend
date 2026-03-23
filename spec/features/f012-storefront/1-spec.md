# F012 — Storefront API (Backend)

## Propósito

Endpoint agregador que retorna toda la data necesaria para renderizar la homepage en un solo request: config del tenant, categorías y productos destacados.

## Endpoints

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/tenants/:tenantId/storefront` | Data completa para homepage | No |

## Domain Layer

### Use Cases

| Interface | Input | Output |
|-----------|-------|--------|
| `GetStorefrontDataUseCase` | `String tenantId` | `StorefrontData` |

### StorefrontData (record)
```java
public record StorefrontData(
    Tenant tenant,
    List<Category> categories,
    List<Product> featuredProducts
) {}
```

## Application Layer

### GetStorefrontDataUseCaseImpl
1. Obtener config del tenant (GetTenantConfigUseCase).
2. Obtener categorías del tenant (ListCategoriesUseCase).
3. Obtener 8 productos destacados (los más recientes activos, o los marcados como featured).
4. Combinar en StorefrontData y retornar.

## Response

```json
{
  "data": {
    "tenant": { ... },
    "categories": [ ... ],
    "featuredProducts": [ ... ]
  }
}
```

## Criterios de Aceptación

- [ ] Un solo request retorna toda la data de la homepage.
- [ ] Incluye config del tenant, categorías y 8 productos.
- [ ] Tenant inexistente retorna `404`.
- [ ] Solo productos activos con stock > 0 se incluyen.

## Notas

- Este endpoint existe para reducir round trips del FE. En vez de 3 requests, hace 1.
- Los datos se pueden cachear en el FE con ISR (60 segundos).
