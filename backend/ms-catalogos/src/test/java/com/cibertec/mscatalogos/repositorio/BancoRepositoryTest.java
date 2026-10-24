package com.cibertec.mscatalogos.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.cibertec.mscatalogos.entidades.Banco;
import com.cibertec.mscatalogos.entidades.Moneda;

@DataJpaTest
class BancoRepositoryTest {

    @Autowired
    private BancoRepository bancoRepository;

    private Banco nuevoBanco(String numeroCuenta, String cci) {
        return Banco.builder()
                .nombre("BCP")
                .numeroCuenta(numeroCuenta)
                .cci(cci)
                .moneda(Moneda.PEN)
                .build();
    }

    @Test
    void insertar_debePersistirLaCuentaBancariaConIdGenerado() {
        Banco guardado = bancoRepository.save(nuevoBanco("193-0000001-0-01", "00219300000000100101"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(bancoRepository.findById(guardado.getId())).isPresent();
    }

    @Test
    void listar_debeRetornarTodasLasCuentasPersistidas() {
        bancoRepository.save(nuevoBanco("193-0000002-0-01", "00219300000000200101"));
        bancoRepository.save(nuevoBanco("193-0000003-0-01", "00219300000000300101"));

        List<Banco> bancos = bancoRepository.findAll();

        assertThat(bancos).hasSize(2);
    }

    @Test
    void actualizar_debeModificarNombreYMoneda() {
        Banco guardado = bancoRepository.save(nuevoBanco("193-0000004-0-01", "00219300000000400101"));

        guardado.setNombre("Interbank");
        guardado.setMoneda(Moneda.USD);
        bancoRepository.save(guardado);

        Banco actualizado = bancoRepository.findById(guardado.getId()).orElseThrow();

        assertThat(actualizado.getNombre()).isEqualTo("Interbank");
        assertThat(actualizado.getMoneda()).isEqualTo(Moneda.USD);
    }

    @Test
    void eliminar_debeRemoverLaCuentaDeLaBaseDeDatos() {
        Banco guardado = bancoRepository.save(nuevoBanco("193-0000005-0-01", "00219300000000500101"));
        Long id = guardado.getId();

        bancoRepository.deleteById(id);

        assertThat(bancoRepository.findById(id)).isEmpty();
    }

    @Test
    void existsByNumeroCuentaYCci_debenDetectarDuplicados() {
        bancoRepository.save(nuevoBanco("193-0000006-0-01", "00219300000000600101"));

        assertThat(bancoRepository.existsByNumeroCuenta("193-0000006-0-01")).isTrue();
        assertThat(bancoRepository.existsByNumeroCuenta("999")).isFalse();
        assertThat(bancoRepository.existsByCci("00219300000000600101")).isTrue();
        assertThat(bancoRepository.existsByCci("000")).isFalse();
    }

    @Test
    void existsAndIdNot_debeIgnorarElPropioRegistroAlEditar() {
        Banco a = bancoRepository.save(nuevoBanco("193-0000007-0-01", "00219300000000700101"));
        Banco b = bancoRepository.save(nuevoBanco("193-0000008-0-01", "00219300000000800101"));

        assertThat(bancoRepository.existsByNumeroCuentaAndIdNot("193-0000007-0-01", a.getId())).isFalse();
        assertThat(bancoRepository.existsByNumeroCuentaAndIdNot("193-0000007-0-01", b.getId())).isTrue();
        assertThat(bancoRepository.existsByCciAndIdNot("00219300000000700101", a.getId())).isFalse();
        assertThat(bancoRepository.existsByCciAndIdNot("00219300000000700101", b.getId())).isTrue();
    }

    @Test
    void insertar_conNumeroDeCuentaDuplicado_debeFallarPorRestriccionUnica() {
        bancoRepository.saveAndFlush(nuevoBanco("193-0000009-0-01", "00219300000000900101"));

        assertThatThrownBy(() -> bancoRepository.saveAndFlush(nuevoBanco("193-0000009-0-01", "00219300000001000101")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
