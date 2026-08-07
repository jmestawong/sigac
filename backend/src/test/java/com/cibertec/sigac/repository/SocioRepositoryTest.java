package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.sigac.entity.Socio;

@DataJpaTest
class SocioRepositoryTest {

    @Autowired
    private SocioRepository socioRepository;

    private Socio nuevoSocio(String codigo) {
        return Socio.builder()
                .codigo(codigo)
                .nombres("Maria")
                .apellidos("Lopez")
                .accion("Ordinaria")
                .etapa("Activo")
                .fechaNacimiento(LocalDate.of(1985, 3, 15))
                .build();
    }

    @Test
    void insertar_debePersistirElSocioConIdGenerado() {
        Socio guardado = socioRepository.save(nuevoSocio("S-100"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(socioRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void listar_debeRetornarTodosLosSociosPersistidos() {
        socioRepository.save(nuevoSocio("S-101"));
        socioRepository.save(nuevoSocio("S-102"));

        List<Socio> socios = socioRepository.findAll();

        assertThat(socios).hasSize(2);
    }

    @Test
    void actualizar_debeModificarLosCamposDelSocio() {
        Socio guardado = socioRepository.save(nuevoSocio("S-103"));

        guardado.setEtapa("Suspendido");
        guardado.setNombres("Maria Fernanda");
        socioRepository.save(guardado);

        Optional<Socio> actualizado = socioRepository.findById(guardado.getId());

        assertThat(actualizado).isPresent();
        assertThat(actualizado.get().getEtapa()).isEqualTo("Suspendido");
        assertThat(actualizado.get().getNombres()).isEqualTo("Maria Fernanda");
    }

    @Test
    void eliminar_debeRemoverElSocioDeLaBaseDeDatos() {
        Socio guardado = socioRepository.save(nuevoSocio("S-104"));
        Long id = guardado.getId();

        socioRepository.deleteById(id);

        assertThat(socioRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByCodigo_debeDetectarCodigosDuplicados() {
        socioRepository.save(nuevoSocio("S-105"));

        assertThat(socioRepository.existsByCodigo("S-105")).isTrue();
        assertThat(socioRepository.existsByCodigo("S-999")).isFalse();
    }
}
