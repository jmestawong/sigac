package com.cibertec.msauth.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.msauth.entidades.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);
}
