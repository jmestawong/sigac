package com.cibertec.sigac.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.entity.CuentaPorCobrar;
import com.cibertec.sigac.entity.Egreso;
import com.cibertec.sigac.entity.EstadoCuenta;
import com.cibertec.sigac.entity.EstadoEgreso;
import com.cibertec.sigac.entity.Recibo;
import com.cibertec.sigac.entity.TipoRecibo;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.repository.CuentaPorCobrarRepository;
import com.cibertec.sigac.repository.EgresoRepository;
import com.cibertec.sigac.repository.ReciboRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {

    private final ReciboRepository reciboRepository;
    private final EgresoRepository egresoRepository;
    private final CuentaPorCobrarRepository cuentaPorCobrarRepository;

    @Override
    public byte[] generarMovimientos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Recibo> recibos = recibosEnRango(desde, hasta);
        List<Egreso> egresos = egresosEnRango(desde, hasta);

        return construirWorkbook(workbook -> {
            Sheet ingresos = crearHoja(workbook, "Ingresos", "Correlativo", "Tipo", "Fecha", "Destinatario", "Monto");
            int fila = 1;
            for (Recibo recibo : recibos) {
                escribirFila(ingresos, fila++, recibo.getCorrelativo(), recibo.getTipo().name(),
                        recibo.getFecha().toLocalDate().toString(), destinatarioDe(recibo), recibo.getMonto());
            }
            autoAjustar(ingresos, 5);

            Sheet egresosSheet = crearHoja(workbook, "Egresos", "Correlativo", "Fecha", "Proveedor", "Motivo", "Importe", "Estado");
            fila = 1;
            for (Egreso egreso : egresos) {
                escribirFila(egresosSheet, fila++, egreso.getCorrelativo(), egreso.getFecha().toString(),
                        egreso.getProveedor(), egreso.getMotivo(), egreso.getImporte(), egreso.getEstado().name());
            }
            autoAjustar(egresosSheet, 6);
        });
    }

    @Override
    public byte[] generarTotales(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Recibo> recibos = recibosEnRango(desde, hasta);
        List<Egreso> egresos = egresosEnRango(desde, hasta);

        BigDecimal totalIngresos = sumar(recibos, Recibo::getMonto);
        BigDecimal totalPagoCuentas = sumar(recibos.stream().filter(r -> r.getTipo() == TipoRecibo.PAGO_CUENTAS).toList(), Recibo::getMonto);
        BigDecimal totalCanjeBancario = sumar(recibos.stream().filter(r -> r.getTipo() == TipoRecibo.CANJE_BANCARIO).toList(), Recibo::getMonto);
        BigDecimal totalIngresoExterno = sumar(recibos.stream().filter(r -> r.getTipo() == TipoRecibo.INGRESO_EXTERNO).toList(), Recibo::getMonto);

        BigDecimal totalEgresosValidos = sumar(
                egresos.stream().filter(e -> e.getEstado() != EstadoEgreso.ANULADO).toList(), Egreso::getImporte);
        BigDecimal totalEgresosAnulados = sumar(
                egresos.stream().filter(e -> e.getEstado() == EstadoEgreso.ANULADO).toList(), Egreso::getImporte);

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
        List<CuentaPorCobrar> cuentas = cuentasAbonadasEnRango(desde, hasta).stream()
                .filter(c -> c.getSocio() != null)
                .toList();

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Socios", "Socio", "Codigo", "Servicio", "Periodo", "Monto", "Fecha de pago", "Recibo");
            int fila = 1;
            for (CuentaPorCobrar cuenta : cuentas) {
                escribirFila(sheet, fila++,
                        cuenta.getSocio().getNombres() + " " + cuenta.getSocio().getApellidos(),
                        cuenta.getSocio().getCodigo(), cuenta.getServicio().getNombre(), cuenta.getPeriodo(),
                        cuenta.getMonto(), cuenta.getRecibo().getFecha().toLocalDate().toString(),
                        cuenta.getRecibo().getCorrelativo());
            }
            autoAjustar(sheet, 7);
        });
    }

    @Override
    public byte[] generarNoSocios(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<CuentaPorCobrar> cuentas = cuentasAbonadasEnRango(desde, hasta).stream()
                .filter(c -> c.getPuesto() != null)
                .toList();

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Puestos", "Puesto", "Inquilino", "Servicio", "Periodo", "Monto", "Fecha de pago", "Recibo");
            int fila = 1;
            for (CuentaPorCobrar cuenta : cuentas) {
                escribirFila(sheet, fila++,
                        cuenta.getPuesto().getNumero(), cuenta.getPuesto().getNombreInquilino(),
                        cuenta.getServicio().getNombre(), cuenta.getPeriodo(), cuenta.getMonto(),
                        cuenta.getRecibo().getFecha().toLocalDate().toString(), cuenta.getRecibo().getCorrelativo());
            }
            autoAjustar(sheet, 7);
        });
    }

    @Override
    public byte[] generarEgresos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        List<Egreso> egresos = egresosEnRango(desde, hasta);

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Egresos", "Correlativo", "Numero documento", "Proveedor", "Fecha",
                    "Documento asociado", "Motivo", "Importe", "Estado");
            int fila = 1;
            for (Egreso egreso : egresos) {
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
        List<Recibo> recibos = recibosEnRango(desde, hasta).stream()
                .filter(r -> r.getTipo() == TipoRecibo.CANJE_BANCARIO)
                .toList();

        return construirWorkbook(workbook -> {
            Sheet sheet = crearHoja(workbook, "Recibos bancarios", "Correlativo", "Fecha", "Banco", "Numero de cuenta",
                    "Fecha deposito", "Socio", "Monto");
            int fila = 1;
            for (Recibo recibo : recibos) {
                List<CuentaPorCobrar> cuentas = cuentaPorCobrarRepository.findByReciboId(recibo.getId());
                String socio = cuentas.stream().findFirst()
                        .map(c -> c.getSocio().getNombres() + " " + c.getSocio().getApellidos())
                        .orElse("");
                escribirFila(sheet, fila++, recibo.getCorrelativo(), recibo.getFecha().toLocalDate().toString(),
                        recibo.getBanco() != null ? recibo.getBanco().getNombre() : "",
                        recibo.getBanco() != null ? recibo.getBanco().getNumeroCuenta() : "",
                        recibo.getFechaDeposito() != null ? recibo.getFechaDeposito().toString() : "",
                        socio, recibo.getMonto());
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

    private List<Recibo> recibosEnRango(LocalDate desde, LocalDate hasta) {
        return reciboRepository.findAllByOrderByIdDesc().stream()
                .filter(r -> !r.getFecha().toLocalDate().isBefore(desde) && !r.getFecha().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing(Recibo::getFecha))
                .toList();
    }

    private List<Egreso> egresosEnRango(LocalDate desde, LocalDate hasta) {
        return egresoRepository.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta).stream()
                .sorted(Comparator.comparing(Egreso::getFecha))
                .toList();
    }

    private List<CuentaPorCobrar> cuentasAbonadasEnRango(LocalDate desde, LocalDate hasta) {
        return cuentaPorCobrarRepository.findByEstado(EstadoCuenta.ABONADA).stream()
                .filter(c -> c.getRecibo() != null)
                .filter(c -> !c.getRecibo().getFecha().toLocalDate().isBefore(desde)
                        && !c.getRecibo().getFecha().toLocalDate().isAfter(hasta))
                .sorted(Comparator.comparing(c -> c.getRecibo().getFecha()))
                .toList();
    }

    private String destinatarioDe(Recibo recibo) {
        return switch (recibo.getTipo()) {
            case INGRESO_EXTERNO -> recibo.getDepositante();
            case CANJE_BANCARIO -> cuentaPorCobrarRepository.findByReciboId(recibo.getId()).stream().findFirst()
                    .map(c -> c.getSocio().getNombres() + " " + c.getSocio().getApellidos())
                    .orElse("");
            case PAGO_CUENTAS -> cuentaPorCobrarRepository.findByReciboId(recibo.getId()).size() + " cuenta(s)";
        };
    }

    private <T> BigDecimal sumar(List<T> lista, java.util.function.Function<T, BigDecimal> extractor) {
        return lista.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private byte[] construirWorkbook(java.util.function.Consumer<XSSFWorkbook> constructor) {
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
