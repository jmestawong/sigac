package com.cibertec.mscobranza.repositorio;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.EstadoCuenta;

public interface CuentaPorCobrarRepository extends JpaRepository<CuentaPorCobrar, Long> {

    List<CuentaPorCobrar> findAllByOrderByIdDesc();

    List<CuentaPorCobrar> findByEstado(EstadoCuenta estado);

    List<CuentaPorCobrar> findBySocioIdOrderByIdDesc(Long socioId);

    List<CuentaPorCobrar> findByPuestoIdOrderByIdDesc(Long puestoId);

    List<CuentaPorCobrar> findByPuestoIdInOrderByIdDesc(Collection<Long> puestoIds);

    List<CuentaPorCobrar> findByReciboId(Long reciboId);

    long countBySocioId(Long socioId);

    long countByPuestoId(Long puestoId);

    long countByServicioId(Long servicioId);
}
