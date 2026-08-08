package com.cibertec.sigac.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.CuentaPorCobrar;

public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    List<CuentaPorCobrar> findAllByOrderByIdDesc();
}
