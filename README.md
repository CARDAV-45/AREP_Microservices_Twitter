# Twitter AREP — Aplicación Twitter-like con Microservicios y Auth0

- **Autores**: Carlos David Barrero Velasquez y Esteban Aguilera Contreras
- **Universidad**: Escuela Colombiana de Ingeniería Julio Garavito
- **Asignatura**: Arquitecturas Empresariales (AREP)
- **Fecha**: Abril 2026


## Introducción

Aplicación web similar a Twitter que permite a usuarios autenticados publicar posts de máximo 140 caracteres en un stream público global. El proyecto inicia como un monolito Spring Boot, se asegura con Auth0 y evoluciona hacia microservicios serverless en AWS Lambda con DynamoDB.

---

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

### Fase 2 — Microservicios AWS Lambda

```
Frontend (Netlify HTTPS)
        │
        ▼
API Gateway HTTP API (twitter-api-gateway)
  https://v6bvjlnu4b.execute-api.us-east-1.amazonaws.com
        │
        ├── GET  /api/posts  ──► Lambda twitter-stream ──► DynamoDB twitter-posts
        ├── POST /api/posts  ──► Lambda twitter-post   ──► DynamoDB twitter-posts
        └── GET  /api/me     ──► Lambda twitter-user   ──► DynamoDB twitter-users
```

### Flujo de Autenticación Auth0

```
Usuario → Login → Auth0 Universal Login
       ← JWT Access Token (con claims: name, email, picture)
       → Frontend guarda token en localStorage
       → Lambda valida JWT (nimbus-jose-jwt: firma RS256, issuer, audience)
       → Acceso concedido al recurso protegido
```

---

## Tecnologías utilizadas

| Capa | Tecnología |
|------|-----------|
| Frontend | React 18 + Vite + Auth0 React SDK |
| Backend (monolito) | Spring Boot 3.4.4 + Spring Security OAuth2 |
| Base de datos (monolito) | H2 (en memoria) |
| Microservicios | AWS Lambda (Java 17) |
| Base de datos (Lambda) | AWS DynamoDB |
| API Gateway | AWS API Gateway HTTP API |
| Hosting frontend | Netlify (HTTPS) |
| Autenticación | Auth0 (SPA + API + Post-Login Action) |
| Documentación | Swagger / OpenAPI 3 (springdoc) |

---

## Estructura del repositorio

```
AREP_Microservices_Twitter/
├── microservices/                  ← Monolito Spring Boot
│   ├── src/main/java/arep/edu/co/microservices/
│   │   ├── config/                 ← SecurityConfig, OpenApiConfig
│   │   ├── controller/             ← PostController, UserController
│   │   ├── dto/                    ← PostRequest, PostResponse, UserResponse
│   │   ├── model/                  ← User, Post
│   │   ├── repository/             ← UserRepository, PostRepository
│   │   └── service/                ← UserService, PostService
│   ├── src/main/resources/
│   │   └── application.properties
│   └── lambda/                     ← Microservicios AWS Lambda
│       └── src/main/java/arep/edu/co/lambda/
│           ├── PostHandler.java    ← POST /api/posts
│           ├── StreamHandler.java  ← GET  /api/posts
│           ├── UserHandler.java    ← GET  /api/me
│           └── util/JwtValidator.java
├── fronted/                        ← Frontend React
│   ├── src/
│   │   ├── components/             ← Navbar, Composer, Feed
│   │   ├── App.jsx
│   │   └── main.jsx
│   └── .env
└── Capturas/                       ← Evidencias del funcionamiento
```

---

## API REST — Endpoints

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| GET | `/api/posts` | No | Stream público de posts |
| POST | `/api/posts` | JWT | Crear un nuevo post (max 140 chars) |
| GET | `/api/me` | JWT | Perfil del usuario autenticado |

---

## Swagger UI (Fase 1 — Monolito)

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

## Base de datos H2 (Fase 1)

Tablas creadas automáticamente al iniciar el monolito:

![H2 Console](Capturas/Captura%20de%20pantalla%202026-04-14%20181213.png)

Acceso en `http://localhost:8080/h2-console`:
- **JDBC URL:** `jdbc:h2:mem:twitterdb`
- **User:** `sa`
- **Password:** *(vacío)*

