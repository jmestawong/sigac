package com.cibertec.mscatalogos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscatalogos.entidades.Puesto;

public interface PuestoRepository extends JpaRepository<Puesto, Long> {

    boolean existsByNumero(String numero);

    boolean existsByNumeroAndIdNot(String numero, Long id);
}
