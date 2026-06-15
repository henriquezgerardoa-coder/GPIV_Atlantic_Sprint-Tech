package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.backend.mapeador.ParConversacion;
import com.gpiv.atlanticsprinttech.commons.comunicacion.dto.RespuestaBorradorMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.util.List;

public interface ServicioMensajeria {

    List<ConversacionMensajeria> listar(String identificadorIngreso);

    List<ParConversacion> listarConConMensajes(String identificadorIngreso);

    ConversacionMensajeria obtenerPorId(String identificadorIngreso, Long id);

    List<MensajeMensajeria> listarMensajes(String identificadorIngreso, Long conversacionId);

    ConversacionMensajeria crear(String identificadorIngreso, Long usuarioResponsableId, String asunto, String mensaje);

    ConversacionMensajeria responder(String identificadorIngreso, Long conversacionId, String mensaje);

    List<Usuario> listarDestinatarios(String identificadorIngreso);

    ConversacionMensajeria crearConsultaPublica(
        String nombreOEmpresa,
        String correoElectronico,
        String telefono,
        String asunto,
        String mensaje
    );

    ConversacionMensajeria notificarEmpresa(Long empresaId, String asunto, String mensaje);

    List<RespuestaBorradorMensajeria> listarBorradores(String identificadorIngreso);

    RespuestaBorradorMensajeria guardarBorrador(String identificadorIngreso, Long borradorId, Long destinatarioId, String asunto, String contenido);

    ConversacionMensajeria enviarBorrador(String identificadorIngreso, Long borradorId);

    void eliminarBorrador(String identificadorIngreso, Long borradorId);
}

