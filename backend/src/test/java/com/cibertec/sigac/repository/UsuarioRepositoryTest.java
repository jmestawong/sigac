package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.cibertec.sigac.entity.Rol;
import com.cibertec.sigac.entity.Usuario;

/**
 * Pruebas de la capa de acceso a datos de Usuario (base del servicio de login):
 * insertar, listar, actualizar y eliminar, mas la consulta por username y la
 * verificacion de que el password se persiste cifrado con BCrypt.
 */
@DataJpaTest
class UsuarioRepositoryTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario nuevoUsuario(String username, String passwordPlano, Rol rol) {
        return Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(passwordPlano))
                .rol(rol)
                .build();
    }

    @Test
    void insertar_debePersistirElUsuarioConIdGenerado() {
        Usuario guardado = usuarioRepository.save(nuevoUsuario("cajero1", "clave123", Rol.OPERADOR));

        assertThat(guardado.getId()).isNotNull();
        assertThat(usuarioRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void insertar_debeAlmacenarElPasswordCifradoYNoEnTextoPlano() {
        usuarioRepository.save(nuevoUsuario("cajero2", "clave123", Rol.OPERADOR));

        Usuario persistido = usuarioRepository.findByUsername("cajero2").orElseThrow();

        assertThat(persistido.getPassword()).isNotEqualTo("clave123");
        assertThat(persistido.getPassword()).startsWith("$2");
        assertThat(passwordEncoder.matches("clave123", persistido.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("otra-clave", persistido.getPassword())).isFalse();
    }

    @Test
    void listar_debeRetornarTodosLosUsuariosPersistidos() {
        usuarioRepository.save(nuevoUsuario("admin1", "a1", Rol.ADMIN));
        usuarioRepository.save(nuevoUsuario("cajero3", "a2", Rol.OPERADOR));

        List<Usuario> usuarios = usuarioRepository.findAll();

        assertThat(usuarios).extracting(Usuario::getUsername).contains("admin1", "cajero3");
    }

    @Test
    void actualizar_debeModificarElRolYElPassword() {
        Usuario guardado = usuarioRepository.save(nuevoUsuario("cajero4", "vieja", Rol.OPERADOR));

        guardado.setRol(Rol.ADMIN);
        guardado.setPassword(passwordEncoder.encode("nueva"));
        usuarioRepository.save(guardado);

        Usuario actualizado = usuarioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getRol()).isEqualTo(Rol.ADMIN);
        assertThat(passwordEncoder.matches("nueva", actualizado.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("vieja", actualizado.getPassword())).isFalse();
    }

    @Test
    void eliminar_debeRemoverElUsuarioDeLaBaseDeDatos() {
        Usuario guardado = usuarioRepository.save(nuevoUsuario("cajero5", "clave", Rol.OPERADOR));
        Long id = guardado.getId();

        usuarioRepository.deleteById(id);

        assertThat(usuarioRepository.findById(id)).isEmpty();
    }

    @Test
    void findByUsername_debeEncontrarAlUsuarioExistenteYNoAlInexistente() {
        usuarioRepository.save(nuevoUsuario("cajero6", "clave", Rol.OPERADOR));

        Optional<Usuario> existente = usuarioRepository.findByUsername("cajero6");
        Optional<Usuario> inexistente = usuarioRepository.findByUsername("no-existe");

        assertThat(existente).isPresent();
        assertThat(existente.get().getRol()).isEqualTo(Rol.OPERADOR);
        assertThat(inexistente).isEmpty();
    }

    @Test
    void existsByUsername_debeDetectarUsuariosDuplicados() {
        usuarioRepository.save(nuevoUsuario("cajero7", "clave", Rol.OPERADOR));

        assertThat(usuarioRepository.existsByUsername("cajero7")).isTrue();
        assertThat(usuarioRepository.existsByUsername("fantasma")).isFalse();
    }

    @Test
    void insertar_conUsernameDuplicado_debeFallarPorRestriccionUnica() {
        usuarioRepository.saveAndFlush(nuevoUsuario("repetido", "clave", Rol.OPERADOR));

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(nuevoUsuario("repetido", "otra", Rol.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
