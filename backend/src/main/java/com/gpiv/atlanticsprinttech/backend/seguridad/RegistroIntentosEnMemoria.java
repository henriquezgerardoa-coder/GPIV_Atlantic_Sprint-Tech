package com.gpiv.atlanticsprinttech.backend.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class RegistroIntentosEnMemoria {
    private final ConcurrentHashMap<String, Deque<Instant>> intentosPorClave = new ConcurrentHashMap<>();

    public boolean excedeLimite(String clave, int maximo, Duration ventana) {
        if (maximo <= 0) {
            return false;
        }
        Deque<Instant> intentos = intentosPorClave.computeIfAbsent(clave, key -> new ConcurrentLinkedDeque<>());
        limpiarExpirados(intentos, ventana);
        return intentos.size() >= maximo;
    }

    public void registrarIntento(String clave, Duration ventana) {
        Deque<Instant> intentos = intentosPorClave.computeIfAbsent(clave, key -> new ConcurrentLinkedDeque<>());
        limpiarExpirados(intentos, ventana);
        intentos.addLast(Instant.now());
    }

    public void limpiar(String clave) {
        intentosPorClave.remove(clave);
    }

    private void limpiarExpirados(Deque<Instant> intentos, Duration ventana) {
        Instant limite = Instant.now().minus(ventana);
        while (!intentos.isEmpty() && intentos.peekFirst().isBefore(limite)) {
            intentos.pollFirst();
        }
    }
}

