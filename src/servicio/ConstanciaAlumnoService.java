package servicio;

import dao.ProgresoAlumnoDAO;
import dao.SolicitudConstanciaDAO;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import modelo.EstadoSolicitud;
import modelo.ProgresoAlumno;
import modelo.SolicitudConstancia;
import modelo.Usuario;

/**
 * Reglas de la constancia del lado del alumno:
 *  - Solo puede solicitarla al llegar al 100% de horas APROBADAS (se revisa en la base).
 *  - Solo hay una solicitud por inscripción: si se la rechazaron, vuelve a pedirla
 *    sobre la misma solicitud.
 *  - Solo puede descargarla cuando el responsable ya subió el PDF (estado 'disponible').
 */
public class ConstanciaAlumnoService {

    private final Usuario alumno;
    private final ProgresoAlumnoDAO progresos = new ProgresoAlumnoDAO();
    private final SolicitudConstanciaDAO solicitudes = new SolicitudConstanciaDAO();

    public ConstanciaAlumnoService(Usuario alumno) {
        if (alumno == null || !alumno.esAlumno()) {
            throw new IllegalArgumentException("La constancia del alumno es solo para alumnos.");
        }
        this.alumno = alumno;
    }

    public Usuario getAlumno() {
        return alumno;
    }

    /** El avance del alumno, o null si todavía no está inscrito. */
    public ProgresoAlumno progreso() throws SQLException {
        return progresos.buscarPorAlumno(alumno.getIdUsuario());
    }

    /** La solicitud del alumno para esa inscripción, o null si no ha pedido. */
    public SolicitudConstancia solicitud(ProgresoAlumno progreso) throws SQLException {
        return progreso == null ? null : solicitudes.buscarPorInscripcion(progreso.getIdInscripcion());
    }

    /** true si en este momento el botón "Solicitar constancia" debe estar activo. */
    public static boolean puedeSolicitar(ProgresoAlumno p, SolicitudConstancia s) {
        return p != null && p.completo() && (s == null || s.getEstado() == EstadoSolicitud.RECHAZADA);
    }

    public void solicitar() throws ReglaNegocioException, SQLException {
        ProgresoAlumno p = progreso();
        if (p == null) {
            throw new ReglaNegocioException("Aún no estás inscrito a una institución.");
        }
        if (!p.completo()) {
            throw new ReglaNegocioException("Todavía no puedes solicitarla: te faltan "
                    + texto(p.horasFaltantes()) + " horas aprobadas.");
        }
        SolicitudConstancia s = solicitudes.buscarPorInscripcion(p.getIdInscripcion());
        if (s == null) {
            try {
                solicitudes.crear(p.getIdInscripcion());
            } catch (SQLIntegrityConstraintViolationException e) {
                throw new ReglaNegocioException("Ya tienes una solicitud de constancia.");
            }
            return;
        }
        if (s.getEstado() != EstadoSolicitud.RECHAZADA) {
            throw new ReglaNegocioException("Ya tienes una solicitud de constancia ("
                    + s.getEstado().toString().toLowerCase() + ").");
        }
        if (!solicitudes.volverASolicitar(s.getIdSolicitud())) {
            throw new ReglaNegocioException("Tu solicitud cambió. Actualiza la pantalla.");
        }
    }

    /** Copia el PDF de la constancia a donde el alumno eligió guardarlo. */
    public void descargar(File destino) throws ReglaNegocioException, SQLException, IOException {
        SolicitudConstancia s = solicitud(progreso());
        if (s == null || s.getEstado() != EstadoSolicitud.DISPONIBLE) {
            throw new ReglaNegocioException("Tu constancia todavía no está disponible.");
        }
        File pdf = PanelDocenteService.archivoDe(s.getRutaPdf());
        if (pdf == null || !pdf.isFile()) {
            throw new ReglaNegocioException("No se encontró el PDF en esta computadora. Avisa al responsable de área.");
        }
        Files.copy(pdf.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static String texto(BigDecimal horas) {
        return horas == null || horas.signum() == 0 ? "0" : horas.stripTrailingZeros().toPlainString();
    }
}
