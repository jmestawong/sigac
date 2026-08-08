package com.cibertec.sigac.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.sigac.entity.Recibo;

public interface ReciboRepository extends JpaRepository<Recibo, Long> {

    List<Recibo> findAllByOrderByIdDesc();
}
