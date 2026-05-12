package com.gpiv.atlanticsprinttech.backend.registro.application;

import com.gpiv.atlanticsprinttech.backend.config.PropiedadesRegistroPublico;
import com.gpiv.atlanticsprinttech.backend.registro.application.ServicioCorreoVerificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ServicioCorreoVerificacionImpl implements ServicioCorreoVerificacion {
    private static final Logger logger = LoggerFactory.getLogger(ServicioCorreoVerificacionImpl.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final PropiedadesRegistroPublico propiedadesRegistroPublico;

    public ServicioCorreoVerificacionImpl(
        ObjectProvider<JavaMailSender> javaMailSenderProvider,
        PropiedadesRegistroPublico propiedadesRegistroPublico
    ) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.propiedadesRegistroPublico = propiedadesRegistroPublico;
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

        if (!propiedadesRegistroPublico.getCorreo().isHabilitado()) {
            logger.info("[MAIL DESHABILITADO] Para {} enviar asunto='{}' cuerpo='{}'", correoDestino, asunto, cuerpo);
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedadesRegistroPublico.getCorreo().getRemitente());
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
    }
}

