-- ============================================================================
-- SIGAC - Sistema de Gestion Administrativa y de Caja
-- Script de estructura de base de datos (MySQL 8)
--
-- Generado desde el esquema real creado por Hibernate (spring.jpa.hibernate.ddl-auto=update)
-- y exportado con mysqldump --no-data. La aplicacion tambien puede crear estas
-- tablas automaticamente al arrancar; este script permite crearlas manualmente.
--
-- Uso:
--   mysql -u root -p < database/schema.sql
-- ============================================================================

CREATE DATABASE IF NOT EXISTS sigac_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE sigac_db;

-- Las tablas se crean en orden alfabetico; se desactivan las llaves foraneas durante la carga.
SET FOREIGN_KEY_CHECKS = 0;

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
  KEY `FK4cmawu2md40dw79hkoevoedtq` (`puesto_id`),
  KEY `FK39mugalxc6qpsyrta88mannpl` (`servicio_id`),
  KEY `FKhtu2p8qbncn9w9j6i2shdvuqx` (`socio_id`),
  KEY `FKk8xgduc4553um8ojxsqcg3vs2` (`recibo_id`),
  CONSTRAINT `FK39mugalxc6qpsyrta88mannpl` FOREIGN KEY (`servicio_id`) REFERENCES `servicios_cobrables` (`id`),
  CONSTRAINT `FK4cmawu2md40dw79hkoevoedtq` FOREIGN KEY (`puesto_id`) REFERENCES `puestos` (`id`),
  CONSTRAINT `FKhtu2p8qbncn9w9j6i2shdvuqx` FOREIGN KEY (`socio_id`) REFERENCES `socios` (`id`),
  CONSTRAINT `FKk8xgduc4553um8ojxsqcg3vs2` FOREIGN KEY (`recibo_id`) REFERENCES `recibos` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE `recibos` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `categoria` varchar(100) DEFAULT NULL,
  `concepto` varchar(255) DEFAULT NULL,
  `correlativo` varchar(20) DEFAULT NULL,
  `depositante` varchar(150) DEFAULT NULL,
  `fecha` datetime(6) NOT NULL,
  `fecha_deposito` date DEFAULT NULL,
  `monto` decimal(12,2) NOT NULL,
  `tipo` enum('CANJE_BANCARIO','INGRESO_EXTERNO','PAGO_CUENTAS') NOT NULL,
  `banco_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKck8u0d4gm9ebeaqrohm91llsa` (`correlativo`),
  KEY `FKimom2545vktcxwiepasf4f99s` (`banco_id`),
  CONSTRAINT `FKimom2545vktcxwiepasf4f99s` FOREIGN KEY (`banco_id`) REFERENCES `bancos` (`id`)
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

CREATE TABLE `usuarios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `password` varchar(255) NOT NULL,
  `rol` enum('ADMIN','OPERADOR') NOT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKm2dvbwfge291euvmk6vkkocao` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
