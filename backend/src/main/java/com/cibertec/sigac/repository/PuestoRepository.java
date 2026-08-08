package com.cibertec.sigac.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.Puesto;

public interface PuestoRepository extends JpaRepository<Puesto, Long> {

    boolean existsByNumero(String numero);

    boolean existsByNumeroAndIdNot(String numero, Long id);
}