---

## Microservicios AWS Lambda (Fase 2)

### Despliegue de funciones Lambda

Las tres funciones Lambda se construyen en un único fat JAR (`twitter-lambda.jar`) con el plugin `maven-shade-plugin`. Cada Lambda se configura con un handler class distinto.

**Función twitter-stream** — desplegada con Java 17, handler `arep.edu.co.lambda.StreamHandler::handleRequest`:

![Lambda twitter-stream](Capturas/Captura%20de%20pantalla%202026-04-16%20132238.png)

**Carga del JAR** — subida del fat JAR (13.8 MB) desde archivo local:

![Lambda JAR Upload](Capturas/Captura%20de%20pantalla%202026-04-16%20132427.png)

**Las tres funciones desplegadas:**

![Lambda Functions List](Capturas/Captura%20de%20pantalla%202026-04-16%20132545.png)

| Función | Handler | Tabla DynamoDB |
|---------|---------|----------------|
| `twitter-stream` | `arep.edu.co.lambda.StreamHandler::handleRequest` | `twitter-posts` (lectura) |
| `twitter-post` | `arep.edu.co.lambda.PostHandler::handleRequest` | `twitter-posts` (escritura) |
| `twitter-user` | `arep.edu.co.lambda.UserHandler::handleRequest` | `twitter-users` (upsert) |

### Variables de entorno en Lambda

Cada función recibe las variables de entorno para validar JWT:

![Lambda Environment Variables](Capturas/Captura%20de%20pantalla%202026-04-16%20132654.png)

| Variable | Valor |
|----------|-------|
| `AUTH0_DOMAIN` | `dev-ctb3u3ue5k6bs30k.us.auth0.com` |
| `AUTH0_AUDIENCE` | `https://twitter-api` |

### Prueba de la función twitter-stream

Test exitoso desde la consola de Lambda — retorna `statusCode: 200` con la lista de posts:

![Lambda Test Result](Capturas/Captura%20de%20pantalla%202026-04-16%20133209.png)

---

## API Gateway HTTP API

### Configuración de integraciones

Se creó una API HTTP llamada `twitter-api-gateway` integrando las tres funciones Lambda:

![API Gateway Integrations](Capturas/Captura%20de%20pantalla%202026-04-16%20133428.png)

### Rutas configuradas

![API Gateway Routes](Capturas/Captura%20de%20pantalla%202026-04-16%20133556.png)

| Método | Ruta | Lambda destino |
|--------|------|---------------|
| GET | `/api/posts` | `twitter-stream` |
| POST | `/api/posts` | `twitter-post` |
| GET | `/api/me` | `twitter-user` |

### Revisión final antes de crear

![API Gateway Review](Capturas/Captura%20de%20pantalla%202026-04-16%20133636.png)

### Configuración CORS

Para permitir peticiones desde el frontend se configuró CORS en el API Gateway:

![API Gateway CORS](Capturas/Captura%20de%20pantalla%202026-04-16%20134344.png)

- **Allow-Origin:** `*`
- **Allow-Headers:** `content-type`, `authorization`
- **Allow-Methods:** `GET`, `POST`, `OPTIONS`

---

## DynamoDB

Los posts publicados se persisten en la tabla `twitter-posts`:

![DynamoDB twitter-posts](Capturas/Captura%20de%20pantalla%202026-04-16%20134919.png)

Cada item contiene: `id`, `authorId`, `authorName`, `authorPicture`, `content`, `createdAt`.

---

## Despliegue del Frontend

### Build de producción

El frontend React se compila a la carpeta `dist/`:

![Frontend dist](Capturas/Captura%20de%20pantalla%202026-04-16%20135509.png)

```bash
cd fronted
npm run build
```

### S3 — Intento de alojamiento estático

Se creó el bucket `twitter-arep-frontend` con alojamiento estático habilitado y política pública:

![S3 Static Hosting](Capturas/Captura%20de%20pantalla%202026-04-16%20135944.png)

![S3 Bucket Policy](Capturas/Captura%20de%20pantalla%202026-04-16%20140108.png)

Se subieron los 3 archivos del build (316.9 KB total):

