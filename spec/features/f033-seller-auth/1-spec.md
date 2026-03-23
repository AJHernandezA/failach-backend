# F033 — Seller Authentication (Backend Spec)

## Endpoints

### `POST /api/v1/auth/register`

Registra un nuevo vendedor. Crea usuario en Cognito + registro inicial en DynamoDB.

**Auth**: No requerida.

**Request body**:
```json
{
  "name": "Juan Vendedor",
  "email": "juan@negocio.com",
  "phone": "+573001234567",
  "businessName": "Mi Tienda Online",
  "password": "MiPassword123!"
}
```

**Response** (`201 Created`):
```json
{
  "message": "Registro exitoso. Revisa tu email para confirmar tu cuenta.",
  "email": "juan@negocio.com"
}
```

**Flujo**:
1. Validar inputs.
2. Generar tenantId a partir de businessName (slug: `mi-tienda-online`).
3. Crear usuario en Cognito con `AdminCreateUser` o `SignUp` con custom attributes (`custom:tenantId`, `custom:role=MERCHANT`).
4. Cognito envía email de verificación automáticamente.
5. Crear registro en DynamoDB: USER (status=PENDING), TENANT (status=PENDING).
6. Retornar respuesta.

### `POST /api/v1/auth/confirm`

Confirma email con código de verificación de Cognito.

**Request body**:
```json
{
  "email": "juan@negocio.com",
  "code": "123456"
}
```

**Response** (`200 OK`):
```json
{ "message": "Email confirmado exitosamente. Ya puedes iniciar sesión." }
```

### `POST /api/v1/auth/login`

Inicia sesión. Retorna tokens JWT.

**Request body**:
```json
{
  "email": "juan@negocio.com",
  "password": "MiPassword123!"
}
```

**Response** (`200 OK`):
```json
{
  "accessToken": "eyJ...",
  "idToken": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": {
    "sub": "cognito-uuid",
    "email": "juan@negocio.com",
    "name": "Juan Vendedor",
    "phone": "+573001234567",
    "role": "MERCHANT",
    "tenantId": "mi-tienda-online",
    "businessName": "Mi Tienda Online",
    "status": "ACTIVE"
  }
}
```

**Flujo**:
1. `AdminInitiateAuth` con Cognito (USER_PASSWORD_AUTH o SRP_AUTH).
2. Cognito valida credenciales y retorna tokens.
3. Decodificar JWT para extraer claims.
4. Obtener perfil del usuario de DynamoDB.
5. Verificar que status != SUSPENDED.
6. Set cookies httpOnly con tokens.
7. Retornar response con user info.

### `POST /api/v1/auth/logout`

Cierra sesión. Invalida refresh token.

**Auth**: Requerida (cookie).

**Response** (`200 OK`):
```json
{ "message": "Sesión cerrada" }
```

### `POST /api/v1/auth/refresh`

Refresca access token usando refresh token de la cookie.

**Auth**: Cookie con refresh token.

**Response** (`200 OK`):
```json
{
  "accessToken": "eyJ...",
  "expiresIn": 3600
}
```

### `GET /api/v1/auth/me`

Retorna usuario autenticado actual.

