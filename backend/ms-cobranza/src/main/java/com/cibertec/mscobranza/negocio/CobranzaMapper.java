package com.cibertec.mscobranza.negocio;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cibertec.mscobranza.client.CatalogosClient;
import com.cibertec.mscobranza.dto.CuentaPorCobrarResponse;
import com.cibertec.mscobranza.dto.ReciboResponse;
import com.cibertec.mscobranza.entidades.CuentaPorCobrar;
import com.cibertec.mscobranza.entidades.Recibo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CobranzaMapper {

    private final CatalogosClient catalogosClient;

    public CatalogoResolver nuevoResolver() {
        return new CatalogoResolver(catalogosClient);
    }

    public CuentaPorCobrarResponse toCuentaResponse(CuentaPorCobrar cuenta, CatalogoResolver catalogos) {
        return CuentaPorCobrarResponse.builder()
                .id(cuenta.getId())
                .estado(cuenta.getEstado())
                .monto(cuenta.getMonto())
                .periodo(cuenta.getPeriodo())
                .puesto(catalogos.puesto(cuenta.getPuestoId()))
                .socio(catalogos.socio(cuenta.getSocioId()))
                .servicio(catalogos.servicio(cuenta.getServicioId()))
                .lecturaInicial(cuenta.getLecturaInicial())
                .lecturaFinal(cuenta.getLecturaFinal())
                .reciboCorrelativo(cuenta.getRecibo() != null ? cuenta.getRecibo().getCorrelativo() : null)
                .build();
    }

    public List<CuentaPorCobrarResponse> toCuentaResponses(List<CuentaPorCobrar> cuentas, CatalogoResolver catalogos) {
        return cuentas.stream().map(cuenta -> toCuentaResponse(cuenta, catalogos)).toList();
    }

    public ReciboResponse toReciboResponse(Recibo recibo, List<CuentaPorCobrar> cuentas, CatalogoResolver catalogos) {
        return ReciboResponse.builder()
                .id(recibo.getId())
                .correlativo(recibo.getCorrelativo())
                .tipo(recibo.getTipo())
                .fecha(recibo.getFecha())
                .monto(recibo.getMonto())
                .banco(catalogos.banco(recibo.getBancoId()))
                .fechaDeposito(recibo.getFechaDeposito())
                .depositante(recibo.getDepositante())
                .categoria(recibo.getCategoria())
                .concepto(recibo.getConcepto())
                .cuentas(toCuentaResponses(cuentas, catalogos))
                .build();
    }
}
