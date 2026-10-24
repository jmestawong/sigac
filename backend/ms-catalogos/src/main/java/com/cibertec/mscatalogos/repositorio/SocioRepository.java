package com.cibertec.mscatalogos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscatalogos.entidades.Socio;

public interface SocioRepository extends JpaRepository<Socio, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
