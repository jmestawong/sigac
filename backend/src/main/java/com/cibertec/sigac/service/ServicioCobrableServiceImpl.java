package com.cibertec.sigac.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.ServicioCobrableRequest;
import com.cibertec.sigac.dto.ServicioCobrableResponse;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.ServicioCobrableRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ServicioCobrableServiceImpl implements ServicioCobrableService {

    private final ServicioCobrableRepository servicioCobrableRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ServicioCobrableResponse> listarTodos() {
        return servicioCobrableRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ServicioCobrableResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public ServicioCobrableResponse crear(ServicioCobrableRequest request) {
        if (servicioCobrableRepository.existsByNombreIgnoreCase(request.getNombre())) {
            throw new DuplicateResourceException("Ya existe un servicio con el nombre " + request.getNombre());
        }

        ServicioCobrable servicio = ServicioCobrable.builder()
                .nombre(request.getNombre())
                .recurrencia(request.getRecurrencia())
                .costo(request.getCosto())
                .moneda(request.getMoneda())
                .destinatario(request.getDestinatario())
                .esPorConsumo(request.isEsPorConsumo())
                .build();

        return toResponse(servicioCobrableRepository.save(servicio));
    }

    @Override
    public ServicioCobrableResponse actualizar(Long id, ServicioCobrableRequest request) {
        ServicioCobrable servicio = buscarOLanzar(id);

        if (servicioCobrableRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre(), id)) {
            throw new DuplicateResourceException("Ya existe un servicio con el nombre " + request.getNombre());
        }

        servicio.setNombre(request.getNombre());
        servicio.setRecurrencia(request.getRecurrencia());
        servicio.setCosto(request.getCosto());
        servicio.setMoneda(request.getMoneda());
        servicio.setDestinatario(request.getDestinatario());
        servicio.setEsPorConsumo(request.isEsPorConsumo());

        return toResponse(servicioCobrableRepository.save(servicio));
    }

    @Override
    public void eliminar(Long id) {
        servicioCobrableRepository.delete(buscarOLanzar(id));
    }

    private ServicioCobrable buscarOLanzar(Long id) {
        return servicioCobrableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio cobrable no encontrado con id " + id));
    }

    private ServicioCobrableResponse toResponse(ServicioCobrable servicio) {
        return ServicioCobrableResponse.builder()
                .id(servicio.getId())
                .nombre(servicio.getNombre())
                .recurrencia(servicio.getRecurrencia())
                .costo(servicio.getCosto())
                .moneda(servicio.getMoneda())
                .destinatario(servicio.getDestinatario())
                .esPorConsumo(servicio.isEsPorConsumo())
                .build();
    }
}
