package com.cibertec.sigac.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.Socio;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
