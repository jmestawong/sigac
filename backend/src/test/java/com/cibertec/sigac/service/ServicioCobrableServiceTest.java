package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.sigac.dto.ServicioCobrableRequest;
import com.cibertec.sigac.dto.ServicioCobrableResponse;
import com.cibertec.sigac.entity.Moneda;
import com.cibertec.sigac.entity.Recurrencia;
import com.cibertec.sigac.entity.ServicioCobrable;
import com.cibertec.sigac.entity.TipoDestinatario;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.ServicioCobrableRepository;

@ExtendWith(MockitoExtension.class)
class ServicioCobrableServiceTest {

    @Mock
    private ServicioCobrableRepository servicioCobrableRepository;

    @InjectMocks
    private ServicioCobrableServiceImpl servicioCobrableService;

    private ServicioCobrable servicioExistente;
    private ServicioCobrableRequest request;

    @BeforeEach
    void setUp() {
        servicioExistente = ServicioCobrable.builder()
                .id(1L)
                .nombre("Mantenimiento")
                .recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("50.00"))
                .moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO)
                .esPorConsumo(false)
                .build();

        request = new ServicioCobrableRequest(
                "Mantenimiento", Recurrencia.MENSUAL, new BigDecimal("50.00"), Moneda.PEN, TipoDestinatario.PUESTO, false);
    }

    @Test
    void listarTodos_debeRetornarTodosLosServicios() {
        when(servicioCobrableRepository.findAll()).thenReturn(List.of(servicioExistente));

        List<ServicioCobrableResponse> resultado = servicioCobrableService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Mantenimiento");
    }

    @Test
    void crear_debePersistirYRetornarElServicioCreado() {
        when(servicioCobrableRepository.existsByNombreIgnoreCase("Mantenimiento")).thenReturn(false);
        when(servicioCobrableRepository.save(any(ServicioCobrable.class))).thenReturn(servicioExistente);

        ServicioCobrableResponse resultado = servicioCobrableService.crear(request);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getDestinatario()).isEqualTo(TipoDestinatario.PUESTO);
        verify(servicioCobrableRepository, times(1)).save(any(ServicioCobrable.class));
    }

    @Test
    void crear_cuandoNombreYaExiste_debeLanzarExcepcion() {
        when(servicioCobrableRepository.existsByNombreIgnoreCase("Mantenimiento")).thenReturn(true);

        assertThatThrownBy(() -> servicioCobrableService.crear(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(servicioCobrableRepository, never()).save(any(ServicioCobrable.class));
    }

    @Test
    void eliminar_cuandoNoExiste_debeLanzarExcepcion() {
        when(servicioCobrableRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicioCobrableService.eliminar(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
