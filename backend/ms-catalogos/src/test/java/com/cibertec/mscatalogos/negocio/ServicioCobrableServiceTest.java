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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cibertec.mscatalogos.dto.ServicioCobrableRequest;
import com.cibertec.mscatalogos.dto.ServicioCobrableResponse;
import com.cibertec.mscatalogos.entidades.Moneda;
import com.cibertec.mscatalogos.entidades.Recurrencia;
import com.cibertec.mscatalogos.entidades.ServicioCobrable;
import com.cibertec.mscatalogos.entidades.TipoDestinatario;
import com.cibertec.sigac.common.exception.DuplicateResourceException;
import com.cibertec.sigac.common.exception.ResourceNotFoundException;
import com.cibertec.mscatalogos.repositorio.ServicioCobrableRepository;

@ExtendWith(MockitoExtension.class)
class ServicioCobrableServiceTest {

    @Mock
    private ServicioCobrableRepository servicioCobrableRepository;

    @Mock
    private ReferenciasCobranza referenciasCobranza;

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

    @Test
    void eliminar_cuandoTieneCobranzaAsociada_debeRechazarlaSinEliminar() {
        ServicioCobrable existente = new ServicioCobrable();
        when(servicioCobrableRepository.findById(1L)).thenReturn(Optional.of(existente));
        doThrow(new DuplicateResourceException("en uso"))
                .when(referenciasCobranza).validarSinUso(eq("servicio"), eq(1L), anyString());

        assertThatThrownBy(() -> servicioCobrableService.eliminar(1L))
                .isInstanceOf(DuplicateResourceException.class);

        verify(servicioCobrableRepository, never()).delete(any(ServicioCobrable.class));
    }
}
