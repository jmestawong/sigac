package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.sigac.entity.Egreso;
import com.cibertec.sigac.entity.EstadoEgreso;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.EgresoRepository;
import com.cibertec.sigac.repository.ReciboRepository;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReciboRepository reciboRepository;

    @Mock
    private EgresoRepository egresoRepository;

    @Mock
    private CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @InjectMocks
    private ReporteServiceImpl reporteService;

    @Test
    void generarMovimientos_debeProducirUnXlsxValidoConLasDosHojas() throws Exception {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);

        Egreso egreso = Egreso.builder()
                .id(1L).correlativo("E-000001").numeroDocumento("F-001").proveedor("Proveedor X")
                .fecha(LocalDate.of(2026, 8, 5)).importe(new BigDecimal("100.00")).motivo("Compra")
                .estado(EstadoEgreso.PROCESADO).build();

        when(reciboRepository.findAllByOrderByIdDesc()).thenReturn(List.of());
        when(egresoRepository.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta)).thenReturn(List.of(egreso));

        byte[] contenido = reporteService.generarMovimientos(desde, hasta);

        assertThat(contenido).isNotEmpty();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheet("Ingresos")).isNotNull();
            assertThat(workbook.getSheet("Egresos")).isNotNull();

            var hojaEgresos = workbook.getSheet("Egresos");
            assertThat(hojaEgresos.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Correlativo");
            assertThat(hojaEgresos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("E-000001");
            assertThat(hojaEgresos.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(100.00);
        }
    }

    @Test
    void generarTotales_debeProducirUnXlsxValidoConLaHojaTotales() throws Exception {
        LocalDate desde = LocalDate.of(2026, 8, 1);
        LocalDate hasta = LocalDate.of(2026, 8, 31);

        when(reciboRepository.findAllByOrderByIdDesc()).thenReturn(List.of());
        when(egresoRepository.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta)).thenReturn(List.of());

        byte[] contenido = reporteService.generarTotales(desde, hasta);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            assertThat(workbook.getSheet("Totales")).isNotNull();
        }
    }

    @Test
    void generar_cuandoDesdeEsPosteriorAHasta_debeLanzarExcepcion() {
        assertThatThrownBy(() -> reporteService.generarEgresos(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
