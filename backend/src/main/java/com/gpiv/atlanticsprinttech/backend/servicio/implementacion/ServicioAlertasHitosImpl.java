package com.gpiv.atlanticsprinttech.backend.servicio.implementacion;

import com.gpiv.atlanticsprinttech.backend.configuracion.PropiedadesAlertasHitos;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioHitoObra;
import com.gpiv.atlanticsprinttech.backend.repositorio.RepositorioUsuario;
import com.gpiv.atlanticsprinttech.backend.servicio.ServicioAlertasHitos;
import com.gpiv.atlanticsprinttech.entities.dominio.HitoObra;
import com.gpiv.atlanticsprinttech.entities.dominio.RolUsuario;
import com.gpiv.atlanticsprinttech.entities.dominio.Usuario;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicioAlertasHitosImpl implements ServicioAlertasHitos {

    private static final Logger logger = LoggerFactory.getLogger(ServicioAlertasHitosImpl.class);
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Set<RolUsuario> ROLES_GESTION = Set.of(RolUsuario.ADMINISTRADOR, RolUsuario.DIRECTIVO);

    private final RepositorioHitoObra repositorioHito;
    private final RepositorioUsuario repositorioUsuario;
    private final PropiedadesAlertasHitos propiedades;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public ServicioAlertasHitosImpl(
        RepositorioHitoObra repositorioHito,
        RepositorioUsuario repositorioUsuario,
        PropiedadesAlertasHitos propiedades,
        ObjectProvider<JavaMailSender> mailSenderProvider
    ) {
        this.repositorioHito = repositorioHito;
        this.repositorioUsuario = repositorioUsuario;
        this.propiedades = propiedades;
        this.mailSenderProvider = mailSenderProvider;
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void enviarAlertasHitos() {
        if (!propiedades.isHabilitado()) {
            logger.info("Alertas de hitos deshabilitadas (app.alertas.hitos.habilitado=false)");
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            logger.warn("No hay JavaMailSender disponible; se omiten alertas de hitos");
            return;
        }

        List<Usuario> destinatarios = repositorioUsuario.findActivosConRolesGestion(ROLES_GESTION);
        if (destinatarios.isEmpty()) {
            logger.info("Sin destinatarios activos con rol ADMIN/DIRECTIVO; se omiten alertas de hitos");
            return;
        }

        LocalDate limiteProximo = LocalDate.now().plusDays(propiedades.getDiasAnticipacion());

        List<HitoObra> proximos = repositorioHito.findHitosProximosAVencerSinNotificar(limiteProximo);
        List<HitoObra> vencidos = repositorioHito.findHitosVencidosSinNotificar();

        if (proximos.isEmpty() && vencidos.isEmpty()) {
            logger.info("Sin hitos relevantes que notificar hoy");
            return;
        }

        String[] correos = destinatarios.stream()
            .map(Usuario::getCorreoElectronico)
            .toArray(String[]::new);

        if (!proximos.isEmpty()) {
            enviarCorreo(sender, correos, asuntoProximos(proximos.size()), cuerpoProximos(proximos));
            proximos.forEach(HitoObra::registrarNotificacion);
            logger.info("Alerta 'próximos a vencer' enviada: {} hito(s)", proximos.size());
        }

        if (!vencidos.isEmpty()) {
            enviarCorreo(sender, correos, asuntoVencidos(vencidos.size()), cuerpoVencidos(vencidos));
            vencidos.forEach(HitoObra::registrarNotificacion);
            logger.info("Alerta 'vencidos sin cumplir' enviada: {} hito(s)", vencidos.size());
        }
    }

    private void enviarCorreo(JavaMailSender sender, String[] destinatarios, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedades.getRemitente());
        mensaje.setTo(destinatarios);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        sender.send(mensaje);
    }

    private String asuntoProximos(int cantidad) {
        return "[GPIV] " + cantidad + " hito(s) de obra próximo(s) a vencer";
    }

    private String asuntoVencidos(int cantidad) {
        return "[GPIV] " + cantidad + " hito(s) de obra vencido(s) sin cumplir";
    }

    private String cuerpoProximos(List<HitoObra> hitos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Los siguientes hitos de obra vencen en los próximos ")
          .append(propiedades.getDiasAnticipacion())
          .append(" día(s):\n\n");
        for (HitoObra h : hitos) {
            agregarLineaHito(sb, h);
        }
        sb.append("\nPor favor, verifique el avance de cada proyecto en el sistema.\n");
        return sb.toString();
    }

    private String cuerpoVencidos(List<HitoObra> hitos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Los siguientes hitos de obra están VENCIDOS y aún no han sido marcados como cumplidos:\n\n");
        for (HitoObra h : hitos) {
            agregarLineaHito(sb, h);
        }
        sb.append("\nPor favor, gestione la situación a la brevedad.\n");
        return sb.toString();
    }

    private void agregarLineaHito(StringBuilder sb, HitoObra h) {
        String empresa = "—";
        String proyecto = h.getProyecto().getNombre();
        if (h.getProyecto().getSolicitudOrigen() != null
                && h.getProyecto().getSolicitudOrigen().getEmpresa() != null) {
            empresa = h.getProyecto().getSolicitudOrigen().getEmpresa().getRazonSocial();
        }
        sb.append("• Empresa: ").append(empresa).append("\n");
        sb.append("  Proyecto: ").append(proyecto).append("\n");
        sb.append("  Hito: ").append(h.getDescripcion()).append("\n");
        sb.append("  Vence: ").append(h.getFechaVencimientoReal().format(FMT_FECHA)).append("\n\n");
    }
}