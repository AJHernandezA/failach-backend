# F018 — Legal Content API (Backend)

## Propósito

Servir contenido legal (términos, privacidad, devoluciones) configurable por tenant. Si el tenant no tiene contenido custom, retornar template por defecto con placeholders reemplazados.

## Endpoints

| Método | Ruta                                    | Descripción                | Auth                   |
| ------ | --------------------------------------- | -------------------------- | ---------------------- |
| `GET`  | `/api/v1/tenants/:tenantId/legal/:type` | Obtener contenido legal    | No                     |
| `PUT`  | `/api/v1/tenants/:tenantId/legal/:type` | Actualizar contenido legal | SUPER_ADMIN / MERCHANT |

### Legal Types

- `terms` — Términos y condiciones
- `privacy` — Política de privacidad
- `returns` — Política de devoluciones

## Domain Layer

### Entidades

**LegalContent** (record): tenantId, type (String), title (String), content (String — markdown), updatedAt (Instant)

### Ports

**LegalContentRepository**

- `Optional<LegalContent> findByType(String tenantId, String type)`
- `void save(LegalContent content)`

### Use Cases

| Interface                   | Input                          | Output         |
| --------------------------- | ------------------------------ | -------------- |
| `GetLegalContentUseCase`    | `String tenantId, String type` | `LegalContent` |
| `UpdateLegalContentUseCase` | `UpdateLegalContentRequest`    | `LegalContent` |

## Application Layer

### GetLegalContentUseCaseImpl

1. Buscar contenido legal del tenant por tipo.
2. Si existe → retornar.
3. Si no existe → cargar template por defecto desde resources.
4. Reemplazar placeholders: `{{tenantName}}`, `{{tenantEmail}}`, `{{tenantAddress}}`.
5. Retornar contenido con template aplicado.

### Templates por Defecto

Ubicación: `src/main/resources/templates/legal/`

- `default-terms.md`
- `default-privacy.md`
- `default-returns.md`

## DynamoDB Access Patterns

| Operación  | PK            | SK             |
| ---------- | ------------- | -------------- |
| Get legal  | `TENANT#<id>` | `LEGAL#<type>` |
| Save legal | `TENANT#<id>` | `LEGAL#<type>` |

## Response

```json
{
  "data": {
    "type": "terms",
    "title": "Términos y Condiciones",
    "content": "# Términos y Condiciones de IDONEO\n\n...",
    "updatedAt": "2026-03-12T12:00:00Z"
  }
}
```

## Criterios de Aceptación

- [x] `GET /legal/terms` retorna contenido de términos del tenant.
- [x] Si no hay contenido custom, retorna template por defecto con nombre del tenant.
- [x] `PUT /legal/terms` actualiza contenido (auth requerida).
- [x] Tipo inválido retorna `400`.
- [x] Content soporta markdown.

## Notas

- El contenido legal se renderiza como markdown en el frontend.
- Los templates por defecto cubren requisitos mínimos de ley colombiana (Ley 1480 de 2011).
- Se recomienda asesoría legal para el contenido definitivo de cada tenant.
