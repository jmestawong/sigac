package com.cibertec.sigac.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.cibertec.sigac.entity.Giro;

@DataJpaTest
class GiroRepositoryTest {

    @Autowired
    private GiroRepository giroRepository;

    private Giro nuevoGiro(String nombre) {
        return Giro.builder().nombre(nombre).build();
    }

    @Test
    void insertar_debePersistirElGiroConIdGenerado() {
        Giro guardado = giroRepository.save(nuevoGiro("Abarrotes"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(giroRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void listar_debeRetornarTodosLosGirosPersistidos() {
        giroRepository.save(nuevoGiro("Verduras"));
        giroRepository.save(nuevoGiro("Calzado"));

        List<Giro> giros = giroRepository.findAll();

        assertThat(giros).extracting(Giro::getNombre).contains("Verduras", "Calzado");
    }

    @Test
    void actualizar_debeModificarElNombreDelGiro() {
        Giro guardado = giroRepository.save(nuevoGiro("Ropa"));

        guardado.setNombre("Ropa y accesorios");
        giroRepository.save(guardado);

        assertThat(giroRepository.findById(guardado.getId()).orElseThrow().getNombre())
                .isEqualTo("Ropa y accesorios");
    }

    @Test
    void eliminar_debeRemoverElGiroDeLaBaseDeDatos() {
        Giro guardado = giroRepository.save(nuevoGiro("Ferreteria"));
        Long id = guardado.getId();

        giroRepository.deleteById(id);

        assertThat(giroRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByNombreIgnoreCase_noDebeDistinguirMayusculasDeMinusculas() {
        giroRepository.save(nuevoGiro("Carnes"));

        assertThat(giroRepository.existsByNombreIgnoreCase("CARNES")).isTrue();
        assertThat(giroRepository.existsByNombreIgnoreCase("carnes")).isTrue();
        assertThat(giroRepository.existsByNombreIgnoreCase("Pescados")).isFalse();
    }

    @Test
    void existsByNombreIgnoreCaseAndIdNot_debeIgnorarElPropioRegistroAlEditar() {
        Giro carnes = giroRepository.save(nuevoGiro("Carnes"));
        Giro lacteos = giroRepository.save(nuevoGiro("Lacteos"));

        assertThat(giroRepository.existsByNombreIgnoreCaseAndIdNot("carnes", carnes.getId())).isFalse();
        assertThat(giroRepository.existsByNombreIgnoreCaseAndIdNot("carnes", lacteos.getId())).isTrue();
    }

    @Test
    void insertar_conNombreDuplicado_debeFallarPorRestriccionUnica() {
        giroRepository.saveAndFlush(nuevoGiro("Bazar"));

        assertThatThrownBy(() -> giroRepository.saveAndFlush(nuevoGiro("Bazar")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
