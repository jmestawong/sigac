package com.cibertec.sigac.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cibertec.sigac.dto.CargaMasivaEgresosResponse;
import com.cibertec.sigac.dto.EgresoRequest;
import com.cibertec.sigac.dto.EgresoResponse;
import com.cibertec.sigac.entity.Egreso;
import com.cibertec.sigac.entity.EstadoEgreso;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.EgresoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EgresoServiceImpl implements EgresoService {

    private static final String PREFIJO_CORRELATIVO = "E-";

    private final EgresoRepository egresoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EgresoResponse> listarTodos() {
        return egresoRepository.findAllByOrderByFechaDescIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EgresoResponse> listarPorRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new BusinessRuleException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return egresoRepository.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EgresoResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public EgresoResponse registrar(EgresoRequest request) {
        Egreso egreso = crearConCorrelativo(Egreso.builder()
                .numeroDocumento(request.getNumeroDocumento())
                .proveedor(request.getProveedor())
                .fecha(request.getFecha())
                .importe(request.getImporte())
                .documentoAsociado(request.getDocumentoAsociado())
                .motivo(request.getMotivo())
                .estado(EstadoEgreso.REGISTRADO)
                .build());

        return toResponse(egreso);
    }

    @Override
    public EgresoResponse procesar(Long id) {
        Egreso egreso = buscarOLanzar(id);
        validarTransicionDesdeRegistrado(egreso);
        egreso.setEstado(EstadoEgreso.PROCESADO);
        return toResponse(egresoRepository.save(egreso));
    }

    @Override
    public EgresoResponse anular(Long id) {
        Egreso egreso = buscarOLanzar(id);
        validarTransicionDesdeRegistrado(egreso);
        egreso.setEstado(EstadoEgreso.ANULADO);
        return toResponse(egresoRepository.save(egreso));
    }

    @Override
    public CargaMasivaEgresosResponse registrarMasivo(MultipartFile archivo) {
        List<EgresoResponse> creados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        int totalFilas = 0;

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || esFilaVacia(row)) {
                    continue;
                }

                totalFilas++;
                int numeroFilaHumano = i + 1;

                try {
                    EgresoRequest request = leerFila(row);
                    creados.add(registrar(request));
                } catch (RuntimeException ex) {
                    errores.add("Fila " + numeroFilaHumano + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("No se pudo leer el archivo: " + ex.getMessage());
        }

        return CargaMasivaEgresosResponse.builder()
                .totalFilas(totalFilas)
                .creados(creados)
                .errores(errores)
                .build();
    }

    private EgresoRequest leerFila(Row row) {
        String numeroDocumento = obtenerTexto(row, 0);
        String proveedor = obtenerTexto(row, 1);
        LocalDate fecha = obtenerFecha(row, 2);
        BigDecimal importe = obtenerImporte(row, 3);
        String documentoAsociado = obtenerTexto(row, 4);
        String motivo = obtenerTexto(row, 5);

        if (numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("el numero de documento (columna A) esta vacio");
        }
        if (proveedor.isBlank()) {
            throw new IllegalArgumentException("el proveedor (columna B) esta vacio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("la fecha (columna C) esta vacia o tiene un formato invalido");
        }
        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("el importe (columna D) esta vacio o no es mayor a 0");
        }
        if (motivo.isBlank()) {
            throw new IllegalArgumentException("el motivo (columna F) esta vacio");
        }

        return new EgresoRequest(
                numeroDocumento, proveedor, fecha, importe, documentoAsociado.isBlank() ? null : documentoAsociado, motivo);
    }

    private String obtenerTexto(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
            default -> "";
        };
    }

    private LocalDate obtenerFecha(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String texto = obtenerTexto(row, index);
            return texto.isBlank() ? null : LocalDate.parse(texto);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BigDecimal obtenerImporte(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            String texto = obtenerTexto(row, index);
            return texto.isBlank() ? null : new BigDecimal(texto);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private boolean esFilaVacia(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private void validarTransicionDesdeRegistrado(Egreso egreso) {
        if (egreso.getEstado() != EstadoEgreso.REGISTRADO) {
            throw new BusinessRuleException("El egreso " + egreso.getCorrelativo() + " ya esta "
                    + egreso.getEstado().name().toLowerCase() + " y no admite mas cambios de estado.");
        }
    }

    private Egreso crearConCorrelativo(Egreso egreso) {
        Egreso guardado = egresoRepository.save(egreso);
        guardado.setCorrelativo(PREFIJO_CORRELATIVO + String.format("%06d", guardado.getId()));
        return egresoRepository.save(guardado);
    }

    private Egreso buscarOLanzar(Long id) {
        return egresoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Egreso no encontrado con id " + id));
    }

    private EgresoResponse toResponse(Egreso egreso) {
        return EgresoResponse.builder()
                .id(egreso.getId())
                .correlativo(egreso.getCorrelativo())
                .numeroDocumento(egreso.getNumeroDocumento())
                .proveedor(egreso.getProveedor())
                .fecha(egreso.getFecha())
                .importe(egreso.getImporte())
                .documentoAsociado(egreso.getDocumentoAsociado())
                .motivo(egreso.getMotivo())
                .estado(egreso.getEstado())
                .build();
    }
}
