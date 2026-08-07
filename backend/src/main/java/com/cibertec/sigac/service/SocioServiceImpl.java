package com.cibertec.sigac.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.SocioRequest;
import com.cibertec.sigac.dto.SocioResponse;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.SocioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocioServiceImpl implements SocioService {

    private final SocioRepository socioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SocioResponse> listarTodos() {
        return socioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SocioResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public SocioResponse crear(SocioRequest request) {
        if (socioRepository.existsByCodigo(request.getCodigo())) {
            throw new DuplicateResourceException("Ya existe un socio con el codigo " + request.getCodigo());
        }

        Socio socio = Socio.builder()
                .codigo(request.getCodigo())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .accion(request.getAccion())
                .etapa(request.getEtapa())
                .fechaNacimiento(request.getFechaNacimiento())
                .build();

        return toResponse(socioRepository.save(socio));
    }

    @Override
    public SocioResponse actualizar(Long id, SocioRequest request) {
        Socio socio = buscarOLanzar(id);

        if (socioRepository.existsByCodigoAndIdNot(request.getCodigo(), id)) {
            throw new DuplicateResourceException("Ya existe un socio con el codigo " + request.getCodigo());
        }

        socio.setCodigo(request.getCodigo());
        socio.setNombres(request.getNombres());
        socio.setApellidos(request.getApellidos());
        socio.setAccion(request.getAccion());
        socio.setEtapa(request.getEtapa());
        socio.setFechaNacimiento(request.getFechaNacimiento());

        return toResponse(socioRepository.save(socio));
    }

    @Override
    public void eliminar(Long id) {
        Socio socio = buscarOLanzar(id);
        socioRepository.delete(socio);
    }

    private Socio buscarOLanzar(Long id) {
        return socioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Socio no encontrado con id " + id));
    }

    private SocioResponse toResponse(Socio socio) {
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
