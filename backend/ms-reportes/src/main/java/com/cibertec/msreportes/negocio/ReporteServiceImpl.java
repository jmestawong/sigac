package com.cibertec.msreportes.negocio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.cibertec.msreportes.client.CobranzaClient;
import com.cibertec.msreportes.client.EgresosClient;
import com.cibertec.msreportes.dto.CuentaPorCobrarResponse;
import com.cibertec.msreportes.dto.EgresoResponse;
import com.cibertec.msreportes.dto.EstadoEgreso;
import com.cibertec.msreportes.dto.ReciboResponse;
import com.cibertec.msreportes.dto.TipoRecibo;
import com.cibertec.sigac.common.exception.BusinessRuleException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private final CobranzaClient cobranzaClient;
    private final EgresosClient egresosClient;

    private record CuentaCobrada(ReciboResponse recibo, CuentaPorCobrarResponse cuenta) {
    }

    @Override
    public byte[] generarMovimientos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<ReciboResponse> recibos = recibosEnRango(desde, hasta);
        List<EgresoResponse> egresos = egresosEnRango(desde, hasta);

        return construirWorkbook(workbook -> {
            Sheet ingresos = crearHoja(workbook, "Ingresos", "Correlativo", "Tipo", "Fecha", "Destinatario", "Monto");
            int fila = 1;
            for (ReciboResponse recibo : recibos) {
                escribirFila(ingresos, fila++, recibo.getCorrelativo(), recibo.getTipo().name(),
                        recibo.getFecha().toLocalDate().toString(), destinatarioDe(recibo), recibo.getMonto());
            }
            autoAjustar(ingresos, 5);

            Sheet egresosSheet = crearHoja(workbook, "Egresos", "Correlativo", "Fecha", "Proveedor", "Motivo", "Importe", "Estado");
            fila = 1;
            for (EgresoResponse egreso : egresos) {
                escribirFila(egresosSheet, fila++, egreso.getCorrelativo(), egreso.getFecha().toString(),
                        egreso.getProveedor(), egreso.getMotivo(), egreso.getImporte(), egreso.getEstado().name());
            }
            autoAjustar(egresosSheet, 6);
        });
    }

    @Override
    public byte[] generarTotales(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<ReciboResponse> recibos = recibosEnRango(desde, hasta);
        List<EgresoResponse> egresos = egresosEnRango(desde, hasta);

        BigDecimal totalIngresos = sumar(recibos, ReciboResponse::getMonto);
        BigDecimal totalPagoCuentas = sumar(deTipo(recibos, TipoRecibo.PAGO_CUENTAS), ReciboResponse::getMonto);
        BigDecimal totalCanjeBancario = sumar(deTipo(recibos, TipoRecibo.CANJE_BANCARIO), ReciboResponse::getMonto);
        BigDecimal totalIngresoExterno = sumar(deTipo(recibos, TipoRecibo.INGRESO_EXTERNO), ReciboResponse::getMonto);

        BigDecimal totalEgresosValidos = sumar(
                egresos.stream().filter(e -> e.getEstado() != EstadoEgreso.ANULADO).toList(), EgresoResponse::getImporte);
        BigDecimal totalEgresosAnulados = sumar(
                egresos.stream().filter(e -> e.getEstado() == EstadoEgreso.ANULADO).toList(), EgresoResponse::getImporte);

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Totales", "Concepto", "Monto");
            int fila = 1;
            fila = escribirFila(sheet, fila, "Total ingresos", totalIngresos);
            fila = escribirFila(sheet, fila, "Total egresos (registrados + procesados)", totalEgresosValidos);
            fila = escribirFila(sheet, fila, "Neto (ingresos - egresos)", totalIngresos.subtract(totalEgresosValidos));
            fila++;
            fila = escribirFila(sheet, fila, "Ingresos por pago de cuentas", totalPagoCuentas);
            fila = escribirFila(sheet, fila, "Ingresos por canje bancario", totalCanjeBancario);
            fila = escribirFila(sheet, fila, "Ingresos externos", totalIngresoExterno);
            fila++;
            escribirFila(sheet, fila, "Egresos anulados (informativo, no afecta el neto)", totalEgresosAnulados);
            autoAjustar(sheet, 2);
        });
    }

    @Override
    public byte[] generarSocios(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<CuentaCobrada> cuentas = cuentasCobradasEnRango(desde, hasta).stream()
                .filter(c -> c.cuenta().getSocio() != null)
                .toList();

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Socios", "Socio", "Codigo", "Servicio", "Periodo", "Monto", "Fecha de pago", "Recibo");
            int fila = 1;
            for (CuentaCobrada cobrada : cuentas) {
                CuentaPorCobrarResponse cuenta = cobrada.cuenta();
                escribirFila(sheet, fila++,
                        cuenta.getSocio().getNombres() + " " + cuenta.getSocio().getApellidos(),
                        cuenta.getSocio().getCodigo(), nombreServicio(cuenta), cuenta.getPeriodo(),
                        cuenta.getMonto(), cobrada.recibo().getFecha().toLocalDate().toString(),
                        cobrada.recibo().getCorrelativo());
            }
            autoAjustar(sheet, 7);
        });
    }

    @Override
    public byte[] generarNoSocios(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<CuentaCobrada> cuentas = cuentasCobradasEnRango(desde, hasta).stream()
                .filter(c -> c.cuenta().getPuesto() != null)
                .toList();

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Puestos", "Puesto", "Inquilino", "Servicio", "Periodo", "Monto", "Fecha de pago", "Recibo");
            int fila = 1;
            for (CuentaCobrada cobrada : cuentas) {
                CuentaPorCobrarResponse cuenta = cobrada.cuenta();
                escribirFila(sheet, fila++,
                        cuenta.getPuesto().getNumero(), cuenta.getPuesto().getNombreInquilino(),
                        nombreServicio(cuenta), cuenta.getPeriodo(), cuenta.getMonto(),
                        cobrada.recibo().getFecha().toLocalDate().toString(), cobrada.recibo().getCorrelativo());
            }
            autoAjustar(sheet, 7);
        });
    }

    @Override
    public byte[] generarEgresos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<EgresoResponse> egresos = egresosEnRango(desde, hasta);

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Egresos", "Correlativo", "Numero documento", "Proveedor", "Fecha",
                    "Documento asociado", "Motivo", "Importe", "Estado");
            int fila = 1;
            for (EgresoResponse egreso : egresos) {
                escribirFila(sheet, fila++, egreso.getCorrelativo(), egreso.getNumeroDocumento(), egreso.getProveedor(),
                        egreso.getFecha().toString(), egreso.getDocumentoAsociado(), egreso.getMotivo(),
                        egreso.getImporte(), egreso.getEstado().name());
            }
            autoAjustar(sheet, 8);
        });
    }

    @Override
    public byte[] generarBancos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<ReciboResponse> recibos = deTipo(recibosEnRango(desde, hasta), TipoRecibo.CANJE_BANCARIO);

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Recibos bancarios", "Correlativo", "Fecha", "Banco", "Numero de cuenta",
                    "Fecha deposito", "Socio", "Monto");
            int fila = 1;
            for (ReciboResponse recibo : recibos) {
                escribirFila(sheet, fila++, recibo.getCorrelativo(), recibo.getFecha().toLocalDate().toString(),
                        recibo.getBanco() != null ? recibo.getBanco().getNombre() : "",
                        recibo.getBanco() != null ? recibo.getBanco().getNumeroCuenta() : "",
                        recibo.getFechaDeposito() != null ? recibo.getFechaDeposito().toString() : "",
                        nombreDelSocio(recibo), recibo.getMonto());
            }
            autoAjustar(sheet, 7);
        });
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new BusinessRuleException("Debes indicar las fechas 'desde' y 'hasta'");
        }
        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private List<ReciboResponse> recibosEnRango(LocalDate desde, LocalDate hasta) {
        return cobranzaClient.listarRecibos().stream()
                .filter(r -> !r.getFecha().toLocalDate().isBefore(desde) && !r.getFecha().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing(ReciboResponse::getFecha))
                .toList();
    }

    private List<EgresoResponse> egresosEnRango(LocalDate desde, LocalDate hasta) {
        return egresosClient.listarPorRango(desde, hasta).stream()
                .sorted(Comparator.comparing(EgresoResponse::getFecha))
                .toList();
    }

    private List<CuentaCobrada> cuentasCobradasEnRango(LocalDate desde, LocalDate hasta) {
        List<CuentaCobrada> cobradas = new ArrayList<>();
        for (ReciboResponse recibo : recibosEnRango(desde, hasta)) {
            for (CuentaPorCobrarResponse cuenta : recibo.getCuentas()) {
                cobradas.add(new CuentaCobrada(recibo, cuenta));
            }
        }
        return cobradas;
    }

    private List<ReciboResponse> deTipo(List<ReciboResponse> recibos, TipoRecibo tipo) {
        return recibos.stream().filter(r -> r.getTipo() == tipo).toList();
    }

    private String destinatarioDe(ReciboResponse recibo) {
        return switch (recibo.getTipo()) {
            case INGRESO_EXTERNO -> recibo.getDepositante();
            case CANJE_BANCARIO -> nombreDelSocio(recibo);
            case PAGO_CUENTAS -> recibo.getCuentas().size() + " cuenta(s)";
        };
    }

    private String nombreDelSocio(ReciboResponse recibo) {
        return recibo.getCuentas().stream()
                .filter(c -> c.getSocio() != null)
                .findFirst()
                .map(c -> c.getSocio().getNombres() + " " + c.getSocio().getApellidos())
                .orElse("");
    }

    private String nombreServicio(CuentaPorCobrarResponse cuenta) {
        return cuenta.getServicio() != null ? cuenta.getServicio().getNombre() : "";
    }

    private <T> BigDecimal sumar(List<T> lista, Function<T, BigDecimal> extractor) {
        return lista.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] construirWorkbook(Consumer<XSSFWorkbook> constructor) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            constructor.accept(workbook);
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el reporte", ex);
        }
    }

    private Sheet crearHoja(XSSFWorkbook workbook, String nombre, String... columnas) {
        Sheet sheet = workbook.createSheet(nombre);
        CellStyle estilo = estiloEncabezado(workbook);
        Row header = sheet.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(estilo);
        }
        return sheet;
    }

    private CellStyle estiloEncabezado(XSSFWorkbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }

    private int escribirFila(Sheet sheet, int indiceFila, Object... valores) {
        Row row = sheet.createRow(indiceFila);
        for (int i = 0; i < valores.length; i++) {
            Cell cell = row.createCell(i);
            Object valor = valores[i];
            if (valor == null) {
                cell.setBlank();
            } else if (valor instanceof BigDecimal bigDecimal) {
                cell.setCellValue(bigDecimal.doubleValue());
            } else if (valor instanceof Number numero) {
                cell.setCellValue(numero.doubleValue());
            } else {
                cell.setCellValue(valor.toString());
            }
        }
        return indiceFila + 1;
    }

    private void autoAjustar(Sheet sheet, int columnas) {
        for (int i = 0; i < columnas; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
