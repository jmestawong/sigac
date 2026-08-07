package com.cibertec.sigac.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.cibertec.sigac.entity.Rol;
import com.cibertec.sigac.entity.Usuario;
import com.cibertec.sigac.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearUsuarioSiNoExiste("admin", "admin123", Rol.ADMIN);
        crearUsuarioSiNoExiste("operador", "operador123", Rol.OPERADOR);
    }

    private void crearUsuarioSiNoExiste(String username, String rawPassword, Rol rol) {
        if (usuarioRepository.existsByUsername(username)) {
            return;
        }

        Usuario usuario = Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .rol(rol)
                .build();

        usuarioRepository.save(usuario);
        log.info("Usuario inicial creado: {} / rol {}", username, rol);
    }
}
