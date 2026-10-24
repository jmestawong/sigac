package com.cibertec.mscatalogos.negocio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscatalogos.dto.GiroRequest;
import com.cibertec.mscatalogos.dto.GiroResponse;
import com.cibertec.mscatalogos.entidades.Giro;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.mscatalogos.repositorio.GiroRepository;

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
