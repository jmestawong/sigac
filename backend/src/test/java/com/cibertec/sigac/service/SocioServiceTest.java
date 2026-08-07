package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.sigac.dto.SocioRequest;
import com.cibertec.sigac.dto.SocioResponse;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.SocioRepository;

@ExtendWith(MockitoExtension.class)
class SocioServiceTest {

    @Mock
    private SocioRepository socioRepository;

    @InjectMocks
    private SocioServiceImpl socioService;

    private Socio socioExistente;
    private SocioRequest socioRequest;

    @BeforeEach
    void setUp() {
        socioExistente = Socio.builder()
                .id(1L)
                .codigo("S-001")
                .nombres("Juan")
                .apellidos("Perez")
                .accion("Ordinaria")
                .etapa("Activo")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .build();

        socioRequest = new SocioRequest(
                "S-001", "Juan", "Perez", "Ordinaria", "Activo", LocalDate.of(1990, 5, 20));
    }

    @Test
    void listarTodos_debeRetornarTodosLosSocios() {
        when(socioRepository.findAll()).thenReturn(List.of(socioExistente));

        List<SocioResponse> resultado = socioService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("S-001");
        verify(socioRepository, times(1)).findAll();
    }

    @Test
    void obtenerPorId_cuandoNoExiste_debeLanzarExcepcion() {
        when(socioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socioService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void crear_debePersistirYRetornarElSocioCreado() {
        when(socioRepository.existsByCodigo("S-001")).thenReturn(false);
        when(socioRepository.save(any(Socio.class))).thenReturn(socioExistente);

        SocioResponse resultado = socioService.crear(socioRequest);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCodigo()).isEqualTo("S-001");
        verify(socioRepository, times(1)).save(any(Socio.class));
    }

    @Test
    void crear_cuandoCodigoYaExiste_debeLanzarExcepcion() {
        when(socioRepository.existsByCodigo("S-001")).thenReturn(true);

        assertThatThrownBy(() -> socioService.crear(socioRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(socioRepository, never()).save(any(Socio.class));
    }

    @Test
    void actualizar_debeModificarYRetornarElSocioActualizado() {
        SocioRequest cambios = new SocioRequest(
                "S-001", "Juan Carlos", "Perez Diaz", "Preferente", "Suspendido", LocalDate.of(1990, 5, 20));

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socioExistente));
        when(socioRepository.existsByCodigoAndIdNot(eq("S-001"), eq(1L))).thenReturn(false);
        when(socioRepository.save(any(Socio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SocioResponse resultado = socioService.actualizar(1L, cambios);

        assertThat(resultado.getNombres()).isEqualTo("Juan Carlos");
        assertThat(resultado.getEtapa()).isEqualTo("Suspendido");
        verify(socioRepository, times(1)).save(any(Socio.class));
    }

    @Test
    void actualizar_cuandoNoExiste_debeLanzarExcepcion() {
        when(socioRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socioService.actualizar(1L, socioRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(socioRepository, never()).save(any(Socio.class));
    }

    @Test
    void eliminar_debeInvocarAlRepositorio() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socioExistente));

        socioService.eliminar(1L);

        verify(socioRepository, times(1)).delete(socioExistente);
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        when(socioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> socioService.eliminar(1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(socioRepository, never()).delete(any(Socio.class));
    }
}
