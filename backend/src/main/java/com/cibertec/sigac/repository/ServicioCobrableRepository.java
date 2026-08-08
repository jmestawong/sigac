package com.cibertec.sigac.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.ServicioCobrable;

public interface ServicioCobrableRepository extends JpaRepository<ServicioCobrable, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
