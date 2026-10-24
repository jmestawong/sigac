package com.cibertec.msauditoria.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.msauditoria.entidades.RegistroAuditoria;

@DataJpaTest
class RegistroAuditoriaRepositoryTest {

    @Autowired
    private RegistroAuditoriaRepository repositorio;

    private RegistroAuditoria registro(String modulo, String accion, String usuario) {
        return RegistroAuditoria.builder()
                .modulo(modulo).accion(accion).referencia("R-000001").detalle("detalle")
                .monto(new BigDecimal("50.00")).usuario(usuario).fecha(LocalDateTime.of(2026, 10, 20, 9, 0))
                .build();
    }

    @Test
    void insertar_debePersistirElRegistro() {
        RegistroAuditoria guardado = repositorio.save(registro("COBRANZA", "RECIBO_EMITIDO", "operador"));

        RegistroAuditoria persistido = repositorio.findById(guardado.getId()).orElseThrow();

        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.getUsuario()).isEqualTo("operador");
        assertThat(persistido.getMonto()).isEqualByComparingTo("50.00");
    }

    @Test
    void listar_debeRetornarDelMasRecienteAlMasAntiguo() {
        RegistroAuditoria primero = repositorio.save(registro("COBRANZA", "RECIBO_EMITIDO", "operador"));
        RegistroAuditoria segundo = repositorio.save(registro("EGRESOS", "EGRESO_REGISTRADO", "admin"));

        assertThat(repositorio.findAllByOrderByIdDesc()).extracting(RegistroAuditoria::getId)
                .containsExactly(segundo.getId(), primero.getId());
    }

    @Test
    void actualizar_debeCambiarElDetalle() {
        RegistroAuditoria guardado = repositorio.save(registro("COBRANZA", "RECIBO_EMITIDO", "operador"));

        guardado.setDetalle("detalle corregido");
        repositorio.save(guardado);

        assertThat(repositorio.findById(guardado.getId()).orElseThrow().getDetalle()).isEqualTo("detalle corregido");
    }

    @Test
    void eliminar_debeRemoverElRegistro() {
        RegistroAuditoria guardado = repositorio.save(registro("COBRANZA", "RECIBO_EMITIDO", "operador"));

        repositorio.deleteById(guardado.getId());

        assertThat(repositorio.findById(guardado.getId())).isEmpty();
    }

    @Test
    void filtros_porModuloYPorUsuario_ignoranMayusculas() {
        repositorio.save(registro("COBRANZA", "RECIBO_EMITIDO", "operador"));
        repositorio.save(registro("EGRESOS", "EGRESO_REGISTRADO", "admin"));

        List<RegistroAuditoria> deEgresos = repositorio.findByModuloIgnoreCaseOrderByIdDesc("egresos");
        List<RegistroAuditoria> deOperador = repositorio.findByUsuarioIgnoreCaseOrderByIdDesc("OPERADOR");

        assertThat(deEgresos).extracting(RegistroAuditoria::getAccion).containsExactly("EGRESO_REGISTRADO");
        assertThat(deOperador).extracting(RegistroAuditoria::getAccion).containsExactly("RECIBO_EMITIDO");
    }
}
