package com.cibertec.mscatalogos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscatalogos.entidades.Giro;

public interface GiroRepository extends JpaRepository<Giro, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
