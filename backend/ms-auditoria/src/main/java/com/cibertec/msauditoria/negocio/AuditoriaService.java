package com.cibertec.msauditoria.negocio;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cibertec.msauditoria.entidades.RegistroAuditoria;
import com.cibertec.msauditoria.repositorio.RegistroAuditoriaRepository;
import com.cibertec.sigac.common.auditoria.AuditoriaEvento;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditoriaService {

    private final RegistroAuditoriaRepository repositorio;

    public RegistroAuditoria registrar(AuditoriaEvento evento) {
        return repositorio.save(RegistroAuditoria.builder()
                .modulo(recortar(evento.getModulo(), 30))
                .accion(recortar(evento.getAccion(), 40))
                .referencia(recortar(evento.getReferencia(), 100))
                .detalle(recortar(evento.getDetalle(), 400))
                .monto(evento.getMonto())
                .usuario(evento.getUsuario() != null ? recortar(evento.getUsuario(), 50) : "sistema")
                .fecha(evento.getFecha() != null ? evento.getFecha() : LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public List<RegistroAuditoria> listar(String modulo, String usuario) {
        if (modulo != null && !modulo.isBlank()) {
            return repositorio.findByModuloIgnoreCaseOrderByIdDesc(modulo);
        }
        if (usuario != null && !usuario.isBlank()) {
            return repositorio.findByUsuarioIgnoreCaseOrderByIdDesc(usuario);
        }
        return repositorio.findAllByOrderByIdDesc();
    }

    private String recortar(String texto, int maximo) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
