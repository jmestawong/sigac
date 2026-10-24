package com.cibertec.mscatalogos.negocio;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.mscatalogos.dto.SocioRequest;
import com.cibertec.mscatalogos.dto.SocioResponse;
import com.cibertec.mscatalogos.entidades.Socio;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.mscatalogos.repositorio.SocioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SocioServiceImpl implements SocioService {

    private final SocioRepository socioRepository;
    private final ReferenciasCobranza referenciasCobranza;

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
        Socio existente = buscarOLanzar(id);
        referenciasCobranza.validarSinUso("socio", id, "el socio " + id);
        socioRepository.delete(existente);
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
