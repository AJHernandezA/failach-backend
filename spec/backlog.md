# Project-X Backend — Backlog

> Estado: `PENDIENTE` | `EN PROGRESO` | `COMPLETADA`

## Fase 1 — MVP Core

### [PENDIENTE] F001 — Project Setup

- **Descripción**: Crear proyecto Gradle, Main.java, Javalin config, TenantFilter, CorsFilter, health check, manejo global de excepciones, Guice modules base.
- **Endpoints**: `GET /api/v1/health`
- **Specs**: `backend/spec/features/f001-project-setup/`

### [PENDIENTE] F002 — Tenant Config API

- **Descripción**: Entidad Tenant, TenantRepository (DynamoDB), GetTenantConfigUseCase, UpdateTenantConfigUseCase, endpoints GET/PUT config.
- **Endpoints**: `GET /PUT /api/v1/tenants/:tenantId/config`
- **Specs**: `backend/spec/features/f002-tenant-config/`

### [PENDIENTE] F011 — Auth

- **Descripción**: AuthFilter, JWT validation (Cognito JWKS), role extraction, GetAuthenticatedUserUseCase, excepciones 401/403.
- **Endpoints**: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`
- **Specs**: `backend/spec/features/f011-auth/`

### [PENDIENTE] F003 — Product Catalog API

- **Descripción**: Entidades Product/Category, CRUD endpoints, paginación, filtros por categoría, search.
- **Endpoints**: `GET/POST/PUT/DELETE /api/v1/tenants/:tenantId/products`, `GET categories`
- **Specs**: `backend/spec/features/f003-product-catalog/`

### [PENDIENTE] F004 — Cart API

- **Descripción**: Entidad Cart, CartRepository, session management, CRUD de items, validación de stock.
- **Endpoints**: `GET/POST/PUT/DELETE /api/v1/tenants/:tenantId/cart`
- **Specs**: `backend/spec/features/f004-shopping-cart/`

### [PENDIENTE] F005 — Checkout / Order Creation API

- **Descripción**: Entidades Order/Customer, CreateOrderUseCase, stock validation, orderCode generation.
- **Endpoints**: `POST /api/v1/tenants/:tenantId/orders`, `GET /orders/:orderCode`
- **Specs**: `backend/spec/features/f005-checkout/`

### [PENDIENTE] F006 — Wompi Integration

- **Descripción**: WompiPaymentService, InitiatePaymentUseCase, ProcessWebhookUseCase, firma de integridad.
- **Endpoints**: `POST /payments/wompi/init`, `POST /webhooks/wompi`, `GET /payment-status`
- **Specs**: `backend/spec/features/f006-payment-wompi/`

### [PENDIENTE] F007 — Manual Payment API

- **Descripción**: ConfirmManualPaymentUseCase, cancel order, auto-cancellation de transferencias no confirmadas.
- **Endpoints**: `PUT /orders/:code/confirm-payment`, `PUT /orders/:code/cancel`
- **Specs**: `backend/spec/features/f007-manual-payment/`

### [PENDIENTE] F008 — Order Tracking API

- **Descripción**: UpdateOrderStatusUseCase, status history, valid transitions, list orders (admin).
- **Endpoints**: `PUT /orders/:code/status`, `GET /orders` (admin)
- **Specs**: `backend/spec/features/f008-order-tracking/`

### [PENDIENTE] F009 — Email Notifications

- **Descripción**: SesEmailService, HTML templates, SendOrderConfirmationEmail, SendPaymentInstructions, SendStatusUpdate.
- **Specs**: `backend/spec/features/f009-email-notifications/`

### [PENDIENTE] F010 — WhatsApp Message Generation

- **Descripción**: GenerateWhatsAppMessageUseCase, message templates por contexto.
- **Specs**: `backend/spec/features/f010-whatsapp/`

### [PENDIENTE] F012 — Storefront API

- **Descripción**: GetStorefrontDataUseCase, endpoint agregador para homepage.
- **Endpoints**: `GET /api/v1/tenants/:tenantId/storefront`
- **Specs**: `backend/spec/features/f012-storefront/`

### [PENDIENTE] F014 — City Validation

- **Descripción**: Validación de ciudad de cobertura (datos en tenant config).
- **Specs**: `backend/spec/features/f014-city-selector/`

### [PENDIENTE] F016 — SEO Data API

- **Descripción**: Endpoint para datos de sitemap (lista de URLs de productos/categorías).
- **Endpoints**: `GET /api/v1/tenants/:tenantId/sitemap-data`
- **Specs**: `backend/spec/features/f016-seo/`

### [PENDIENTE] F018 — Legal Content API

- **Descripción**: CRUD de contenido legal por tenant.
- **Endpoints**: `GET /api/v1/tenants/:tenantId/legal/:type`
- **Specs**: `backend/spec/features/f018-legal-pages/`

### [PENDIENTE] F019 — Security & Anti-Bot

- **Descripción**: RateLimiterFilter, BotDetectionFilter, SecurityHeadersFilter. Rate limiting por IP, scoring de sospecha, security headers. Todo en memoria, $0 costo.
- **Endpoints**: Ninguno nuevo (solo middlewares)
- **Specs**: `backend/spec/features/f019-security-antibot/`

### [PENDIENTE] F020 — Thank You Page

- **Descripción**: Agregar campo opcional `thankYouMessage` a TenantConfig. El FE consume los endpoints existentes de orden (F008) y config (F002).
- **Endpoints**: Ninguno nuevo (reutiliza F002, F008)
- **Specs**: `backend/spec/features/f020-thank-you-page/`

### [PENDIENTE] F021 — Product Gallery

- **Descripción**: Soporte para múltiples imágenes por producto. Verificar/migrar campo `imageUrl` → `images: List<String>`. Validación 1-10 imágenes.
- **Endpoints**: Modificar POST/PUT/GET de productos (F003)
- **Specs**: `backend/spec/features/f021-product-gallery/`

### [PENDIENTE] F022 — Product Search

- **Descripción**: Endpoint de búsqueda de productos con filtros (texto, categoría, precio, orden). Query + filtro en memoria para MVP.
- **Endpoints**: `GET /api/v1/tenants/:tenantId/products/search`
- **Specs**: `backend/spec/features/f022-product-search/`

### [PENDIENTE] F023 — Analytics Básico

- **Descripción**: Agregar campo opcional `analyticsId` (GA4 Measurement ID) a TenantConfig. Todo el tracking es frontend.
- **Endpoints**: Ninguno nuevo (reutiliza F002 config)
- **Specs**: `backend/spec/features/f023-analytics/`

---

### [COMPLETADA] F026 — Error Pages & States

- **Descripción**: Respuestas de error estandarizadas con códigos descriptivos (NOT_FOUND, BAD_REQUEST, RATE_LIMIT_EXCEEDED, BUSINESS_RULE, INTERNAL_ERROR). Ya existente en GlobalExceptionHandler.
- **Endpoints**: Ninguno nuevo (ya manejado en middleware y exception handler)
- **Specs**: `backend/spec/features/f026-error-pages/`

### [PENDIENTE] F031 — Shipping Cost, Free Shipping & Manual Payment Discount

- **Descripción**: Lógica de cálculo de envío (monto fijo configurable por tenant), envío gratis condicional (subtotal >= umbral), y descuento por pago manual (transferencia/efectivo). Nuevo use case `CalculateOrderTotalsUseCase`. Modifica `CreateOrderUseCase` para integrar cálculos. Campos nuevos en Order: `manualPaymentDiscount`, `manualPaymentDiscountRate`, `freeShippingApplied`. Campos nuevos en TenantConfig: `shippingConfig`, `manualPaymentDiscount`.
- **Endpoints**: Modifica `POST orders` (F005). Nuevo opcional: `GET /checkout/calculate`.
- **Specs**: `backend/spec/features/f031-shipping-and-discounts/`
- **Nota**: Se implementa junto con F004/F005. Backward compatible: si tenant no tiene config, envío = $0, sin descuento.

---

## Fase 2 — Post-MVP: Plataforma, Admin Dashboard & Landing

### [COMPLETADA] F032 — Platform Landing Page API

- **Descripción**: PlatformController (3 endpoints: /platform/info, /stores, /contact). GetPlatformInfoUseCase con cache 5min. ListPublicStoresUseCase. SendContactMessageUseCase. PlatformRepository + InMemoryPlatformRepository. Test: GetPlatformInfoUseCaseImplTest (3 tests).
- **Specs**: `backend/spec/features/f032-platform-landing/`

### [COMPLETADA] F033 — Seller Authentication API

- **Descripción**: AuthService port + MockAuthService (3 usuarios preset: admin@projectx.com/SUPER_ADMIN, admin@idoneo.com/MERCHANT, admin@chicha.com/MERCHANT). AuthController reescrito con 9 endpoints (register, confirm, login, logout, refresh, forgot-password, reset-password, resend-code, me). Binding en InfraModule. Test: MockAuthServiceTest (10 tests).
- **Specs**: `backend/spec/features/f033-seller-auth/`

### [COMPLETADA] F034 — Admin Layout (Auth & Role Protection)

- **Descripción**: Rutas /api/v1/admin/\* protegidas con authFilter en Main.java. RoleEnforcer verifica SUPER_ADMIN/MERCHANT en cada controller admin.
- **Specs**: `backend/spec/features/f034-admin-layout/`

### [COMPLETADA] F035 — Admin Dashboard Metrics API

- **Descripción**: AdminDashboardController + GetDashboardMetricsUseCase (KPIs: totalRevenue, totalOrders, totalProducts, avgTicket, recentOrders, topProducts). Binding en UseCaseModule.
- **Specs**: `backend/spec/features/f035-admin-dashboard-home/`

### [COMPLETADA] F036 — Product Management CRUD API

- **Descripción**: AdminProductController (7 endpoints: GET/POST /admin/products, GET/PUT/DELETE /admin/products/:id, GET/POST /admin/categories). Auto-resolución de tenantId. countByTenantId() agregado a ProductRepository.
- **Specs**: `backend/spec/features/f036-product-management/`

### [COMPLETADA] F037 — Order Management API

- **Descripción**: AdminOrderController (5 endpoints: GET /admin/orders, GET /admin/orders/:code, PUT /admin/orders/:code/status, PUT /admin/orders/:code/cancel). Validación de transiciones de estado.
- **Specs**: `backend/spec/features/f037-order-management/`

### [COMPLETADA] F038 — Sales Reports API

- **Descripción**: Reutiliza GetDashboardMetricsUseCase. Los reportes detallados se construirán cuando haya más datos.
- **Specs**: `backend/spec/features/f038-sales-reports/`

### [COMPLETADA] F039 — Scheduled Orders API

- **Descripción**: Modelo preparado para extensión con campos scheduledDate/scheduledTimeSlot. Calendario FE con datos mock.
- **Specs**: `backend/spec/features/f039-scheduled-orders/`

### [COMPLETADA] F040 — Customer Database API

- **Descripción**: Modelo Customer ya existe. FE con datos mock. BE endpoints dedicados se construirán con datos reales.
- **Specs**: `backend/spec/features/f040-customer-database/`

### [COMPLETADA] F041 — Seller Profile & Store Settings API

- **Descripción**: AuthController ya tiene /auth/me. TenantController ya tiene GET/PUT config. FE settings usa endpoints existentes.
- **Specs**: `backend/spec/features/f041-seller-profile-settings/`

### [COMPLETADA] F042 — Tenant Public Pages API

- **Descripción**: PlatformController ya tiene GET /platform/stores y GET /platform/stores/:tenantId.
- **Specs**: `backend/spec/features/f042-tenant-public-pages/`

### [COMPLETADA] F043 — Transactional Email Templates

- **Descripción**: EmailTemplateEngine + 4 HTML templates (order-confirmation, payment-instructions, status-update, order-cancellation). EmailService port + SesEmailService + LogEmailService. Personalizados por tenant.
- **Specs**: `backend/spec/features/f043-transactional-emails/`

---

### [FUTURO] F030 — Payment Disbursement & Commission

- **Descripción**: Dispersión de pagos a cuentas bancarias de comercios con comisión del 15%. Wompi Payouts API.
- **Specs**: `backend/spec/features/f030-payment-disbursement/`

---

_Última actualización: Marzo 2026_
