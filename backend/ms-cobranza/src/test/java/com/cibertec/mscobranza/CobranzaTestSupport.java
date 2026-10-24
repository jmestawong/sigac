package com.cibertec.mscobranza;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.BancoResponse;
import com.cibertec.mscobranza.dto.GiroResponse;
import com.cibertec.mscobranza.dto.Moneda;
import com.cibertec.mscobranza.dto.PuestoResponse;
import com.cibertec.mscobranza.dto.Recurrencia;
import com.cibertec.mscobranza.dto.ServicioCobrableResponse;
import com.cibertec.mscobranza.dto.SocioResponse;
import com.cibertec.mscobranza.dto.TipoDestinatario;

final class CobranzaTestSupport {

    private CobranzaTestSupport() {
    }

    static void configurarCatalogos(CatalogosClient catalogos) {
        SocioResponse socio = SocioResponse.builder()
                .id(1L).codigo("S-001").nombres("Ana").apellidos("Torres").accion("Ordinaria").etapa("1")
                .fechaNacimiento(LocalDate.of(1990, 1, 1)).build();
        PuestoResponse puesto = PuestoResponse.builder()
                .id(1L).numero("P-01").nombreInquilino("Juan Perez")
                .fechaInicioVigencia(LocalDate.of(2026, 1, 1)).fechaFinVigencia(LocalDate.of(2026, 12, 31))
                .giro(GiroResponse.builder().id(1L).nombre("Abarrotes").build()).socio(socio).build();
        ServicioCobrableResponse mantenimiento = ServicioCobrableResponse.builder()
                .id(1L).nombre("Mantenimiento").recurrencia(Recurrencia.MENSUAL).costo(new BigDecimal("50.00"))
                .moneda(Moneda.PEN).destinatario(TipoDestinatario.PUESTO).esPorConsumo(false).build();
        ServicioCobrableResponse cuota = ServicioCobrableResponse.builder()
                .id(2L).nombre("Cuota asociativa").recurrencia(Recurrencia.ANUAL).costo(new BigDecimal("100.00"))
                .moneda(Moneda.PEN).destinatario(TipoDestinatario.SOCIO).esPorConsumo(false).build();
        BancoResponse banco = BancoResponse.builder()
                .id(1L).nombre("BCP").numeroCuenta("193-1").cci("00219300123456").moneda(Moneda.PEN).build();

        when(catalogos.obtenerSocio(1L)).thenReturn(socio);
        when(catalogos.listarSocios()).thenReturn(java.util.List.of(socio));
        when(catalogos.obtenerPuesto(1L)).thenReturn(puesto);
        when(catalogos.listarPuestos()).thenReturn(java.util.List.of(puesto));
        when(catalogos.obtenerServicio(1L)).thenReturn(mantenimiento);
        when(catalogos.obtenerServicio(2L)).thenReturn(cuota);
        when(catalogos.obtenerBanco(1L)).thenReturn(banco);
    }
}
