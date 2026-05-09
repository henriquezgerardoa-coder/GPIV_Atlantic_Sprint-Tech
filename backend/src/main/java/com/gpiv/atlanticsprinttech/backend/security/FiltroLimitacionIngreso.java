package com.gpiv.atlanticsprinttech.backend.security;

import com.gpiv.atlanticsprinttech.backend.config.PropiedadesRegistroPublico;
import com.gpiv.atlanticsprinttech.backend.security.RegistroIntentosEnMemoria;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class FiltroLimitacionIngreso extends OncePerRequestFilter {
	private final RegistroIntentosEnMemoria registroIntentosEnMemoria;
	private final PropiedadesRegistroPublico propiedadesRegistroPublico;

	public FiltroLimitacionIngreso(
		RegistroIntentosEnMemoria registroIntentosEnMemoria,
		PropiedadesRegistroPublico propiedadesRegistroPublico
	) {
		this.registroIntentosEnMemoria = registroIntentosEnMemoria;
		this.propiedadesRegistroPublico = propiedadesRegistroPublico;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"/api/yo".equals(request.getRequestURI());
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String identificador = extraerIdentificadorCredencial(request);
		if (identificador == null || identificador.isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		String claveIntento = "ingreso:" + request.getRemoteAddr() + ":" + identificador.toLowerCase();
		Duration ventana = Duration.ofMinutes(propiedadesRegistroPublico.getLimites().getVentanaMinutos());
		if (registroIntentosEnMemoria.excedeLimite(
			claveIntento,
			propiedadesRegistroPublico.getLimites().getMaxIntentosIngreso(),
			ventana
		)) {
			response.setStatus(429);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write("{\"mensaje\":\"Demasiados intentos fallidos. Intenta nuevamente mas tarde\"}");
			return;
		}

		filterChain.doFilter(request, response);

		if (response.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
			registroIntentosEnMemoria.registrarIntento(claveIntento, ventana);
		} else if (response.getStatus() < HttpServletResponse.SC_BAD_REQUEST) {
			registroIntentosEnMemoria.limpiar(claveIntento);
		}
	}

	private String extraerIdentificadorCredencial(HttpServletRequest request) {
		String autorizacion = request.getHeader("Authorization");
		if (autorizacion == null || !autorizacion.startsWith("Basic ")) {
			return null;
		}
		try {
			String decodificado = new String(Base64.getDecoder().decode(autorizacion.substring(6)), StandardCharsets.UTF_8);
			int separador = decodificado.indexOf(':');
			if (separador <= 0) {
				return null;
			}
			return decodificado.substring(0, separador).trim();
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}

