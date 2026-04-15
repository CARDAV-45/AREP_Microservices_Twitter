# Twitter AREP — Aplicación Twitter-like con Microservicios y Auth0

- **Autores**: Carlos David Barrero Velasquez y Esteban Aguilera Contreras
- **Universidad**: Escuela Colombiana de Ingeniería Julio Garavito
- **Asignatura**: Arquitecturas Empresariales (AREP)
- **Fecha**: Abril 2026

## Introducción

Aplicación web similar a Twitter que permite a usuarios autenticados publicar posts de máximo 140 caracteres en un stream público global. El proyecto inicia como un monolito Spring Boot, se asegura con Auth0 y evoluciona hacia microservicios serverless en AWS Lambda.

## Arquitectura

### Fase 1 — Monolito Spring Boot

```
Frontend (React + Vite)          Backend (Spring Boot)
     localhost:3000       →           localhost:8080
          │                               │
          │  Auth0 JWT                    │  OAuth2 Resource Server
          └──── POST /api/posts ─────────►│
          └──── GET  /api/posts ─────────►│  (público)
          └──── GET  /api/me   ─────────►│  (protegido)
                                          │
                                     H2 Database
                                   (POSTS, USERS)
```

### Flujo de Autenticación Auth0

```
Usuario → Login → Auth0 Universal Login
       ← JWT Access Token (con claims: name, email, picture)
       → Frontend guarda token en localStorage
       → Backend valida JWT (issuer + audience)
       → Acceso concedido al recurso protegido
```

---

## Tecnologías utilizadas

| Capa | Tecnología |
|------|-----------|
| Frontend | React 18 + Vite + Auth0 React SDK |
| Backend | Spring Boot 3.4.4 + Spring Security OAuth2 |
| Base de datos | H2 (en memoria, desarrollo) |
| Autenticación | Auth0 (SPA + API) |
| Documentación | Swagger / OpenAPI 3 (springdoc) |
| Microservicios | AWS Lambda + DynamoDB |

---

## Estructura del repositorio

```
AREP_Microservices_Twitter/
├── microservices/          ← Monolito Spring Boot
│   ├── src/main/java/arep/edu/co/microservices/
│   │   ├── config/         ← SecurityConfig, OpenApiConfig
│   │   ├── controller/     ← PostController, UserController
│   │   ├── dto/            ← PostRequest, PostResponse, UserResponse
│   │   ├── model/          ← User, Post
│   │   ├── repository/     ← UserRepository, PostRepository
│   │   └── service/        ← UserService, PostService
│   └── src/main/resources/
│       └── application.properties
├── fronted/                ← Frontend React
│   ├── src/
│   │   ├── components/     ← Navbar, Composer, Feed
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── .env
└── Capturas/               ← Evidencias del funcionamiento
```

---

## API REST — Endpoints

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/api/posts` | No | Stream público de posts |
| POST | `/api/posts` | JWT | Crear un nuevo post (max 140 chars) |
| GET | `/api/me` | JWT | Perfil del usuario autenticado |

---

## Swagger UI

La documentación OpenAPI está disponible en `http://localhost:8080/swagger-ui.html`.

![Swagger UI](Capturas/Captura%20de%20pantalla%202026-04-14%20181224.png)

---

## Configuración de Auth0

### Aplicaciones registradas

![Auth0 Applications](Capturas/Captura%20de%20pantalla%202026-04-14%20183823.png)

- **AREP_Twitter** — Single Page Application (frontend)
- **twitter-api (Test Application)** — M2M generado automáticamente con la API

### Flujo Post-Login (Add Profile Claims)

Para incluir `name`, `email` y `picture` en el access token se configuró una **Action** en el trigger `post-login`:

![Auth0 Post Login Flow](Capturas/Captura%20de%20pantalla%202026-04-14%20185735.png)

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const ns = 'https://twitter-api/';
  api.accessToken.setCustomClaim(ns + 'name',     event.user.name);
  api.accessToken.setCustomClaim(ns + 'email',    event.user.email);
  api.accessToken.setCustomClaim(ns + 'picture',  event.user.picture);
  api.accessToken.setCustomClaim(ns + 'nickname', event.user.nickname);
};
```

### Pantalla de Login Auth0

![Auth0 Login](Capturas/Captura%20de%20pantalla%202026-04-14%20185706.png)

### Pantalla de Autorización Auth0

![Auth0 Authorize](Capturas/Captura%20de%20pantalla%202026-04-14%20185719.png)

---

## Base de datos H2

Tablas creadas automáticamente al iniciar el monolito:

![H2 Console](Capturas/Captura%20de%20pantalla%202026-04-14%20181213.png)

Acceso en `http://localhost:8080/h2-console`:
- **JDBC URL:** `jdbc:h2:mem:twitterdb`
- **User:** `sa`
- **Password:** *(vacío)*

---

## Frontend — Aplicación React

### Stream con múltiples usuarios

![Frontend con nombre de usuario](Capturas/Captura%20de%20pantalla%202026-04-14%20185646.png)

La aplicación permite:
- Login / Logout con Auth0
- Ver el stream público sin autenticación
- Crear posts de hasta 140 caracteres (requiere login)
- Ver el nombre y avatar del autor en cada post

---

## Ejecución local

### Requisitos previos
- Java 17+
- Node.js 18+
- Maven 3.8+
- Cuenta Auth0 (gratuita)

### 1. Configurar Auth0

En `microservices/src/main/resources/application.properties`:
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://TU-DOMINIO.auth0.com/
auth0.audience=https://twitter-api
```

En `fronted/.env`:
```env
VITE_AUTH0_DOMAIN=TU-DOMINIO.auth0.com
VITE_AUTH0_CLIENT_ID=TU_CLIENT_ID
VITE_AUTH0_AUDIENCE=https://twitter-api
```

### 2. Correr el backend
```bash
cd microservices
./mvnw spring-boot:run
```
Backend disponible en `http://localhost:8080`

### 3. Correr el frontend
```bash
cd fronted
npm install
npm run dev
```
Frontend disponible en `http://localhost:3000`

---

## Pruebas realizadas

| Prueba | Resultado |
|--------|-----------|
| GET `/api/posts` sin token | 200 OK — retorna lista vacía |
| POST `/api/posts` sin token | 401 Unauthorized |
| POST `/api/posts` con JWT válido | 201 Created |
| GET `/api/me` con JWT válido | 200 OK — retorna perfil del usuario |
| Login con Auth0 | Redirige a Universal Login, retorna JWT |
| Dos usuarios distintos publicando | Ambos posts visibles en el stream |
| Post > 140 caracteres | Bloqueado en frontend y backend |

---

## Microservicios AWS Lambda (en desarrollo)

La migración al modelo serverless separa el monolito en 3 funciones Lambda independientes:

| Servicio | Trigger | Descripción |
|----------|---------|-------------|
| `user-service` | GET `/api/me` | Gestión de usuarios — upsert en DynamoDB |
| `post-service` | POST `/api/posts` | Creación de posts — requiere JWT |
| `stream-service` | GET `/api/posts` | Feed público — lectura de DynamoDB |

Cada Lambda valida el JWT de Auth0 usando `nimbus-jose-jwt` y persiste datos en **AWS DynamoDB**.

---

## Variables de entorno

**No commitear** las siguientes variables:

| Variable | Descripción |
|----------|-------------|
| `VITE_AUTH0_CLIENT_ID` | Client ID de la SPA en Auth0 |
| `AUTH0_DOMAIN` | Dominio Auth0 |
| `AUTH0_AUDIENCE` | Audience del API |

