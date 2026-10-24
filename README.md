# SIGAC — Sistema de Gestión Administrativa y de Caja

Proyecto de investigación aplicada del curso **Desarrollo de Aplicaciones Web II (4697)** — CIBERTEC, Grupo 5.
Aplicado a la Asociación de Comerciantes “Mercado Unión y Progreso”.

Repositorio: **https://github.com/jmestawong/sigac**

SIGAC reemplaza el registro manual de socios, puestos, cobros y egresos por un sistema web: catálogos, cuentas por cobrar, cobranza con recibos de correlativo único, canje bancario, ingresos externos, egresos (con carga masiva desde Excel), reportes XLSX y una bitácora de auditoría.

## Arquitectura de microservicios

Sigue el esquema del repositorio del curso: **Eureka** (descubrimiento), **API Gateway**, **OpenFeign** (comunicación síncrona), **RabbitMQ** (comunicación asíncrona), login con **BCrypt + JWT** y MySQL en **Docker**.

```
Angular (:4200) ──► api-gateway (:8762) ──► ms-auth        (:8083)  login BCrypt + JWT       BD sigac_auth
                          │                 ms-catalogos   (:8081)  socios, puestos, giros,   BD sigac_catalogos
                          │                                         servicios y bancos
                     eureka-server          ms-cobranza    (:8082)  cuentas por cobrar,       BD sigac_cobranza
                        (:8761)                                     pagos y recibos
                                            ms-egresos     (:8084)  egresos + carga masiva    BD sigac_egresos
                                            ms-reportes    (:8085)  6 reportes XLSX           (sin BD, usa Feign)
                                            ms-auditoria   (:8086)  bitácora (consume cola)   BD sigac_auditoria
```

- **OpenFeign:** `ms-cobranza → ms-catalogos` (datos de socio, puesto, servicio y banco), `ms-catalogos → ms-cobranza` (no se elimina lo que tiene cobranza), `ms-reportes → ms-cobranza y ms-egresos`.
- **RabbitMQ:** `ms-cobranza` y `ms-egresos` publican cada movimiento en `sigac.auditoria.queue`; `ms-auditoria` lo consume y lo guarda con el usuario que lo hizo.
- **Seguridad:** `ms-auth` emite el JWT; **cada servicio** lo valida con la misma clave y aplica los roles `ADMIN` / `OPERADOR`.
- `backend/sigac-common`: biblioteca compartida (manejo de errores, validación de JWT, relay del token y eventos de auditoría).

## Stack

Java 17 · Spring Boot 4.0 · Spring Cloud 2025.1 (Eureka, Gateway, OpenFeign) · Spring Data JPA · Spring MVC · Spring Security · Lombok · RabbitMQ · MySQL 8 · Apache POI · Angular 22 + Angular Material · JUnit 5, Mockito, Vitest, Playwright.

## Estructura

```
sigac/
  backend/    proyecto Maven con 9 módulos: sigac-common, eureka-server, api-gateway, ms-auth, ms-catalogos,
              ms-cobranza, ms-egresos, ms-reportes, ms-auditoria  (+ scripts/levantar-servicios.js)
  frontend/   Angular: pantallas, servicios HttpClient, guards, interceptores, pruebas unitarias y e2e
  database/   docker-compose (MySQL + phpMyAdmin), schema.sql (una base por servicio) y datos-iniciales.sql
  queue/      docker-compose de RabbitMQ
  postman/    colección y entorno de Postman (flujo completo con pruebas automáticas)
  docs/       diagramas, capturas y guion del Demo Reel
  entrega/    informe (.docx) y presentación (.pptx)
```

## Cómo levantarlo

1. **Infraestructura (Docker):**

```bash
cd database && docker compose up -d
cd ../queue && docker compose -f docker-compose-rabbitmq.yml up -d
```

MySQL queda en `localhost:5510` (usuario `root`, clave `password`; cada servicio crea su base) y RabbitMQ en `localhost:5672` (panel en `http://localhost:15672`, guest/guest).

