package com.gpiv.atlanticsprinttech.backend.salud.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControladorSalud {
	@GetMapping({"/salud", "/health"})
	public Map<String, String> obtenerEstado() {
		return Map.of("estado", "ok");
	}
}