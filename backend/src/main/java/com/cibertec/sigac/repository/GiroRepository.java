package com.cibertec.sigac.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.Giro;

public interface GiroRepository extends JpaRepository<Giro, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
