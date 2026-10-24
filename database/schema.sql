-- ============================================================================
-- SIGAC - Estructura de las bases de datos (una por microservicio)
--
--   sigac_auth        -> ms-auth        (usuarios, contrasenas con BCrypt)
--   sigac_catalogos   -> ms-catalogos   (giros, socios, puestos, servicios_cobrables, bancos)
--   sigac_cobranza    -> ms-cobranza    (cuentas_por_cobrar, recibos)
--   sigac_egresos     -> ms-egresos     (egresos)
--   sigac_auditoria   -> ms-auditoria   (auditoria)
--
-- Ningun servicio lee las tablas de otro: se comunican por API (OpenFeign) o por
-- mensajes (RabbitMQ). Los servicios crean estas tablas solos al arrancar
-- (spring.jpa.hibernate.ddl-auto=update); este script es solo la referencia
-- del modelo y permite crearlas a mano.
-- Generado con mysqldump --no-data desde MySQL 8.0.
-- ============================================================================

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sigac_auth` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `sigac_auth`;
CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `rol` enum('ADMIN','OPERADOR') NOT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2dvbwfge291euvmk6vkkocao` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sigac_catalogos` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `sigac_catalogos`;
CREATE TABLE `bancos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cci` varchar(20) NOT NULL,
  `moneda` enum('PEN','USD') NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `numero_cuenta` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKp03q0j0wja873mpkbbfh37ncc` (`cci`),
  UNIQUE KEY `UKp806nsr9tgekcodhcbpbdftfq` (`numero_cuenta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `giros` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKeoyvqsu7h18k8npveqi3fyb5e` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `puestos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `fecha_fin_vigencia` date NOT NULL,
  `fecha_inicio_vigencia` date NOT NULL,
  `nombre_inquilino` varchar(150) NOT NULL,
  `numero` varchar(20) NOT NULL,
  `giro_id` bigint NOT NULL,
  `socio_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr79tydfqs6hrrekklyxe56j9w` (`numero`),
  KEY `FK6b4hm3x679qwrr61d4gmp3qnm` (`giro_id`),
  KEY `FKirlehqc5j83lkfuopya0o60sc` (`socio_id`),
  CONSTRAINT `FK6b4hm3x679qwrr61d4gmp3qnm` FOREIGN KEY (`giro_id`) REFERENCES `giros` (`id`),
  CONSTRAINT `FKirlehqc5j83lkfuopya0o60sc` FOREIGN KEY (`socio_id`) REFERENCES `socios` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `servicios_cobrables` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `costo` decimal(12,2) NOT NULL,
  `destinatario` enum('PUESTO','SOCIO') NOT NULL,
  `es_por_consumo` bit(1) NOT NULL,
  `moneda` enum('PEN','USD') NOT NULL,
  `nombre` varchar(150) NOT NULL,
  `recurrencia` enum('ANUAL','BIMESTRAL','MENSUAL','TRIMESTRAL','UNICA') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `socios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accion` varchar(100) NOT NULL,
  `apellidos` varchar(100) NOT NULL,
  `codigo` varchar(30) NOT NULL,
  `etapa` varchar(50) NOT NULL,
  `fecha_nacimiento` date NOT NULL,
  `nombres` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKhjh4jjojtq9khoi7jxl4e14hb` (`codigo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sigac_cobranza` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `sigac_cobranza`;
CREATE TABLE `cuentas_por_cobrar` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `estado` enum('ABONADA','EXONERADA','PENDIENTE') NOT NULL,
  `lectura_final` decimal(12,2) DEFAULT NULL,
  `lectura_inicial` decimal(12,2) DEFAULT NULL,
  `monto` decimal(12,2) NOT NULL,
  `periodo` varchar(20) NOT NULL,
  `puesto_id` bigint DEFAULT NULL,
  `servicio_id` bigint NOT NULL,
  `socio_id` bigint DEFAULT NULL,
  `recibo_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKk8xgduc4553um8ojxsqcg3vs2` (`recibo_id`),
  CONSTRAINT `FKk8xgduc4553um8ojxsqcg3vs2` FOREIGN KEY (`recibo_id`) REFERENCES `recibos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `recibos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `banco_id` bigint DEFAULT NULL,
  `categoria` varchar(100) DEFAULT NULL,
  `concepto` varchar(255) DEFAULT NULL,
  `correlativo` varchar(20) DEFAULT NULL,
  `depositante` varchar(150) DEFAULT NULL,
  `fecha` datetime(6) NOT NULL,
  `fecha_deposito` date DEFAULT NULL,
  `monto` decimal(12,2) NOT NULL,
  `tipo` enum('CANJE_BANCARIO','INGRESO_EXTERNO','PAGO_CUENTAS') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKck8u0d4gm9ebeaqrohm91llsa` (`correlativo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sigac_egresos` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `sigac_egresos`;
CREATE TABLE `egresos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `correlativo` varchar(20) DEFAULT NULL,
  `documento_asociado` varchar(30) DEFAULT NULL,
  `estado` enum('ANULADO','PROCESADO','REGISTRADO') NOT NULL,
  `fecha` date NOT NULL,
  `importe` decimal(12,2) NOT NULL,
  `motivo` varchar(255) NOT NULL,
  `numero_documento` varchar(30) NOT NULL,
  `proveedor` varchar(150) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKjjxrk3m3tclucgum56xg1jck2` (`correlativo`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE DATABASE /*!32312 IF NOT EXISTS*/ `sigac_auditoria` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `sigac_auditoria`;
CREATE TABLE `auditoria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accion` varchar(40) NOT NULL,
  `detalle` varchar(400) DEFAULT NULL,
  `fecha` datetime(6) NOT NULL,
  `modulo` varchar(30) NOT NULL,
  `monto` decimal(12,2) DEFAULT NULL,
  `referencia` varchar(100) DEFAULT NULL,
  `usuario` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
