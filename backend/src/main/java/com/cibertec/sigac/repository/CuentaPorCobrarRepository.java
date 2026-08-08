package com.cibertec.sigac.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.CuentaPorCobrar;

public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    List<CuentaPorCobrar> findAllByOrderByIdDesc();

    List<CuentaPorCobrar> findBySocioIdOrderByIdDesc(Long socioId);

    List<CuentaPorCobrar> findByPuestoIdOrderByIdDesc(Long puestoId);

    List<CuentaPorCobrar> findByPuesto_Socio_IdOrderByIdDesc(Long socioId);

    List<CuentaPorCobrar> findByReciboId(Long reciboId);
}
