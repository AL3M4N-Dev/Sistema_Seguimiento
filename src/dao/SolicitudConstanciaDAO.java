package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import modelo.EstadoSolicitud;
import modelo.SolicitudConstancia;
import util.Conexion;

/**
 * Tabla solicitudes_constancia (hay UNA por inscripción: id_inscripcion es único).
 *  - Docente / responsable: aprobar con PDF o rechazar una solicitud.
 *  - Alumno: pedirla o volver a pedirla si se la rechazaron.
 */
public class SolicitudConstanciaDAO {

    private static final String SELECT_BASE =
            "SELECT s.id_solicitud, s.id_inscripcion, u.nombre AS alumno, u.matricula, ins.tipo, "
            + "       ins.nombre AS institucion, s.fecha_solicitud, s.estado, s.comentario, "
            + "       s.ruta_pdf, s.fecha_validacion, p.horas_requeridas, p.horas_aprobadas "
            + "FROM solicitudes_constancia s "
            + "JOIN inscripciones i     ON i.id_inscripcion = s.id_inscripcion "
            + "JOIN usuarios u          ON u.id_usuario = i.id_alumno "
            + "JOIN instituciones ins   ON ins.id_institucion = i.id_institucion "
            + "JOIN v_progreso_alumno p ON p.id_inscripcion = s.id_inscripcion ";

    /** La solicitud de una inscripción, o null si todavía no existe. */
    public SolicitudConstancia buscarPorInscripcion(int idInscripcion) throws SQLException {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE s.id_inscripcion = ?")) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Docente o responsable: guarda la ruta del PDF y deja la constancia 'disponible'
     * para que el alumno la descargue. Si el alumno no la había pedido, se crea la fila.
     * También marca la inscripción como 'completada'. Todo pasa junto o nada.
     */
    public void aprobarConPdf(int idInscripcion, int idRevisor, String rutaPdf) throws SQLException {
        String actualizar = "UPDATE solicitudes_constancia "
                + "SET estado = 'disponible', ruta_pdf = ?, id_responsable = ?, "
                + "    fecha_validacion = NOW(), comentario = NULL "
                + "WHERE id_inscripcion = ?";
        String crear = "INSERT INTO solicitudes_constancia "
                + "(id_inscripcion, estado, id_responsable, fecha_validacion, ruta_pdf) "
                + "VALUES (?, 'disponible', ?, NOW(), ?)";
        String completar = "UPDATE inscripciones SET estado = 'completada' WHERE id_inscripcion = ?";
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(actualizar);
                 PreparedStatement ps2 = con.prepareStatement(crear);
                 PreparedStatement ps3 = con.prepareStatement(completar)) {
                ps1.setString(1, rutaPdf);
                ps1.setInt(2, idRevisor);
                ps1.setInt(3, idInscripcion);
                if (ps1.executeUpdate() == 0) {
                    ps2.setInt(1, idInscripcion);
                    ps2.setInt(2, idRevisor);
                    ps2.setString(3, rutaPdf);
                    ps2.executeUpdate();
                }
                ps3.setInt(1, idInscripcion);
                ps3.executeUpdate();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Docente o responsable: solicitada o validada → rechazada, con el motivo para el alumno. */
    public boolean rechazar(int idSolicitud, int idRevisor, String comentario) throws SQLException {
        String sql = "UPDATE solicitudes_constancia "
                + "SET estado = 'rechazada', id_responsable = ?, comentario = ? "
                + "WHERE id_solicitud = ? AND estado IN ('solicitada', 'validada')";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRevisor);
            ps.setString(2, comentario);
            ps.setInt(3, idSolicitud);
            return ps.executeUpdate() == 1;
        }
    }

    /** Alumno: pide su constancia por primera vez (estado 'solicitada'). */
    public void crear(int idInscripcion) throws SQLException {
        String sql = "INSERT INTO solicitudes_constancia (id_inscripcion) VALUES (?)";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            ps.executeUpdate();
        }
    }

    /**
     * Alumno: rechazada → solicitada. Se reutiliza la misma fila porque solo puede
     * haber una solicitud por inscripción.
     */
    public boolean volverASolicitar(int idSolicitud) throws SQLException {
        String sql = "UPDATE solicitudes_constancia "
                + "SET estado = 'solicitada', fecha_solicitud = NOW(), id_responsable = NULL, "
                + "    fecha_validacion = NULL, comentario = NULL "
                + "WHERE id_solicitud = ? AND estado = 'rechazada'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idSolicitud);
            return ps.executeUpdate() == 1;
        }
    }

    private SolicitudConstancia mapear(ResultSet rs) throws SQLException {
        SolicitudConstancia s = new SolicitudConstancia();
        s.setIdSolicitud(rs.getInt("id_solicitud"));
        s.setIdInscripcion(rs.getInt("id_inscripcion"));
        s.setAlumnoNombre(rs.getString("alumno"));
        s.setMatricula(rs.getString("matricula"));
        s.setTipoPrograma(rs.getString("tipo"));
        s.setInstitucionNombre(rs.getString("institucion"));
        Timestamp solicitada = rs.getTimestamp("fecha_solicitud");
        s.setFechaSolicitud(solicitada == null ? null : solicitada.toLocalDateTime());
        s.setEstado(EstadoSolicitud.desdeBd(rs.getString("estado")));
        s.setComentario(rs.getString("comentario"));
        s.setRutaPdf(rs.getString("ruta_pdf"));
        Timestamp validada = rs.getTimestamp("fecha_validacion");
        s.setFechaValidacion(validada == null ? null : validada.toLocalDateTime());
        s.setHorasRequeridas(rs.getInt("horas_requeridas"));
        s.setHorasAprobadas(rs.getBigDecimal("horas_aprobadas"));
        return s;
    }
}
