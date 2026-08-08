package com.cibertec.sigac.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.GiroRequest;
import com.cibertec.sigac.dto.GiroResponse;
import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.GiroRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class GiroServiceImpl implements GiroService {

    private final GiroRepository giroRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GiroResponse> listarTodos() {
        return giroRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GiroResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public GiroResponse crear(GiroRequest request) {
        if (giroRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new DuplicateResourceException("Ya existe un giro con el nombre " + request.getNombre());
        }

        Giro giro = Giro.builder().nombre(request.getNombre()).build();
        return toResponse(giroRepository.save(giro));
    }

    @Override
    public GiroResponse actualizar(Long id, GiroRequest request) {
        Giro giro = buscarOLanzar(id);

        if (giroRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre(), id)) {
            throw new DuplicateResourceException("Ya existe un giro con el nombre " + request.getNombre());
        }

        giro.setNombre(request.getNombre());
        return toResponse(giroRepository.save(giro));
    }

    @Override
    public void eliminar(Long id) {
        giroRepository.delete(buscarOLanzar(id));
    }

    private Giro buscarOLanzar(Long id) {
        return giroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giro no encontrado con id " + id));
    }

    private GiroResponse toResponse(Giro giro) {
        return GiroResponse.builder().id(giro.getId()).nombre(giro.getNombre()).build();
    }
}
