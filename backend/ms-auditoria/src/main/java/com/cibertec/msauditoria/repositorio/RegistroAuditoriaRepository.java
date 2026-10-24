package com.cibertec.msauditoria.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cibertec.msauditoria.entidades.RegistroAuditoria;

public interface RegistroAuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {

    List<RegistroAuditoria> findAllByOrderByIdDesc();

    List<RegistroAuditoria> findByModuloIgnoreCaseOrderByIdDesc(String modulo);

    List<RegistroAuditoria> findByUsuarioIgnoreCaseOrderByIdDesc(String usuario);
}
