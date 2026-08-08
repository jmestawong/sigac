package com.cibertec.sigac.service;

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

import com.cibertec.sigac.dto.GiroRequest;
import com.cibertec.sigac.dto.GiroResponse;
import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.GiroRepository;

@ExtendWith(MockitoExtension.class)
class GiroServiceTest {

    @Mock
    private GiroRepository giroRepository;

    @InjectMocks
    private GiroServiceImpl giroService;

    private Giro giroExistente;

    @BeforeEach
    void setUp() {
        giroExistente = Giro.builder().id(1L).nombre("Abarrotes").build();
    }

    @Test
    void listarTodos_debeRetornarTodosLosGiros() {
        when(giroRepository.findAll()).thenReturn(List.of(giroExistente));

        List<GiroResponse> resultado = giroService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Abarrotes");
    }

    @Test
    void crear_debePersistirYRetornarElGiroCreado() {
        when(giroRepository.existsByNombreIgnoreCase("Abarrotes")).thenReturn(false);
        when(giroRepository.save(any(Giro.class))).thenReturn(giroExistente);

        GiroResponse resultado = giroService.crear(new GiroRequest("Abarrotes"));

        assertThat(resultado.getId()).isEqualTo(1L);
        verify(giroRepository, times(1)).save(any(Giro.class));
    }

    @Test
    void crear_cuandoNombreYaExiste_debeLanzarExcepcion() {
        when(giroRepository.existsByNombreIgnoreCase("Abarrotes")).thenReturn(true);

        assertThatThrownBy(() -> giroService.crear(new GiroRequest("Abarrotes")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(giroRepository, never()).save(any(Giro.class));
    }

    @Test
    void actualizar_debeModificarYRetornarElGiroActualizado() {
        when(giroRepository.findById(1L)).thenReturn(Optional.of(giroExistente));
        when(giroRepository.existsByNombreIgnoreCaseAndIdNot("Ferreteria", 1L)).thenReturn(false);
        when(giroRepository.save(any(Giro.class))).thenAnswer(inv -> inv.getArgument(0));

        GiroResponse resultado = giroService.actualizar(1L, new GiroRequest("Ferreteria"));

        assertThat(resultado.getNombre()).isEqualTo("Ferreteria");
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        when(giroRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> giroService.eliminar(1L)).isInstanceOf(ResourceNotFoundException.class);

        verify(giroRepository, never()).delete(any(Giro.class));
    }

    @Test
    void eliminar_debeInvocarAlRepositorio() {
        when(giroRepository.findById(1L)).thenReturn(Optional.of(giroExistente));

        giroService.eliminar(1L);

        verify(giroRepository, times(1)).delete(giroExistente);
    }
}
