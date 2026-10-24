package com.cibertec.mscatalogos.negocio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscatalogos.dto.GiroResponse;
import com.cibertec.mscatalogos.dto.PuestoRequest;
import com.cibertec.mscatalogos.dto.PuestoResponse;
import com.cibertec.mscatalogos.dto.SocioResponse;
import com.cibertec.mscatalogos.entidades.Giro;
import com.cibertec.mscatalogos.entidades.Puesto;
import com.cibertec.mscatalogos.entidades.Socio;
import com.cibertec.sigac.common.exception.BusinessRuleException;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.mscatalogos.repositorio.GiroRepository;
import com.cibertec.mscatalogos.repositorio.PuestoRepository;
import com.cibertec.mscatalogos.repositorio.SocioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PuestoServiceImpl implements PuestoService {

    private final PuestoRepository puestoRepository;
    private final ReferenciasCobranza referenciasCobranza;
    private final GiroRepository giroRepository;
    private final SocioRepository socioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PuestoResponse> listarTodos() {
        return puestoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PuestoResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public PuestoResponse crear(PuestoRequest request) {
        if (puestoRepository.existsByNumero(request.getNumero())) {
            throw new DuplicateResourceException("Ya existe un puesto con el numero " + request.getNumero());
        }
        validarVigencia(request);

        Puesto puesto = Puesto.builder()
                .numero(request.getNumero())
                .nombreInquilino(request.getNombreInquilino())
                .fechaInicioVigencia(request.getFechaInicioVigencia())
                .fechaFinVigencia(request.getFechaFinVigencia())
                .giro(buscarGiro(request.getGiroId()))
                .socio(buscarSocioOpcional(request.getSocioId()))
                .build();

        return toResponse(puestoRepository.save(puesto));
    }

    @Override
    public PuestoResponse actualizar(Long id, PuestoRequest request) {
        Puesto puesto = buscarOLanzar(id);

        if (puestoRepository.existsByNumeroAndIdNot(request.getNumero(), id)) {
            throw new DuplicateResourceException("Ya existe un puesto con el numero " + request.getNumero());
        }
        validarVigencia(request);

        puesto.setNumero(request.getNumero());
        puesto.setNombreInquilino(request.getNombreInquilino());
        puesto.setFechaInicioVigencia(request.getFechaInicioVigencia());
        puesto.setFechaFinVigencia(request.getFechaFinVigencia());
        puesto.setGiro(buscarGiro(request.getGiroId()));
        puesto.setSocio(buscarSocioOpcional(request.getSocioId()));

        return toResponse(puestoRepository.save(puesto));
    }

    @Override
    public void eliminar(Long id) {
        Puesto existente = buscarOLanzar(id);
        referenciasCobranza.validarSinUso("puesto", id, "el puesto " + id);
        puestoRepository.delete(existente);
    }

    private void validarVigencia(PuestoRequest request) {
        if (request.getFechaFinVigencia().isBefore(request.getFechaInicioVigencia())) {
            throw new BusinessRuleException(
                    "La fecha de fin de vigencia no puede ser anterior a la fecha de inicio");
        }
    }

    private Giro buscarGiro(Long giroId) {
        return giroRepository.findById(giroId)
                .orElseThrow(() -> new ResourceNotFoundException("Giro no encontrado con id " + giroId));
    }

    private Socio buscarSocioOpcional(Long socioId) {
        if (socioId == null) {
            return null;
        }
        return socioRepository.findById(socioId)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado con id " + socioId));
    }

    private Puesto buscarOLanzar(Long id) {
        return puestoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Puesto no encontrado con id " + id));
    }

    private PuestoResponse toResponse(Puesto puesto) {
        return PuestoResponse.builder()
                .id(puesto.getId())
                .numero(puesto.getNumero())
                .nombreInquilino(puesto.getNombreInquilino())
                .fechaInicioVigencia(puesto.getFechaInicioVigencia())
                .fechaFinVigencia(puesto.getFechaFinVigencia())
                .giro(GiroResponse.builder().id(puesto.getGiro().getId()).nombre(puesto.getGiro().getNombre()).build())
                .socio(toSocioResponse(puesto.getSocio()))
                .build();
    }

    private SocioResponse toSocioResponse(Socio socio) {
        if (socio == null) {
            return null;
        }
        return SocioResponse.builder()
                .id(socio.getId())
                .codigo(socio.getCodigo())
                .nombres(socio.getNombres())
                .apellidos(socio.getApellidos())
                .accion(socio.getAccion())
                .etapa(socio.getEtapa())
                .fechaNacimiento(socio.getFechaNacimiento())
                .build();
    }
}
