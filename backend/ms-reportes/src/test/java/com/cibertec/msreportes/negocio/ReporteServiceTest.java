package com.cibertec.msreportes.negocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.msreportes.client.CobranzaClient;
import com.cibertec.msreportes.client.EgresosClient;
import com.cibertec.msreportes.dto.BancoResponse;
import com.cibertec.msreportes.dto.CuentaPorCobrarResponse;
import com.cibertec.msreportes.dto.EgresoResponse;
import com.cibertec.msreportes.dto.EstadoCuenta;
import com.cibertec.msreportes.dto.EstadoEgreso;
import com.cibertec.msreportes.dto.GiroResponse;
import com.cibertec.msreportes.dto.Moneda;
import com.cibertec.msreportes.dto.PuestoResponse;
import com.cibertec.msreportes.dto.ReciboResponse;
import com.cibertec.msreportes.dto.ServicioCobrableResponse;
import com.cibertec.msreportes.dto.SocioResponse;
import com.cibertec.msreportes.dto.TipoRecibo;
import com.cibertec.sigac.common.exception.BusinessRuleException;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 8, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 8, 31);

    @Mock
    private CobranzaClient cobranzaClient;

    @Mock
    private EgresosClient egresosClient;

    @InjectMocks
    private ReporteServiceImpl reporteService;

    private EgresoResponse egreso() {
        return EgresoResponse.builder()
                .id(1L).correlativo("E-000001").numeroDocumento("F-001").proveedor("Proveedor X")
                .fecha(LocalDate.of(2026, 8, 5)).importe(new BigDecimal("100.00")).motivo("Compra")
                .estado(EstadoEgreso.PROCESADO).build();
    }

    private CuentaPorCobrarResponse cuentaDeSocio() {
        return CuentaPorCobrarResponse.builder()
                .id(1L).estado(EstadoCuenta.ABONADA).monto(new BigDecimal("50.00")).periodo("2026")
                .socio(SocioResponse.builder().id(1L).codigo("S-001").nombres("Ana").apellidos("Torres").build())
                .servicio(ServicioCobrableResponse.builder().id(1L).nombre("Cuota").build())
                .reciboCorrelativo("R-000001").build();
    }

    private CuentaPorCobrarResponse cuentaDePuesto() {
        return CuentaPorCobrarResponse.builder()
                .id(2L).estado(EstadoCuenta.ABONADA).monto(new BigDecimal("30.00")).periodo("2026-08")
                .puesto(PuestoResponse.builder().id(1L).numero("P-01").nombreInquilino("Juan Perez")
                        .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).build())
                .servicio(ServicioCobrableResponse.builder().id(2L).nombre("Agua").build())
                .reciboCorrelativo("R-000002").build();
    }

    private ReciboResponse recibo(long id, TipoRecibo tipo, String monto, int dia, List<CuentaPorCobrarResponse> cuentas) {
        return ReciboResponse.builder()
                .id(id).correlativo("R-%06d".formatted(id)).tipo(tipo).fecha(LocalDateTime.of(2026, 8, dia, 10, 0))
                .monto(new BigDecimal(monto)).cuentas(cuentas).depositante("Municipalidad").build();
    }

    @Test
    void generarMovimientos_debeProducirUnXlsxValidoConLasDosHojas() throws Exception {
        when(cobranzaClient.listarRecibos()).thenReturn(List.of());
        when(egresosClient.listarPorRango(DESDE, HASTA)).thenReturn(List.of(egreso()));

        byte[] contenido = reporteService.generarMovimientos(DESDE, HASTA);

        assertThat(contenido).isNotEmpty();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheet("Ingresos")).isNotNull();
            Sheet hojaEgresos = workbook.getSheet("Egresos");
            assertThat(hojaEgresos.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Correlativo");
            assertThat(hojaEgresos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("E-000001");
            assertThat(hojaEgresos.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(100.00);
        }
    }

    @Test
    void generarMovimientos_soloIncluyeLosRecibosDentroDelRango() throws Exception {
        ReciboResponse dentro = recibo(1, TipoRecibo.INGRESO_EXTERNO, "500.00", 10, List.of());
        ReciboResponse fuera = ReciboResponse.builder().id(2L).correlativo("R-000002").tipo(TipoRecibo.INGRESO_EXTERNO)
                .fecha(LocalDateTime.of(2026, 9, 2, 10, 0)).monto(new BigDecimal("999")).cuentas(List.of()).build();
        when(cobranzaClient.listarRecibos()).thenReturn(List.of(fuera, dentro));
        when(egresosClient.listarPorRango(DESDE, HASTA)).thenReturn(List.of());

        byte[] contenido = reporteService.generarMovimientos(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet ingresos = workbook.getSheet("Ingresos");
            assertThat(ingresos.getLastRowNum()).isEqualTo(1);
            assertThat(ingresos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("R-000001");
            assertThat(ingresos.getRow(1).getCell(3).getStringCellValue()).isEqualTo("Municipalidad");
        }
    }

    @Test
    void generarTotales_sumaIngresosYEgresosYCalculaElNeto() throws Exception {
        when(cobranzaClient.listarRecibos()).thenReturn(List.of(
                recibo(1, TipoRecibo.PAGO_CUENTAS, "80.00", 3, List.of(cuentaDeSocio())),
                recibo(2, TipoRecibo.INGRESO_EXTERNO, "20.00", 4, List.of())));
        when(egresosClient.listarPorRango(DESDE, HASTA)).thenReturn(List.of(egreso()));

        byte[] contenido = reporteService.generarTotales(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = workbook.getSheet("Totales");
            assertThat(hoja.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(100.00); // ingresos
            assertThat(hoja.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(100.00); // egresos validos
            assertThat(hoja.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(0.00); // neto
        }
    }

    @Test
    void generarSocios_listaSoloLasCuentasDeSocioCobradas() throws Exception {
        when(cobranzaClient.listarRecibos()).thenReturn(List.of(
                recibo(1, TipoRecibo.PAGO_CUENTAS, "80.00", 3, List.of(cuentaDeSocio(), cuentaDePuesto()))));

        byte[] contenido = reporteService.generarSocios(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = workbook.getSheet("Socios");
            assertThat(hoja.getLastRowNum()).isEqualTo(1);
            assertThat(hoja.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ana Torres");
            assertThat(hoja.getRow(1).getCell(6).getStringCellValue()).isEqualTo("R-000001");
        }
    }

    @Test
    void generarNoSocios_listaSoloLasCuentasDePuestoCobradas() throws Exception {
        when(cobranzaClient.listarRecibos()).thenReturn(List.of(
                recibo(1, TipoRecibo.PAGO_CUENTAS, "80.00", 3, List.of(cuentaDeSocio(), cuentaDePuesto()))));

        byte[] contenido = reporteService.generarNoSocios(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = workbook.getSheet("Puestos");
            assertThat(hoja.getLastRowNum()).isEqualTo(1);
            assertThat(hoja.getRow(1).getCell(0).getStringCellValue()).isEqualTo("P-01");
            assertThat(hoja.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Juan Perez");
        }
    }

    @Test
    void generarBancos_listaSoloLosCanjesBancariosConSuBanco() throws Exception {
        ReciboResponse canje = recibo(3, TipoRecibo.CANJE_BANCARIO, "50.00", 8, List.of(cuentaDeSocio()));
        canje.setBanco(BancoResponse.builder().id(1L).nombre("BCP").numeroCuenta("193-1").moneda(Moneda.PEN).build());
        canje.setFechaDeposito(LocalDate.of(2026, 8, 7));
        when(cobranzaClient.listarRecibos()).thenReturn(List.of(
                canje, recibo(4, TipoRecibo.INGRESO_EXTERNO, "10.00", 9, List.of())));

        byte[] contenido = reporteService.generarBancos(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = workbook.getSheet("Recibos bancarios");
            assertThat(hoja.getLastRowNum()).isEqualTo(1);
            assertThat(hoja.getRow(1).getCell(2).getStringCellValue()).isEqualTo("BCP");
            assertThat(hoja.getRow(1).getCell(5).getStringCellValue()).isEqualTo("Ana Torres");
        }
    }

    @Test
    void generarEgresos_incluyeTodasLasColumnas() throws Exception {
        when(egresosClient.listarPorRango(DESDE, HASTA)).thenReturn(List.of(egreso()));

        byte[] contenido = reporteService.generarEgresos(DESDE, HASTA);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet hoja = workbook.getSheet("Egresos");
            assertThat(hoja.getRow(0).getLastCellNum()).isEqualTo((short) 8);
            assertThat(hoja.getRow(1).getCell(7).getStringCellValue()).isEqualTo("PROCESADO");
        }
    }

    @Test
    void generar_cuandoDesdeEsPosteriorAHasta_debeLanzarExcepcion() {
        assertThatThrownBy(() -> reporteService.generarEgresos(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void generar_sinFechas_debeLanzarExcepcion() {
        assertThatThrownBy(() -> reporteService.generarTotales(null, HASTA)).isInstanceOf(BusinessRuleException.class);
    }
}