2. **Microservicios** (Java 17), desde `backend/`:

```bash
node scripts/levantar-servicios.js
```

Compila si hace falta y levanta Eureka, los seis servicios y el gateway en orden (tarda ~1 minuto). Con `dev` usa H2 en archivo en vez de MySQL (`node scripts/levantar-servicios.js dev`; RabbitMQ sigue siendo necesario para la auditoría). Panel de Eureka: `http://localhost:8761`.

3. **Frontend:**

```bash
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`; el frontend solo conoce el gateway (`http://localhost:8762/api`).

Variables opcionales de los servicios: `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `EUREKA_URL`, `RABBIT_HOST`, `SERVER_PORT`.

### Usuarios de demostración

| Usuario    | Contraseña    | Rol      | Permisos                                                                     |
|------------|---------------|----------|------------------------------------------------------------------------------|
| `admin`    | `admin123`    | ADMIN    | Todo, incluidos catálogos, generación de cuentas y auditoría                 |
| `operador` | `operador123` | OPERADOR | Consulta y operación de caja (cobranza, ingresos, egresos, recibos, reportes) |

Solo para entornos de prueba: cambiar las credenciales y el `JWT_SECRET` antes de desplegar.

## Servicios REST (53 endpoints: 26 GET, 16 POST, 5 PUT, 6 DELETE)

Todos se llaman a través del gateway (`/api/...`) y exigen `Authorization: Bearer <token>`, excepto el login.

| Microservicio | Rutas |
|---------------|-------|
| ms-auth | `POST /api/auth/login` (público) |
| ms-catalogos (CRUD; escritura solo ADMIN) | `/api/socios`, `/api/puestos`, `/api/giros`, `/api/servicios`, `/api/bancos` (GET, POST, PUT, DELETE) |
| ms-cobranza | `GET /api/cuentas-por-cobrar[/{id}]`, `DELETE /{id}` y `POST /generar/...` (ADMIN), `GET /resumen/socio/{id}`, `/resumen/puesto/{id}`, `/referencias/{tipo}/{id}`; `POST /api/pagos/procesar-cuentas`, `/canje-bancario`, `/ingreso-externo`; `GET /api/recibos[/{id}]` |
| ms-egresos | `GET/POST /api/egresos`, `GET /{id}`, `POST /{id}/procesar`, `/{id}/anular`, `/carga-masiva` (.xlsx) |
| ms-reportes | `GET /api/reportes/{movimientos,totales,socios,no-socios,egresos,bancos}` (XLSX) |
| ms-auditoria | `GET /api/auditoria` (solo ADMIN) |

El detalle con rol requerido está en el Anexo B del informe (`entrega/`). La carpeta `postman/` trae una colección que recorre todos los flujos con aserciones.

## Pruebas

| Comando | Qué ejecuta |
|---------|-------------|
| `cd backend && ./mvnw test` | 218 pruebas de los 9 módulos: acceso a datos (los 10 repositorios), servicios con Mockito, integración con MockMvc (seguridad de **todos** los endpoints descubiertos automáticamente), clientes Feign simulados, gateway y Eureka |
| `cd frontend && npm test` | 104 pruebas unitarias (Vitest) |
| `cd frontend && npx playwright test` | 20 pruebas e2e (levanta Eureka, gateway y servicios con perfil `e2e` en H2, y el frontend) |
| `cd frontend && npx ng build` | Compilación de producción |

## Documentación

- `entrega/INFORME_FINAL_GRUPO5_DAW2.docx` — informe del proyecto (diagnóstico SEPTE, objetivos SMART, justificación, alcance, conclusiones, anexos y matriz de rúbrica).
- `entrega/PRESENTACION_GRUPO5_DAW2.pptx` — presentación para la sustentación.
- `docs/guion-demo-reel.md` — guion del video Demo Reel (3 a 5 minutos).
- `docs/diagramas/` — arquitectura de microservicios y modelo de datos por servicio; `docs/capturas/` — capturas del sistema.