**Auth**: Requerida (Bearer token o cookie).

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
  "status": "ACTIVE"
}
```

### `POST /api/v1/auth/forgot-password`

Solicita código de recuperación de contraseña.

**Request body**:
```json
{ "email": "juan@negocio.com" }
```

**Response** (`200 OK`):
```json
{ "message": "Si el email existe, recibirás un código de recuperación." }
```

### `POST /api/v1/auth/reset-password`

Cambia la contraseña con el código de recuperación.

**Request body**:
```json
{
  "email": "juan@negocio.com",
  "code": "123456",
  "newPassword": "NuevoPassword123!"
}
```

**Response** (`200 OK`):
```json
{ "message": "Contraseña actualizada exitosamente." }
```

### `POST /api/v1/auth/resend-code`

Reenvía código de verificación de email.

**Request body**:
```json
{ "email": "juan@negocio.com" }
```

## Arquitectura (Hexagonal)

```
infrastructure/
├── web/
│   └── AuthController.java             // Endpoints REST
├── middleware/
│   ├── AuthFilter.java                 // Valida JWT en rutas protegidas
│   └── RoleFilter.java                 // Verifica rol del usuario
├── persistence/
│   └── DynamoUserRepository.java       // Persistencia de usuarios
├── cognito/
│   └── CognitoAuthService.java         // Interacción con Cognito SDK
application/
├── usecase/
│   ├── RegisterUserUseCase.java
│   ├── ConfirmEmailUseCase.java
│   ├── LoginUserUseCase.java
│   ├── LogoutUserUseCase.java
│   ├── RefreshTokenUseCase.java
│   ├── GetCurrentUserUseCase.java
│   ├── ForgotPasswordUseCase.java
│   ├── ResetPasswordUseCase.java
│   └── ResendCodeUseCase.java
domain/
├── model/
│   ├── User.java                       // Entidad de usuario
│   └── AuthTokens.java                 // Tokens de autenticación
├── port/
│   ├── UserRepository.java             // Puerto de persistencia
│   └── AuthService.java                // Puerto de autenticación
```

## AuthFilter (Middleware)

- Se ejecuta en rutas protegidas: `/api/v1/admin/**`.
- Extrae token de header `Authorization: Bearer <token>` o cookie `access_token`.
- Valida JWT con la JWK pública de Cognito.
- Verifica: no expirado, audience correcto, issuer correcto.
- Extrae claims: sub, email, custom:role, custom:tenantId.
- Inyecta `AuthContext` en el request para uso en controllers.
- Si falla → `401 Unauthorized`.

## RoleFilter

- Se ejecuta después de AuthFilter en rutas que requieren rol específico.
- Verifica que el usuario tenga el rol requerido (SUPER_ADMIN o MERCHANT).
- Para rutas admin-only → requiere SUPER_ADMIN.
- Si falla → `403 Forbidden`.

## DynamoDB

| PK | SK | Datos |
|----|-----|-------|
| `USER#<cognitoSub>` | `PROFILE` | email, name, phone, role, tenantId, businessName, status, createdAt |
| `TENANT#<tenantId>` | `OWNER` | cognitoSub, email, name, status, createdAt |

## Cognito SDK Calls

| Operación | SDK Method |
|-----------|-----------|
| Register | `signUp()` o `adminCreateUser()` |
| Confirm | `confirmSignUp()` |
| Login | `adminInitiateAuth()` con `AUTH_FLOW=USER_PASSWORD_AUTH` |
| Refresh | `adminInitiateAuth()` con `AUTH_FLOW=REFRESH_TOKEN_AUTH` |
| Forgot | `forgotPassword()` |
| Reset | `confirmForgotPassword()` |
| Resend | `resendConfirmationCode()` |
| Logout | `adminUserGlobalSignOut()` |

## Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `COGNITO_USER_POOL_ID` | ID del User Pool |
| `COGNITO_CLIENT_ID` | ID del App Client |
| `COGNITO_REGION` | Región (us-east-1) |
| `AUTH_MOCK` | `true` en desarrollo local (usa mock auth) |

## Seguridad

- Passwords nunca se logean ni se retornan en responses.
- Tokens JWT tienen TTL corto (1h access, 30d refresh).
- Rate limiting: 10 requests/min en endpoints de auth.
- El mock auth (desarrollo local) se deshabilita en producción.

## Comentarios en código

Sí, todos los archivos Java deben tener comentarios explicatorios en español.

## Notas

- En desarrollo local, `AUTH_MOCK=true` permite usar headers mock (X-Mock-Role, X-Mock-Email, X-Mock-TenantId) como ya existe en el proyecto.
- En producción, `AUTH_MOCK=false` fuerza validación real con Cognito.
- Los cookies deben ser `httpOnly`, `Secure` (en prod), `SameSite=Lax`.
- Al registrar un vendedor, el tenantId se genera como slug del businessName. Si ya existe, se agrega sufijo numérico.
