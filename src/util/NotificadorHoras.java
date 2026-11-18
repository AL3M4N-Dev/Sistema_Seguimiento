package util;

import dao.InscripcionDAO;
import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Avisa por correo al docente cuando un alumno manda un turno a revisión. */
public class NotificadorHoras {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * @return true si se envió; false si no hay correo configurado o no hay a quién avisar
     */
    public static boolean avisarTurnoPendiente(int idInscripcion, String alumno, boolean esReenvio,
            LocalDate fecha, LocalTime inicio, LocalTime fin, BigDecimal horas, String reporte)
            throws SQLException, MessagingException {
        if (!CorreoUtil.configurado()) {
            return false;
        }
        List<String> correos = new InscripcionDAO().correosParaRevision(idInscripcion);
        if (correos.isEmpty()) {
            return false;
        }
        String asunto = (esReenvio ? "Turno corregido" : "Nuevo turno")
                + " pendiente de revisión - " + alumno;
        String cuerpo = alumno + (esReenvio ? " corrigió y reenvió" : " registró")
                + " un turno que está pendiente de tu revisión.\n\n"
                + "Fecha: " + fecha.format(FECHA) + "\n"
                + "Horario: " + inicio.format(HORA) + " a " + fin.format(HORA) + "\n"
                + "Horas: " + horas + "\n\n"
                + "Reporte:\n" + reporte + "\n\n"
                + "Entra al sistema para aprobarlo o rechazarlo.";
        CorreoUtil.enviar(String.join(",", correos), asunto, cuerpo);
        return true;
    }
}