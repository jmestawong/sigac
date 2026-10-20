# Guion del video Demo Reel (3 a 5 minutos)

Proyecto SIGAC — Desarrollo de Aplicaciones Web II, Grupo 5. El video no reemplaza a la sustentación.
Duración prevista: 4 min 50 s. Grabar la pantalla con voz en off, a 1080p, con datos de demostración cargados.

| Tiempo | Escena | Qué se muestra y qué se dice | Referencia |
|---|---|---|---|
| 0:00 – 0:20 | 1. Presentación | Carátula y equipo. Problema: la Asociación “Mercado Unión y Progreso” administra socios, cobros y egresos en cuadernos y hojas de cálculo. Una frase del diagnóstico SEPTE (2,612 mercados; 70.7 % de empleo informal). | Secciones 2 y 3 |
| 0:20 – 0:45 | 2. Arquitectura | Diagrama: Angular 22 → API REST Spring Boot (51 endpoints) → MySQL. Mencionar Spring Security, JWT, BCrypt, Lombok y Spring Data. | Figura 1 |
| 0:45 – 1:15 | 3. Login y seguridad | Credenciales incorrectas (mensaje de error); ingresar como admin. Mostrar en MySQL la tabla usuarios con el hash BCrypt ($2a$...). Abrir una ruta sin sesión: redirige al login. | RF-01 a RF-04 |
| 1:15 – 1:55 | 4. CRUD de socios | Listar (GET), crear (POST), editar (PUT) y eliminar con confirmación (DELETE). Recargar la página para evidenciar la persistencia en la base de datos. | RF-05 a RF-07 |
| 1:55 – 2:25 | 5. Catálogos y cuentas por cobrar | Mostrar puestos, servicios y bancos. Generar cuentas de un servicio por consumo (lecturas inicial y final) y de un servicio fijo para socios filtrando por etapa. | RF-08 a RF-18 |
| 2:25 – 3:15 | 6. Cobranza y recibo | Buscar por socio, marcar una cuenta como abonada y otra como exonerada, ver el total, procesar y abrir el voucher con su correlativo (R-00000X). | RF-19 a RF-23, RF-29 |
| 3:15 – 3:40 | 7. Canje bancario e ingreso externo | Canjear una cuenta indicando banco y fecha de depósito; registrar un ingreso externo; filtrar Recibos por tipo y fecha. | RF-24, RF-25, RF-31 |
| 3:40 – 4:10 | 8. Egresos y reportes | Registrar un egreso, cargar el Excel de egresos masivos (filas válidas e inválidas), procesar y anular; descargar un reporte XLSX y abrirlo. | RF-27 a RF-33 |
| 4:10 – 4:30 | 9. Rol OPERADOR | Cerrar sesión e ingresar como operador: menú acotado; intentar crear un socio no está disponible (la API responde 403). | Sección 6.5 |
| 4:30 – 4:50 | 10. Pruebas y cierre | Terminal: ./mvnw test (141 pruebas en verde), npm test (101) y npx playwright test (18). Cerrar con las conclusiones y el repositorio https://github.com/jmestawong/sigac. | Anexo D |

## Preparación

1. Backend con perfil dev y frontend en marcha (ver README).
2. Iniciar sesión como `admin` / `admin123` y cargar algunos socios, puestos, servicios y cuentas.
3. Tener a mano el archivo Excel de egresos masivos (con alguna fila inválida) y la terminal lista para ejecutar las pruebas.
