package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.sigac.entity.Giro;
import com.cibertec.sigac.entity.Puesto;
import com.cibertec.sigac.entity.Socio;

/**
 * Pruebas del repositorio de Puesto, que ademas valida las relaciones
 * obligatoria (giro) y opcional (socio) del catalogo.
 */
@DataJpaTest
class PuestoRepositoryTest {

    @Autowired
    private PuestoRepository puestoRepository;

    @Autowired
    private GiroRepository giroRepository;

    @Autowired
    private SocioRepository socioRepository;

    private Giro giro;
    private Socio socio;

    @BeforeEach
    void prepararCatalogos() {
        giro = giroRepository.save(Giro.builder().nombre("Abarrotes").build());
        socio = socioRepository.save(Socio.builder()
                .codigo("S-PUESTO")
                .nombres("Rosa")
                .apellidos("Quispe")
                .accion("Ordinaria")
                .etapa("1")
                .fechaNacimiento(LocalDate.of(1980, 6, 1))
                .build());
    }

    private Puesto nuevoPuesto(String numero, Socio propietario) {
        return Puesto.builder()
                .numero(numero)
                .nombreInquilino("Carlos Ramos")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1))
                .fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(giro)
                .socio(propietario)
                .build();
    }

    @Test
    void insertar_debePersistirElPuestoConSuGiroYSocio() {
        Puesto guardado = puestoRepository.save(nuevoPuesto("P-01", socio));

        Puesto persistido = puestoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.getGiro().getId()).isEqualTo(giro.getId());
        assertThat(persistido.getSocio().getId()).isEqualTo(socio.getId());
    }

    @Test
    void insertar_debePermitirUnPuestoSinSocioAsociado() {
        Puesto guardado = puestoRepository.save(nuevoPuesto("P-02", null));

        assertThat(puestoRepository.findById(guardado.getId()).orElseThrow().getSocio()).isNull();
    }

    @Test
    void listar_debeRetornarTodosLosPuestosPersistidos() {
        puestoRepository.save(nuevoPuesto("P-03", socio));
        puestoRepository.save(nuevoPuesto("P-04", null));

        List<Puesto> puestos = puestoRepository.findAll();

        assertThat(puestos).extracting(Puesto::getNumero).containsExactlyInAnyOrder("P-03", "P-04");
    }

    @Test
    void actualizar_debeModificarInquilinoYVigencia() {
        Puesto guardado = puestoRepository.save(nuevoPuesto("P-05", socio));

        guardado.setNombreInquilino("Lucia Paredes");
        guardado.setFechaFinVigencia(LocalDate.of(2027, 6, 30));
        puestoRepository.save(guardado);

        Puesto actualizado = puestoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getNombreInquilino()).isEqualTo("Lucia Paredes");
        assertThat(actualizado.getFechaFinVigencia()).isEqualTo(LocalDate.of(2027, 6, 30));
    }

    @Test
    void eliminar_debeRemoverElPuestoSinBorrarSuGiroNiSuSocio() {
        Puesto guardado = puestoRepository.save(nuevoPuesto("P-06", socio));
        Long id = guardado.getId();

        puestoRepository.deleteById(id);

        assertThat(puestoRepository.findById(id)).isEmpty();
        assertThat(giroRepository.findById(giro.getId())).isPresent();
        assertThat(socioRepository.findById(socio.getId())).isPresent();
    }

    @Test
    void existsByNumero_yExistsByNumeroAndIdNot_debenDetectarDuplicados() {
        Puesto p07 = puestoRepository.save(nuevoPuesto("P-07", socio));
        Puesto p08 = puestoRepository.save(nuevoPuesto("P-08", null));

        assertThat(puestoRepository.existsByNumero("P-07")).isTrue();
        assertThat(puestoRepository.existsByNumero("P-99")).isFalse();
        assertThat(puestoRepository.existsByNumeroAndIdNot("P-07", p07.getId())).isFalse();
        assertThat(puestoRepository.existsByNumeroAndIdNot("P-07", p08.getId())).isTrue();
    }
}
