package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.cibertec.sigac.dto.CargaMasivaEgresosResponse;
import com.cibertec.sigac.dto.EgresoRequest;
import com.cibertec.sigac.dto.EgresoResponse;
import com.cibertec.sigac.entity.Egreso;
import com.cibertec.sigac.entity.EstadoEgreso;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.EgresoRepository;

@ExtendWith(MockitoExtension.class)
class EgresoServiceTest {

    @Mock
    private EgresoRepository egresoRepository;

    @InjectMocks
    private EgresoServiceImpl egresoService;

    private Egreso egresoRegistrado;

    @BeforeEach
    void setUp() {
        egresoRegistrado = Egreso.builder()
                .id(1L).correlativo("E-000001").numeroDocumento("F-001").proveedor("Ferreteria Lima")
                .fecha(LocalDate.of(2026, 8, 1)).importe(new BigDecimal("150.00")).motivo("Compra de material")
                .estado(EstadoEgreso.REGISTRADO)
                .build();
    }

    @Test
    void registrar_debeAsignarCorrelativoYEstadoRegistrado() {
        EgresoRequest request = new EgresoRequest("F-002", "Proveedor X", LocalDate.of(2026, 8, 5),
                new BigDecimal("80.00"), null, "Mantenimiento");

        when(egresoRepository.save(any(Egreso.class))).thenAnswer(inv -> {
            Egreso e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(5L);
            }
            return e;
        });

        EgresoResponse resultado = egresoService.registrar(request);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEgreso.REGISTRADO);
        assertThat(resultado.getCorrelativo()).isEqualTo("E-000005");
    }

    @Test
    void procesar_debeTransicionarAProcesado() {
        when(egresoRepository.findById(1L)).thenReturn(Optional.of(egresoRegistrado));
        when(egresoRepository.save(any(Egreso.class))).thenAnswer(inv -> inv.getArgument(0));

        EgresoResponse resultado = egresoService.procesar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEgreso.PROCESADO);
    }

    @Test
    void anular_debeTransicionarAAnulado() {
        when(egresoRepository.findById(1L)).thenReturn(Optional.of(egresoRegistrado));
        when(egresoRepository.save(any(Egreso.class))).thenAnswer(inv -> inv.getArgument(0));

        EgresoResponse resultado = egresoService.anular(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoEgreso.ANULADO);
    }

    @Test
    void procesar_cuandoYaFueAnulado_debeLanzarExcepcion() {
        egresoRegistrado.setEstado(EstadoEgreso.ANULADO);
        when(egresoRepository.findById(1L)).thenReturn(Optional.of(egresoRegistrado));

        assertThatThrownBy(() -> egresoService.procesar(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void anular_cuandoYaFueProcesado_debeLanzarExcepcion() {
        egresoRegistrado.setEstado(EstadoEgreso.PROCESADO);
        when(egresoRepository.findById(1L)).thenReturn(Optional.of(egresoRegistrado));

        assertThatThrownBy(() -> egresoService.anular(1L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void procesar_cuandoNoExiste_debeLanzarExcepcion() {
        when(egresoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> egresoService.procesar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarPorRango_cuandoDesdeEsPosteriorAHasta_debeLanzarExcepcion() {
        assertThatThrownBy(() -> egresoService.listarPorRango(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void registrarMasivo_debeCrearFilasValidasYReportarLasInvalidas() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "egresos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", construirXlsxDePrueba());

        when(egresoRepository.save(any(Egreso.class))).thenAnswer(inv -> {
            Egreso e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId((long) (Math.random() * 1000 + 1));
            }
            return e;
        });

        CargaMasivaEgresosResponse resultado = egresoService.registrarMasivo(archivo);

        assertThat(resultado.getTotalFilas()).isEqualTo(2);
        assertThat(resultado.getCreados()).hasSize(1);
        assertThat(resultado.getCreados().get(0).getProveedor()).isEqualTo("Proveedor Valido");
        assertThat(resultado.getErrores()).hasSize(1);
        assertThat(resultado.getErrores().get(0)).contains("Fila 3");
    }

    /**
     * Fila 2: valida. Fila 3: importe vacio -> debe fallar sin abortar la fila valida.
     */
    private byte[] construirXlsxDePrueba() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Egresos");

            var header = sheet.createRow(0);
            String[] columnas = {"numeroDocumento", "proveedor", "fecha", "importe", "documentoAsociado", "motivo"};
            for (int i = 0; i < columnas.length; i++) {
                header.createCell(i).setCellValue(columnas[i]);
            }

            var filaValida = sheet.createRow(1);
            filaValida.createCell(0).setCellValue("F-100");
            filaValida.createCell(1).setCellValue("Proveedor Valido");
            filaValida.createCell(2).setCellValue("2026-08-01");
            filaValida.createCell(3).setCellValue(200.00);
            filaValida.createCell(5).setCellValue("Compra de utiles");

            var filaInvalida = sheet.createRow(2);
            filaInvalida.createCell(0).setCellValue("F-101");
            filaInvalida.createCell(1).setCellValue("Proveedor Invalido");
            filaInvalida.createCell(2).setCellValue("2026-08-02");
            // importe vacio a proposito
            filaInvalida.createCell(5).setCellValue("Motivo cualquiera");

            workbook.write(salida);
            return salida.toByteArray();
        }
    }
}
