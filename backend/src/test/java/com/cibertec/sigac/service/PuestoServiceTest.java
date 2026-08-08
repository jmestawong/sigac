package com.cibertec.sigac.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.cibertec.sigac.dto.PuestoRequest;
import com.cibertec.sigac.dto.PuestoResponse;
import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Socio;
import com.cibertec.sigac.exception.BusinessRuleException;
import com.cibertec.sigac.exception.DuplicateResourceException;
import com.cibertec.sigac.exception.ResourceNotFoundException;
import com.cibertec.sigac.repository.GiroRepository;
import com.cibertec.sigac.repository.PuestoRepository;
import com.cibertec.sigac.repository.SocioRepository;

@ExtendWith(MockitoExtension.class)
class PuestoServiceTest {

    @Mock
    private PuestoRepository puestoRepository;

    @Mock
    private GiroRepository giroRepository;

    @Mock
    private SocioRepository socioRepository;

    @InjectMocks
    private PuestoServiceImpl puestoService;

    private Giro giro;
    private Socio socio;
    private Puesto puestoExistente;
    private PuestoRequest puestoRequest;

    @BeforeEach
    void setUp() {
        giro = Giro.builder().id(10L).nombre("Abarrotes").build();
        socio = Socio.builder()
                .id(20L)
                .codigo("S-001")
                .nombres("Juan")
                .apellidos("Perez")
                .accion("Ordinaria")
                .etapa("Activo")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .build();

        puestoExistente = Puesto.builder()
                .id(1L)
                .numero("P-01")
                .nombreInquilino("Juan Perez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1))
                .fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro)
                .socio(socio)
                .build();

        puestoRequest = new PuestoRequest(
                "P-01", "Juan Perez", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 10L, 20L);
    }

    @Test
    void listarTodos_debeRetornarTodosLosPuestos() {
        when(puestoRepository.findAll()).thenReturn(List.of(puestoExistente));

        List<PuestoResponse> resultado = puestoService.listarTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getGiro().getNombre()).isEqualTo("Abarrotes");
        assertThat(resultado.get(0).getSocio().getCodigo()).isEqualTo("S-001");
    }

    @Test
    void crear_debePersistirConGiroYSocioAsociados() {
        when(puestoRepository.existsByNumero("P-01")).thenReturn(false);
        when(giroRepository.findById(10L)).thenReturn(Optional.of(giro));
        when(socioRepository.findById(20L)).thenReturn(Optional.of(socio));
        when(puestoRepository.save(any(Puesto.class))).thenReturn(puestoExistente);

        PuestoResponse resultado = puestoService.crear(puestoRequest);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getGiro().getId()).isEqualTo(10L);
        assertThat(resultado.getSocio().getId()).isEqualTo(20L);
    }

    @Test
    void crear_sinSocio_debePersistirConSocioNulo() {
        PuestoRequest sinSocio = new PuestoRequest(
                "P-02", "Maria Lopez", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 10L, null);
        Puesto guardado = Puesto.builder()
                .id(2L)
                .numero("P-02")
                .nombreInquilino("Maria Lopez")
                .fechaInicioVigencia(sinSocio.getFechaInicioVigencia())
                .fechaFinVigencia(sinSocio.getFechaFinVigencia())
                .giro(giro)
                .socio(null)
                .build();

        when(puestoRepository.existsByNumero("P-02")).thenReturn(false);
        when(giroRepository.findById(10L)).thenReturn(Optional.of(giro));
        when(puestoRepository.save(any(Puesto.class))).thenReturn(guardado);

        PuestoResponse resultado = puestoService.crear(sinSocio);

        assertThat(resultado.getSocio()).isNull();
        verify(socioRepository, never()).findById(any());
    }

    @Test
    void crear_cuandoNumeroYaExiste_debeLanzarExcepcion() {
        when(puestoRepository.existsByNumero("P-01")).thenReturn(true);

        assertThatThrownBy(() -> puestoService.crear(puestoRequest)).isInstanceOf(DuplicateResourceException.class);

        verify(puestoRepository, never()).save(any(Puesto.class));
    }

    @Test
    void crear_cuandoGiroNoExiste_debeLanzarExcepcion() {
        when(puestoRepository.existsByNumero("P-01")).thenReturn(false);
        when(giroRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> puestoService.crear(puestoRequest)).isInstanceOf(ResourceNotFoundException.class);

        verify(puestoRepository, never()).save(any(Puesto.class));
    }

    @Test
    void crear_cuandoFechaFinEsAnteriorAFechaInicio_debeLanzarExcepcion() {
        PuestoRequest vigenciaInvalida = new PuestoRequest(
                "P-03", "Ana Torres", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), 10L, null);

        when(puestoRepository.existsByNumero("P-03")).thenReturn(false);

        assertThatThrownBy(() -> puestoService.crear(vigenciaInvalida)).isInstanceOf(BusinessRuleException.class);

        verify(puestoRepository, never()).save(any(Puesto.class));
    }

    @Test
    void eliminar_debeInvocarAlRepositorio() {
        when(puestoRepository.findById(1L)).thenReturn(Optional.of(puestoExistente));

        puestoService.eliminar(1L);

        verify(puestoRepository, times(1)).delete(puestoExistente);
    }
}
