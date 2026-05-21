package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.configuracion.PropiedadesRegistroPublico;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAuditLog;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioCorreoVerificacion;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioCorreoVerificacionImpl implements ServicioCorreoVerificacion {
    private static final Logger logger = LoggerFactory.getLogger(ServicioCorreoVerificacionImpl.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final PropiedadesRegistroPublico propiedadesRegistroPublico;
    private final ServicioAuditLog servicioAuditLog;

    public ServicioCorreoVerificacionImpl(
        ObjectProvider<JavaMailSender> javaMailSenderProvider,
        PropiedadesRegistroPublico propiedadesRegistroPublico,
        ServicioAuditLog servicioAuditLog
    ) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.propiedadesRegistroPublico = propiedadesRegistroPublico;
        this.servicioAuditLog = servicioAuditLog;
    }

    @Override
    public void enviarCorreoVerificacion(String correoDestino, String enlaceVerificacion, long expiracionHoras, String contactoSoporte) {
        String asunto = "Verifica tu correo electronico - GPIV";
        String cuerpo = "Hola,\n\n"
            + "Recibimos una solicitud de registro en GPIV.\n"
            + "Para activar tu cuenta, verifica tu correo haciendo clic en el siguiente enlace:\n\n"
            + enlaceVerificacion + "\n\n"
            + "Este enlace vence en " + expiracionHoras + " horas.\n"
            + "Si no solicitaste esta cuenta, puedes ignorar este correo.\n\n"
            + "Soporte: " + contactoSoporte + "\n"
            + "GPIV";

        if (!propiedadesRegistroPublico.getMail().isHabilitado()) {
            logger.info("[MAIL DESHABILITADO] Para {} enviar asunto='{}' cuerpo='{}'", correoDestino, asunto, cuerpo);
            servicioAuditLog.registrarEvento(
                obtenerUsuarioActual(correoDestino), "ENVIO_CORREO_VERIFICACION", "Usuario",
                correoDestino, null, "mail_deshabilitado", obtenerIpActual()
            );
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedadesRegistroPublico.getMail().getRemitente());
        mensaje.setTo(correoDestino);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No hay configuracion de correo para enviar verificaciones"
            );
        }
        javaMailSender.send(mensaje);
        servicioAuditLog.registrarEvento(
            obtenerUsuarioActual(correoDestino), "ENVIO_CORREO_VERIFICACION", "Usuario",
            correoDestino, null, "enviado", obtenerIpActual()
        );
    }

    private String obtenerUsuarioActual(String fallback) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
            ? auth.getName()
            : fallback;
    }

    private String obtenerIpActual() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "desconocida";
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
            ? forwarded.split(",")[0].trim()
            : request.getRemoteAddr();
    }
}

