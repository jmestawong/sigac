package com.cibertec.sigac.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.Egreso;

public interface EgresoRepository extends JpaRepository<Egreso, Long> {

    List<Egreso> findAllByOrderByFechaDescIdDesc();

    List<Egreso> findByFechaBetweenOrderByFechaDescIdDesc(LocalDate desde, LocalDate hasta);
}
