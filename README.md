# SIGAC — Sistema de Gestión Administrativa y de Caja

Proyecto académico del curso **Desarrollo de Aplicaciones Web I** (CIBERTEC). Primera versión: login + CRUD de socios.

Repositorio: **https://github.com/jmestawong/sigac**

## Stack

- **Backend**: Java + Spring Boot, Spring Data JPA, Spring MVC, Spring Security, Lombok
- **Frontend**: Angular (standalone components), `HttpClient`
- **Base de datos**: MySQL (perfil por defecto), con `application-postgres.properties` listo para migrar a PostgreSQL
- **Autenticación**: JWT, password cifrado con `BCryptPasswordEncoder`

## Estructura

```
sigac/
  backend/    Spring Boot: entidades, repositorios, servicios, controladores, seguridad
  frontend/   Angular: login, listado/CRUD de socios
```

## Cómo levantarlo localmente

### Backend

Requiere MySQL corriendo en `localhost:3306` (o usa el perfil `dev` con H2 sin instalar nada):

```bash
cd backend
./mvnw spring-boot:run
```

Configuración vía variables de entorno (`DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `SERVER_PORT`, `JWT_SECRET`, etc.), con defaults en `application.properties`.

Para probar sin instalar MySQL (usa un archivo H2 local en `backend/data/`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend en `http://localhost:8080`. Al arrancar se crean automáticamente dos usuarios:

| Usuario    | Password      | Rol      |
|------------|---------------|----------|
| `admin`    | `admin123`    | ADMIN    |
| `operador` | `operador123` | OPERADOR |

### Frontend

```bash
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`.

### Pruebas del backend

```bash
cd backend
./mvnw test
```

## Endpoints

- `POST /api/auth/login` — público, retorna un JWT
- `GET|POST|PUT|DELETE /api/socios` (y `/api/socios/{id}`) — requieren `Authorization: Bearer <token>`
