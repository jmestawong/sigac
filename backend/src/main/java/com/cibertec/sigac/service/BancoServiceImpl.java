package com.cibertec.sigac.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.sigac.dto.BancoRequest;
import com.cibertec.sigac.dto.BancoResponse;
import com.cibertec.sigac.entity.Banco;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.BancoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BancoServiceImpl implements BancoService {

    private final BancoRepository bancoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BancoResponse> listarTodos() {
        return bancoRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BancoResponse obtenerPorId(Long id) {
        return toResponse(buscarOLanzar(id));
    }

    @Override
    public BancoResponse crear(BancoRequest request) {
        validarDuplicados(request, null);

        Banco banco = Banco.builder()
                .nombre(request.getNombre())
                .numeroCuenta(request.getNumeroCuenta())
                .cci(request.getCci())
                .moneda(request.getMoneda())
                .build();

        return toResponse(bancoRepository.save(banco));
    }

    @Override
    public BancoResponse actualizar(Long id, BancoRequest request) {
        Banco banco = buscarOLanzar(id);
        validarDuplicados(request, id);

        banco.setNombre(request.getNombre());
        banco.setNumeroCuenta(request.getNumeroCuenta());
        banco.setCci(request.getCci());
        banco.setMoneda(request.getMoneda());

        return toResponse(bancoRepository.save(banco));
    }

    @Override
    public void eliminar(Long id) {
        bancoRepository.delete(buscarOLanzar(id));
    }

    private void validarDuplicados(BancoRequest request, Long idActual) {
        boolean cuentaDuplicada = idActual == null
                ? bancoRepository.existsByNumeroCuenta(request.getNumeroCuenta())
                : bancoRepository.existsByNumeroCuentaAndIdNot(request.getNumeroCuenta(), idActual);

        if (cuentaDuplicada) {
            throw new DuplicateResourceException(
                    "Ya existe una cuenta bancaria con el numero " + request.getNumeroCuenta());
        }

        boolean cciDuplicado = idActual == null
                ? bancoRepository.existsByCci(request.getCci())
                : bancoRepository.existsByCciAndIdNot(request.getCci(), idActual);

        if (cciDuplicado) {
            throw new DuplicateResourceException("Ya existe una cuenta bancaria con el CCI " + request.getCci());
        }
    }

    private Banco buscarOLanzar(Long id) {
        return bancoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banco no encontrado con id " + id));
    }

    private BancoResponse toResponse(Banco banco) {
        return BancoResponse.builder()
                .id(banco.getId())
                .nombre(banco.getNombre())
                .numeroCuenta(banco.getNumeroCuenta())
                .cci(banco.getCci())
                .moneda(banco.getMoneda())
                .build();
    }
}
