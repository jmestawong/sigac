package com.cibertec.mscatalogos.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.cibertec.mscatalogos.entidades.Moneda;
import com.cibertec.mscatalogos.entidades.Recurrencia;
import com.cibertec.mscatalogos.entidades.ServicioCobrable;
import com.cibertec.mscatalogos.entidades.TipoDestinatario;

@DataJpaTest
class ServicioCobrableRepositoryTest {

    @Autowired
    private ServicioCobrableRepository servicioRepository;

    private ServicioCobrable nuevoServicio(String nombre, boolean porConsumo) {
        return ServicioCobrable.builder()
                .nombre(nombre)
                .recurrencia(Recurrencia.MENSUAL)
                .costo(new BigDecimal("3.50"))
                .moneda(Moneda.PEN)
                .destinatario(TipoDestinatario.PUESTO)
                .esPorConsumo(porConsumo)
                .build();
    }

    @Test
    void insertar_debePersistirElServicioConIdGenerado() {
        ServicioCobrable guardado = servicioRepository.save(nuevoServicio("Agua", true));

        ServicioCobrable persistido = servicioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(persistido.getId()).isNotNull();
        assertThat(persistido.isEsPorConsumo()).isTrue();
        assertThat(persistido.getCosto()).isEqualByComparingTo("3.50");
    }

    @Test
    void listar_debeRetornarTodosLosServiciosPersistidos() {
        servicioRepository.save(nuevoServicio("Agua", true));
        servicioRepository.save(nuevoServicio("Mantenimiento", false));

        List<ServicioCobrable> servicios = servicioRepository.findAll();

        assertThat(servicios).extracting(ServicioCobrable::getNombre).containsExactlyInAnyOrder("Agua", "Mantenimiento");
    }

    @Test
    void actualizar_debeModificarCostoRecurrenciaYModalidad() {
        ServicioCobrable guardado = servicioRepository.save(nuevoServicio("Seguridad", false));

        guardado.setCosto(new BigDecimal("120.00"));
        guardado.setRecurrencia(Recurrencia.ANUAL);
        guardado.setDestinatario(TipoDestinatario.SOCIO);
        servicioRepository.save(guardado);

        ServicioCobrable actualizado = servicioRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getCosto()).isEqualByComparingTo("120.00");
        assertThat(actualizado.getRecurrencia()).isEqualTo(Recurrencia.ANUAL);
        assertThat(actualizado.getDestinatario()).isEqualTo(TipoDestinatario.SOCIO);
    }

    @Test
    void eliminar_debeRemoverElServicioDeLaBaseDeDatos() {
        ServicioCobrable guardado = servicioRepository.save(nuevoServicio("Luz", true));
        Long id = guardado.getId();

        servicioRepository.deleteById(id);

        assertThat(servicioRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByNombreIgnoreCase_yExistsAndIdNot_debenDetectarDuplicados() {
        ServicioCobrable agua = servicioRepository.save(nuevoServicio("Agua", true));
        ServicioCobrable luz = servicioRepository.save(nuevoServicio("Luz", true));

        assertThat(servicioRepository.existsByNombreIgnoreCase("AGUA")).isTrue();
        assertThat(servicioRepository.existsByNombreIgnoreCase("Gas")).isFalse();
        assertThat(servicioRepository.existsByNombreIgnoreCaseAndIdNot("agua", agua.getId())).isFalse();
        assertThat(servicioRepository.existsByNombreIgnoreCaseAndIdNot("agua", luz.getId())).isTrue();
    }
}
