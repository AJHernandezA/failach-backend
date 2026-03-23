# F019 — Security & Anti-Bot (Backend)

## Propósito

Implementar middlewares de seguridad en el backend que protejan contra bots, abuso automatizado y ataques comunes. Costo $0, todo en código Java puro con estructuras en memoria.

## Middlewares

| Middleware       | Clase                   | Descripción                                              |
| ---------------- | ----------------------- | -------------------------------------------------------- |
| Rate Limiter     | `RateLimiterFilter`     | Limita requests por IP con ventana deslizante            |
| Bot Detection    | `BotDetectionFilter`    | Scoring de sospecha basado en headers, timing y honeypot |
| Security Headers | `SecurityHeadersFilter` | Inyecta headers de seguridad en cada response            |

## RateLimiterFilter

- Usa `ConcurrentHashMap<String, List<Long>>` para registrar timestamps de requests por IP.
- Dos niveles de límite:
  - **General** (GET): 60 requests/minuto por IP.
  - **Escritura** (POST/PUT/DELETE a rutas críticas): 10 requests/minuto por IP.
- Rutas críticas de escritura: `/orders`, `/cart`, `/auth/login`.
- Cleanup automático cada 5 minutos via `ScheduledExecutorService`.
- Response 429 con body: `{ "error": { "code": "RATE_LIMIT_EXCEEDED", "message": "Demasiados requests. Intente de nuevo en un momento." } }`.
- Header `Retry-After: 60` en respuestas 429.

## BotDetectionFilter

- Calcula score de sospecha por request:

| Señal                         | Puntos |
| ----------------------------- | ------ |
| User-Agent vacío              | +2     |
| User-Agent conocido como bot  | +3     |
| Sin header Accept-Language    | +1     |
| Sin header Referer (en POST)  | +1     |
| Honeypot field `hp` con valor | +5     |
| Form timing `ft` < 2000ms     | +2     |

- **Score >= 5**: Bloquear con 429.
- **Score 3-4**: Permitir pero loguear como `WARN`.
- **Score 0-2**: Normal.
- User-Agents bot conocidos: `bot`, `crawler`, `spider`, `scraper`, `curl`, `wget`, `python-requests`, `httpclient`, `java/`, `go-http`.
- Se aplica SOLO a rutas de la API (`/api/v1/**`), no a health check ni webhooks.

## SecurityHeadersFilter

- Se registra como `app.after(...)` para inyectar headers en CADA response:

```java
ctx.header("X-Content-Type-Options", "nosniff");
ctx.header("X-Frame-Options", "DENY");
ctx.header("X-XSS-Protection", "0");
ctx.header("Referrer-Policy", "strict-origin-when-cross-origin");
ctx.header("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
ctx.header("Cache-Control", "no-store, no-cache, must-revalidate");
ctx.header("Pragma", "no-cache");
```

- Nota: `X-XSS-Protection: 0` porque el header legacy puede causar vulnerabilidades en browsers modernos. CSP es la protección correcta.
- `Cache-Control: no-store` solo en endpoints con datos sensibles (se puede override por controller).

## Registro en Main.java

```java
// Orden de middlewares (before):
// 1. SecurityHeadersFilter (after — en cada response)
// 2. RateLimiterFilter (before — primero que todo)
// 3. TenantFilter (before — después del rate limiter)
// 4. BotDetectionFilter (before — después del tenant, antes de controllers)
// 5. AuthFilter (before — en rutas protegidas)
```

## Endpoints

No agrega endpoints nuevos. Solo middlewares.

## Modelo de Datos

No agrega entidades ni tablas. Solo estructuras en memoria (`ConcurrentHashMap`).

## Criterios de Aceptación

- [x] IP que excede 60 GET/min recibe 429 con `Retry-After: 60`.
- [x] IP que excede 10 POST/min en `/orders` recibe 429.
- [x] Request sin User-Agent + honeypot lleno = bloqueado (score 7).
- [x] Request normal de browser pasa sin problemas (score 0-1).
- [x] Security headers presentes en toda response (verificar con curl).
- [x] Rate limiter limpia entries viejas automáticamente (no memory leak).
- [x] Health check y webhooks están excluidos del rate limiter y bot detector.
- [x] Logs WARN para requests con score 3-4.
- [x] Logs ERROR para requests bloqueados (score >= 5).

## Dependencias

- **Features previas**: F001 (Main.java, middleware pattern).
- **Librerías**: Ninguna adicional (todo con java.util.concurrent).

## Notas

- Para producción multi-instancia, considerar migrar el rate limiter a Redis (ElastiCache) o AWS WAF.
- El `ScheduledExecutorService` del cleanup se cierra con un shutdown hook.
- Body size limit se configura en Javalin: `config.http.maxRequestSize = 1_048_576L` (1MB).
