package com.gpiv.atlanticsprinttech.backend.mapeador;

import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import java.util.List;

/**
 * Par interno que agrupa una conversación y su lista de mensajes.
 * Utilizado exclusivamente para transferir datos entre la capa de servicio y el mapeador.
 */
public record ParConversacion(ConversacionMensajeria conversacion, List<MensajeMensajeria> mensajes) {}

