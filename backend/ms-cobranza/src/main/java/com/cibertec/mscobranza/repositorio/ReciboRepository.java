package com.cibertec.mscobranza.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscobranza.entidades.Recibo;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {

    List<Recibo> findAllByOrderByIdDesc();

    long countByBancoId(Long bancoId);
}