![S3 Upload](Capturas/Captura%20de%20pantalla%202026-04-16%20141726.png)

Sin embargo, al abrir la URL de S3 la aplicación lanzaba el error:

> **`auth0-spa-js must run on a secure origin`**

El SDK de Auth0 v2 exige que la aplicación corra bajo **HTTPS**. S3 static website hosting solo sirve HTTP (`http://bucket.s3-website-*.amazonaws.com`). La solución habitual sería poner una distribución **CloudFront** delante del bucket para obtener HTTPS, pero **AWS Academy no otorga permisos para crear distribuciones CloudFront**.

Se intentó igualmente crear la distribución:

![CloudFront Specify Origin](Capturas/Captura%20de%20pantalla%202026-04-16%20141204.png)

![CloudFront Review](Capturas/Captura%20de%20pantalla%202026-04-16%20141327.png)

La operación fue denegada por las restricciones de la cuenta de AWS Academy.

### Netlify — Hosting con HTTPS

Como alternativa gratuita y sin restricciones de permisos, el frontend se desplegó en **Netlify** arrastrando la carpeta `dist/`:

![Netlify Deploy](Capturas/Captura%20de%20pantalla%202026-04-16%20143757.png)

**URL pública:** `https://69e138d0171a454a28b2fd59--profound-monstera-754629.netlify.app`

---

## Aplicación en producción

La aplicación completa corriendo en Netlify, conectada a los Lambda vía API Gateway, con múltiples usuarios publicando posts:

![App en producción](Capturas/Captura%20de%20pantalla%202026-04-16%20143807.png)

---

## Ejecución local (Fase 1 — Monolito)

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
VITE_API_URL=http://localhost:8080
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

## Despliegue en AWS (Fase 2 — Lambda)

### 1. Compilar el fat JAR
```bash
cd microservices/lambda
mvn clean package -DskipTests
# Genera: target/twitter-lambda.jar
```

### 2. Crear las funciones Lambda en AWS Console
Para cada función (`twitter-stream`, `twitter-post`, `twitter-user`):
- Runtime: **Java 17**
- Arquitectura: **x86_64**
- Subir `twitter-lambda.jar` desde archivo local
- Configurar el handler correspondiente (ver tabla arriba)
- Agregar variables de entorno: `AUTH0_DOMAIN` y `AUTH0_AUDIENCE`

### 3. Crear API Gateway HTTP API
- Integrar las tres funciones Lambda
- Configurar las rutas (GET/POST /api/posts, GET /api/me)
- Habilitar CORS (Allow-Origin `*`, headers `content-type,authorization`)

### 4. Construir y desplegar el frontend
```bash
cd fronted
# Actualizar .env con la URL del API Gateway
echo "VITE_API_URL=https://TU-API-GATEWAY-URL" >> .env
npm run build
# Subir la carpeta dist/ a Netlify o S3 con HTTPS
```

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
| Lambda twitter-stream test | 200 OK, `body: "[]"`, 2830ms, 159MB RAM |
| DynamoDB persistencia | Posts almacenados correctamente con todos los campos |
| Frontend en Netlify (HTTPS) | Funciona completo, Auth0 no bloquea por origen inseguro |

---

## URLs de producción

| Servicio | URL |
|----------|-----|
| Frontend (Netlify) | `https://69e138d0171a454a28b2fd59--profound-monstera-754629.netlify.app` |
| API Gateway | `https://v6bvjlnu4b.execute-api.us-east-1.amazonaws.com` |

---

## Variables de entorno

| Variable | Descripción |
|----------|-------------|
| `VITE_AUTH0_CLIENT_ID` | Client ID de la SPA en Auth0 |
| `AUTH0_DOMAIN` | Dominio Auth0 |
| `AUTH0_AUDIENCE` | Audience del API |

---

## Video de demostración

> **Nota:** El video requiere acceso con correo institucional de la Escuela Colombiana de Ingeniería.

[Ver video de demostración](https://pruebacorreoescuelaingeduco-my.sharepoint.com/:v:/g/personal/carlos_barrero-v_mail_escuelaing_edu_co/IQAuD7sslsQ2RY8TI03YbER4AbG3EjLURWXP5R63ht-WGiM?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0NvcHkifX0&e=TvXK7K)

---
