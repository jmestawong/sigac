package com.cibertec.mscatalogos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.mscatalogos.entidades.Banco;

public interface BancoRepository extends JpaRepository<Banco, Long> {

    boolean existsByNumeroCuenta(String numeroCuenta);

    boolean existsByNumeroCuentaAndIdNot(String numeroCuenta, Long id);

    boolean existsByCci(String cci);

    boolean existsByCciAndIdNot(String cci, Long id);
}
