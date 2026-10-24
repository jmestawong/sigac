package com.cibertec.mscatalogos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscatalogos.entidades.ServicioCobrable;

public interface ServicioCobrableRepository extends JpaRepository<ServicioCobrable, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
