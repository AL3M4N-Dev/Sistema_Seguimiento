package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import modelo.EstadoSolicitud;
import modelo.SolicitudConstancia;
import util.Conexion;

/**
 * Solicitudes de constancia: validar las horas, marcar como disponible con su PDF,
 * o rechazar. Las horas aprobadas salen de la vista v_progreso_alumno.
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

    /** @param estado el filtro, o null para ver todas */
    public List<SolicitudConstancia> listar(EstadoSolicitud estado) throws SQLException {
        String sql = SELECT_BASE
                + (estado == null ? "" : "WHERE s.estado = ? ")
                + "ORDER BY s.fecha_solicitud ASC";
        List<SolicitudConstancia> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (estado != null) {
                ps.setString(1, estado.getValorBd());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public SolicitudConstancia buscarPorId(int idSolicitud) throws SQLException {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE s.id_solicitud = ?")) {
            ps.setInt(1, idSolicitud);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** solicitada → validada: el responsable confirmó que las horas están completas. */
    public boolean validar(int idSolicitud, int idResponsable) throws SQLException {
        String sql = "UPDATE solicitudes_constancia "
                + "SET estado = 'validada', id_responsable = ?, fecha_validacion = NOW(), comentario = NULL "
                + "WHERE id_solicitud = ? AND estado = 'solicitada'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idResponsable);
            ps.setInt(2, idSolicitud);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * validada → disponible: guarda la ruta del PDF y marca la inscripción como completada.
     * Las dos cosas pasan juntas o ninguna.
     */
    public boolean marcarDisponible(int idSolicitud, int idInscripcion, int idResponsable, String rutaPdf)
            throws SQLException {
        String sqlSolicitud = "UPDATE solicitudes_constancia "
                + "SET estado = 'disponible', ruta_pdf = ?, id_responsable = ? "
                + "WHERE id_solicitud = ? AND estado = 'validada'";
        String sqlInscripcion = "UPDATE inscripciones SET estado = 'completada' WHERE id_inscripcion = ?";
        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps1 = con.prepareStatement(sqlSolicitud);
                 PreparedStatement ps2 = con.prepareStatement(sqlInscripcion)) {
                ps1.setString(1, rutaPdf);
                ps1.setInt(2, idResponsable);
                ps1.setInt(3, idSolicitud);
                if (ps1.executeUpdate() != 1) {
                    con.rollback();
                    return false;
                }
                ps2.setInt(1, idInscripcion);
                ps2.executeUpdate();
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** solicitada o validada → rechazada, con el motivo para el alumno. */
    public boolean rechazar(int idSolicitud, int idResponsable, String comentario) throws SQLException {
        String sql = "UPDATE solicitudes_constancia "
                + "SET estado = 'rechazada', id_responsable = ?, comentario = ? "
                + "WHERE id_solicitud = ? AND estado IN ('solicitada', 'validada')";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idResponsable);
            ps.setString(2, comentario);
            ps.setInt(3, idSolicitud);
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
