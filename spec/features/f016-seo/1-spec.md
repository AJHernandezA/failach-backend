# F016 — SEO Data API (Backend)

## Propósito

Proveer datos para que el frontend genere el sitemap.xml dinámicamente: lista de URLs de productos y categorías del tenant.

## Endpoints

| Método | Ruta                                     | Descripción       | Auth |
| ------ | ---------------------------------------- | ----------------- | ---- |
| `GET`  | `/api/v1/tenants/:tenantId/sitemap-data` | URLs para sitemap | No   |

## Domain Layer

### Use Cases

| Interface               | Input             | Output        |
| ----------------------- | ----------------- | ------------- |
| `GetSitemapDataUseCase` | `String tenantId` | `SitemapData` |

### SitemapData (record)

```java
public record SitemapData(
    List<SitemapEntry> entries
) {}

public record SitemapEntry(
    String url,
    String lastModified,
    String changeFrequency,
    double priority
) {}
```

## Application Layer

### GetSitemapDataUseCaseImpl

1. Obtener todas las categorías del tenant.
2. Obtener todos los productos activos del tenant.
3. Generar entradas de sitemap:
   - Homepage: priority 1.0, changeFreq daily
   - Cada categoría: priority 0.8, changeFreq weekly
   - Cada producto: priority 0.6, changeFreq weekly
4. Retornar lista de entradas.

## Response

```json
{
  "data": {
    "entries": [
      {
        "url": "/",
        "lastModified": "2026-03-12",
        "changeFrequency": "daily",
        "priority": 1.0
      },
      {
        "url": "/bebidas",
        "lastModified": "2026-03-10",
        "changeFrequency": "weekly",
        "priority": 0.8
      },
      {
        "url": "/product/prod001",
        "lastModified": "2026-03-11",
        "changeFrequency": "weekly",
        "priority": 0.6
      }
    ]
  }
}
```

## Criterios de Aceptación

- [x] Retorna todas las URLs indexables del tenant.
- [x] Solo incluye productos activos.
- [x] Incluye homepage, categorías y productos.
- [x] Cada entrada tiene lastModified, changeFrequency y priority.
