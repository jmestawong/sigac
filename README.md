# SIGAC — Sistema de Gestión Administrativa y de Caja

Proyecto de investigación aplicada del curso **Desarrollo de Aplicaciones Web II (4697)** — CIBERTEC, Grupo 5.
Aplicado a la Asociación de Comerciantes “Mercado Unión y Progreso”.

Repositorio: **https://github.com/jmestawong/sigac**

SIGAC reemplaza el registro manual de socios, puestos, cobros y egresos por un sistema web centralizado: catálogos, cuentas por cobrar, cobranza con recibos de correlativo único, canje bancario, ingresos externos, egresos (con carga masiva desde Excel) y seis reportes XLSX.

## Stack

- **Backend**: Java 17, Spring Boot 4.1, Spring Data JPA (Hibernate), Spring MVC, Spring Security 7, Lombok, Bean Validation, Apache POI (Excel).
- **Frontend**: Angular 22 (componentes standalone, signals, rutas con carga diferida), Angular Material, RxJS.
- **Base de datos**: MySQL 8 (perfil por defecto); H2 para el perfil `dev`, las pruebas y e2e.
- **Seguridad**: login REST, contraseñas cifradas con `BCryptPasswordEncoder`, tokens JWT y roles `ADMIN` / `OPERADOR` validados en el backend.

## Estructura

```
sigac/
  backend/    Spring Boot: entidades, repositorios, servicios, controladores, seguridad y pruebas
  frontend/   Angular: pantallas, servicios HttpClient, guards, interceptores, pruebas unitarias y e2e
  database/   schema.sql (9 tablas) y datos-iniciales.sql (usuarios con hash BCrypt)
  docs/       diagramas, capturas de pantalla y guion del Demo Reel
  entrega/    informe (.docx) y presentación (.pptx) del proyecto
```

## Cómo levantarlo localmente

### Base de datos (opcional)

Con MySQL 8 en `localhost:3306`, ejecutar en orden `database/schema.sql` y `database/datos-iniciales.sql`.
Si se omiten, el backend crea el esquema (`ddl-auto=update`) y los usuarios de demostración al arrancar.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Variables de entorno opcionales: `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`, `DB_NAME`, `SERVER_PORT`, `JWT_SECRET`. Escucha en `http://localhost:8080`.

Sin instalar MySQL (H2 en archivo local `backend/data/`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Abre `http://localhost:4200`. En desarrollo el frontend apunta a `http://localhost:8081/api` (`src/environments/environment.development.ts`), por lo que el backend debe arrancarse con `SERVER_PORT=8081`; la compilación de producción apunta a `http://localhost:8080/api`.

### Usuarios de demostración

| Usuario    | Contraseña    | Rol      | Permisos                                                                 |
|------------|---------------|----------|--------------------------------------------------------------------------|
| `admin`    | `admin123`    | ADMIN    | Todo el sistema, incluida la gestión de catálogos y generación de cuentas |
| `operador` | `operador123` | OPERADOR | Consulta y operación de caja (cobranza, ingresos, egresos, recibos, reportes) |

Solo para entornos de prueba: cambiar las credenciales y el `JWT_SECRET` antes de desplegar.

## Servicios REST (51 endpoints: 24 GET, 16 POST, 5 PUT, 6 DELETE)

Todos exigen `Authorization: Bearer <token>`, excepto el login.

| Módulo | Rutas |
|--------|-------|
| Autenticación | `POST /api/auth/login` (público) |
| Catálogos (CRUD completo; escritura solo ADMIN) | `/api/socios`, `/api/puestos`, `/api/giros`, `/api/servicios`, `/api/bancos` |
| Cuentas por cobrar | `GET /api/cuentas-por-cobrar[/{id}]`, `DELETE /{id}` (ADMIN), `POST /generar/puestos-monto-fijo`, `/generar/puestos-consumo`, `/generar/socios` (ADMIN), `GET /resumen/socio/{id}`, `/resumen/puesto/{id}` |
| Pagos | `POST /api/pagos/procesar-cuentas`, `/canje-bancario`, `/ingreso-externo` |
| Recibos | `GET /api/recibos[/{id}]` |
| Egresos | `GET /api/egresos[/{id}]`, `POST /api/egresos`, `/{id}/procesar`, `/{id}/anular`, `/carga-masiva` (.xlsx) |
| Reportes (XLSX) | `GET /api/reportes/...` (movimientos, totales, socios, puestos, egresos, bancos) |

El detalle completo con rol requerido está en el Anexo B del informe (`entrega/`).

## Pruebas

| Comando | Qué ejecuta |
|---------|-------------|
| `cd backend && ./mvnw test` | 141 pruebas: acceso a datos (55, los 9 repositorios), servicios con Mockito (63) e integración con MockMvc (23, incluida la seguridad de todos los endpoints) |
| `cd frontend && npm test` | 101 pruebas unitarias (Vitest) |
| `cd frontend && npx playwright test` | 18 pruebas e2e (levanta el backend con perfil `e2e` y el frontend) |
| `cd frontend && npx ng build` | Compilación de producción |

## Documentación

- `entrega/INFORME_FINAL_GRUPO5_DAW2.docx` — informe del proyecto (diagnóstico SEPTE, objetivos SMART, justificación, alcance, conclusiones, anexos y matriz de rúbrica).
- `entrega/PRESENTACION_GRUPO5_DAW2.pptx` — presentación para la sustentación.
- `docs/guion-demo-reel.md` — guion del video Demo Reel (3 a 5 minutos).
- `docs/diagramas/` — arquitectura y modelo de datos; `docs/capturas/` — capturas del sistema.
