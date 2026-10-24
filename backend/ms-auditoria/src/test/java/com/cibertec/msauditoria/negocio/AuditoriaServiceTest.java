package com.cibertec.msauditoria.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.msauditoria.entidades.RegistroAuditoria;
import com.cibertec.msauditoria.repositorio.RegistroAuditoriaRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaEvento;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private RegistroAuditoriaRepository repositorio;

    @InjectMocks
    private AuditoriaService auditoriaService;

    @Test
    void registrar_guardaTodosLosCamposDelEvento() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 20, 9, 30);
        AuditoriaEvento evento = AuditoriaEvento.builder().modulo("COBRANZA").accion("RECIBO_EMITIDO")
                .referencia("R-000007").detalle("2 cuentas").monto(new BigDecimal("80.00"))
                .usuario("operador").fecha(fecha).build();
        when(repositorio.save(any(RegistroAuditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        auditoriaService.registrar(evento);

        ArgumentCaptor<RegistroAuditoria> captor = ArgumentCaptor.forClass(RegistroAuditoria.class);
        verify(repositorio).save(captor.capture());
        RegistroAuditoria guardado = captor.getValue();
        assertThat(guardado.getModulo()).isEqualTo("COBRANZA");
        assertThat(guardado.getAccion()).isEqualTo("RECIBO_EMITIDO");
        assertThat(guardado.getReferencia()).isEqualTo("R-000007");
        assertThat(guardado.getUsuario()).isEqualTo("operador");
        assertThat(guardado.getFecha()).isEqualTo(fecha);
        assertThat(guardado.getMonto()).isEqualByComparingTo("80.00");
    }

    @Test
    void registrar_sinUsuarioNiFecha_usaSistemaYLaFechaActual() {
        AuditoriaEvento evento = AuditoriaEvento.builder().modulo("EGRESOS").accion("EGRESO_ANULADO").build();
        when(repositorio.save(any(RegistroAuditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroAuditoria guardado = auditoriaService.registrar(evento);

        assertThat(guardado.getUsuario()).isEqualTo("sistema");
        assertThat(guardado.getFecha()).isNotNull();
    }

    @Test
    void registrar_recortaLosTextosQueExcedenLaColumna() {
        AuditoriaEvento evento = AuditoriaEvento.builder().modulo("COBRANZA").accion("RECIBO_EMITIDO")
                .detalle("x".repeat(600)).usuario("operador").build();
        when(repositorio.save(any(RegistroAuditoria.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroAuditoria guardado = auditoriaService.registrar(evento);

        assertThat(guardado.getDetalle()).hasSize(400);
    }

    @Test
    void listar_sinFiltros_retornaTodo() {
        when(repositorio.findAllByOrderByIdDesc()).thenReturn(List.of(new RegistroAuditoria()));

        assertThat(auditoriaService.listar(null, null)).hasSize(1);
        verify(repositorio, never()).findByModuloIgnoreCaseOrderByIdDesc(any());
    }

    @Test
    void listar_conModulo_filtraPorModulo() {
        when(repositorio.findByModuloIgnoreCaseOrderByIdDesc("COBRANZA")).thenReturn(List.of(new RegistroAuditoria()));

        assertThat(auditoriaService.listar("COBRANZA", null)).hasSize(1);
    }

    @Test
    void listar_conUsuario_filtraPorUsuario() {
        when(repositorio.findByUsuarioIgnoreCaseOrderByIdDesc("admin")).thenReturn(List.of());

        assertThat(auditoriaService.listar(null, "admin")).isEmpty();
        verify(repositorio).findByUsuarioIgnoreCaseOrderByIdDesc("admin");
    }
}
