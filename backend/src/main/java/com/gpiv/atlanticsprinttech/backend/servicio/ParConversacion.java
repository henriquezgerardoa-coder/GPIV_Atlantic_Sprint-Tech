package com.gpiv.atlanticsprinttech.backend.servicio;

import com.gpiv.atlanticsprinttech.entities.dominio.ConversacionMensajeria;
import com.gpiv.atlanticsprinttech.entities.dominio.MensajeMensajeria;
import java.util.List;

public record ParConversacion(ConversacionMensajeria conversacion, List<MensajeMensajeria> mensajes) {}
