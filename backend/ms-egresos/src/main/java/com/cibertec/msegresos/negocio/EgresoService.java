package com.cibertec.msegresos.negocio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cibertec.msegresos.dto.CargaMasivaEgresosResponse;
import com.cibertec.msegresos.dto.EgresoRequest;
import com.cibertec.msegresos.dto.EgresoResponse;

public interface EgresoService {

    List<EgresoResponse> listarTodos();

    List<EgresoResponse> listarPorRango(LocalDate desde, LocalDate hasta);

    EgresoResponse obtenerPorId(Long id);

    EgresoResponse registrar(EgresoRequest request);

    EgresoResponse procesar(Long id);

    EgresoResponse anular(Long id);

    CargaMasivaEgresosResponse registrarMasivo(MultipartFile archivo);
}
