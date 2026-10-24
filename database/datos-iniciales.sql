-- ============================================================================
-- SIGAC - Datos iniciales (usuarios de demostracion de ms-auth)
--
-- Las contrasenas se almacenan cifradas con BCrypt (BCryptPasswordEncoder);
-- nunca en texto plano. Credenciales de demostracion:
--     admin     / admin123      (rol ADMIN)
--     operador  / operador123   (rol OPERADOR)
--
-- Nota: ms-auth crea estos dos usuarios automaticamente al arrancar si no
-- existen (clase DataSeeder), por lo que este script es opcional.
-- ============================================================================

USE sigac_auth;

INSERT INTO usuarios (username, password, rol) VALUES
  ('admin',    '$2a$10$K8QmP0jbwYoAYnD16Q.oCu/QcElSFIIeITAJ3zKVd9D.nuf8Np0V2', 'ADMIN'),
  ('operador', '$2a$10$BiFA23bEVZaVJcUxfIMf1eYyiZ3o3D7JCjTmqfUrbHNJFJyQ417Ym', 'OPERADOR');
