package com.cibertec.mscatalogos.negocio;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.mscatalogos.dto.BancoRequest;
import com.cibertec.mscatalogos.dto.BancoResponse;
import com.cibertec.mscatalogos.entidades.Banco;
import com.cibertec.mscatalogos.entidades.Moneda;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.mscatalogos.repositorio.BancoRepository;

@ExtendWith(MockitoExtension.class)
class BancoServiceTest {

    @Mock
    private BancoRepository bancoRepository;

    @Mock
    private ReferenciasCobranza referenciasCobranza;

    @InjectMocks
    private BancoServiceImpl bancoService;

    private Banco bancoExistente;
    private BancoRequest bancoRequest;

    @BeforeEach
    void setUp() {
        bancoExistente = Banco.builder()
                .id(1L)
                .nombre("BCP")
                .numeroCuenta("193-1234567-0-01")
                .cci("00219300123456700146")
                .moneda(Moneda.PEN)
                .build();

        bancoRequest = new BancoRequest("BCP", "193-1234567-0-01", "00219300123456700146", Moneda.PEN);
    }

    @Test
    void listarTodos_debeRetornarTodosLosBancos() {
        when(bancoRepository.findAll()).thenReturn(List.of(bancoExistente));

        List<BancoResponse> resultado = bancoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCci()).isEqualTo("00219300123456700146");
    }

    @Test
    void crear_debePersistirYRetornarElBancoCreado() {
        when(bancoRepository.existsByNumeroCuenta(bancoRequest.getNumeroCuenta())).thenReturn(false);
        when(bancoRepository.existsByCci(bancoRequest.getCci())).thenReturn(false);
        when(bancoRepository.save(any(Banco.class))).thenReturn(bancoExistente);

        BancoResponse resultado = bancoService.crear(bancoRequest);

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(bancoRepository, times(1)).save(any(Banco.class));
    }

    @Test
    void crear_cuandoNumeroDeCuentaYaExiste_debeLanzarExcepcion() {
        when(bancoRepository.existsByNumeroCuenta(bancoRequest.getNumeroCuenta())).thenReturn(true);

        assertThatThrownBy(() -> bancoService.crear(bancoRequest)).isInstanceOf(DuplicateResourceException.class);

        verify(bancoRepository, never()).save(any(Banco.class));
    }

    @Test
    void crear_cuandoCciYaExiste_debeLanzarExcepcion() {
        when(bancoRepository.existsByNumeroCuenta(bancoRequest.getNumeroCuenta())).thenReturn(false);
        when(bancoRepository.existsByCci(bancoRequest.getCci())).thenReturn(true);

        assertThatThrownBy(() -> bancoService.crear(bancoRequest)).isInstanceOf(DuplicateResourceException.class);

        verify(bancoRepository, never()).save(any(Banco.class));
    }

    @Test
    void actualizar_debeModificarYRetornarElBancoActualizado() {
        BancoRequest cambios = new BancoRequest("BCP Agencia Centro", "193-1234567-0-01", "00219300123456700146", Moneda.USD);

        when(bancoRepository.findById(1L)).thenReturn(Optional.of(bancoExistente));
        when(bancoRepository.existsByNumeroCuentaAndIdNot(cambios.getNumeroCuenta(), 1L)).thenReturn(false);
        when(bancoRepository.existsByCciAndIdNot(cambios.getCci(), 1L)).thenReturn(false);
        when(bancoRepository.save(any(Banco.class))).thenAnswer(inv -> inv.getArgument(0));

        BancoResponse resultado = bancoService.actualizar(1L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("BCP Agencia Centro");
        assertThat(resultado.getMoneda()).isEqualTo(Moneda.USD);
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        when(bancoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bancoService.eliminar(1L)).isInstanceOf(ResourceNotFoundException.class);

        verify(bancoRepository, never()).delete(any(Banco.class));
    }

    @Test
    void eliminar_cuandoTieneCobranzaAsociada_debeRechazarlaSinEliminar() {
        Banco existente = new Banco();
        when(bancoRepository.findById(1L)).thenReturn(Optional.of(existente));
        doThrow(new DuplicateResourceException("en uso"))
                .when(referenciasCobranza).validarSinUso(eq("banco"), eq(1L), anyString());

        assertThatThrownBy(() -> bancoService.eliminar(1L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(bancoRepository, never()).delete(any(Banco.class));
    }
}
