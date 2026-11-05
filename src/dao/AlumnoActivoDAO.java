package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import modelo.AlumnoActivo;
import modelo.EstadoSolicitud;
import util.Conexion;

/**
 * Alumnos activos inscritos a una institución, con su avance (v_progreso_alumno)
 * y su constancia (solicitudes_constancia, si existe).
 */
public class AlumnoActivoDAO {

    private static final String SELECT_BASE =
            "SELECT u.id_usuario, u.nombre, u.matricula, u.carrera, u.correo, ins.tipo, "
            + "       i.id_inscripcion, i.estado AS estado_inscripcion, ins.nombre AS institucion, "
            + "       d.nombre AS docente, p.horas_requeridas, p.horas_aprobadas, p.horas_pendientes, "
            + "       s.id_solicitud, s.estado AS estado_solicitud, s.ruta_pdf, s.fecha_solicitud, "
            + "       s.fecha_validacion, s.comentario AS comentario_solicitud "
            + "FROM inscripciones i "
            + "JOIN usuarios u          ON u.id_usuario = i.id_alumno "
            + "JOIN instituciones ins   ON ins.id_institucion = i.id_institucion "
            + "JOIN v_progreso_alumno p ON p.id_inscripcion = i.id_inscripcion "
            + "LEFT JOIN usuarios d     ON d.id_usuario = i.id_docente "
            + "LEFT JOIN solicitudes_constancia s ON s.id_inscripcion = i.id_inscripcion "
            + "WHERE u.rol = 'alumno' AND u.activo = 1 ";

    private static final String FILTRO_DOCENTE = "AND (i.id_docente = ? OR i.id_docente IS NULL) ";

    /**
     * @param idDocente si no es null, solo los alumnos asignados a ese docente
     *                  (o sin docente asignado). null = todos (responsable de área).
     */
    public List<AlumnoActivo> listar(Integer idDocente) throws SQLException {
        String sql = SELECT_BASE + (idDocente == null ? "" : FILTRO_DOCENTE) + "ORDER BY u.nombre";
        List<AlumnoActivo> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (idDocente != null) {
                ps.setInt(1, idDocente);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Un alumno por su inscripción, con el mismo filtro de docente; null si no se encuentra. */
    public AlumnoActivo buscarPorInscripcion(int idInscripcion, Integer idDocente) throws SQLException {
        String sql = SELECT_BASE + "AND i.id_inscripcion = ? " + (idDocente == null ? "" : FILTRO_DOCENTE);
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            if (idDocente != null) {
                ps.setInt(2, idDocente);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    private AlumnoActivo mapear(ResultSet rs) throws SQLException {
        AlumnoActivo a = new AlumnoActivo();
        a.setIdAlumno(rs.getInt("id_usuario"));
        a.setNombre(rs.getString("nombre"));
        a.setMatricula(rs.getString("matricula"));
        a.setCarrera(rs.getString("carrera"));
        a.setCorreo(rs.getString("correo"));
        a.setTipoPrograma(rs.getString("tipo"));
        a.setIdInscripcion(rs.getInt("id_inscripcion"));
        a.setEstadoInscripcion(rs.getString("estado_inscripcion"));
        a.setInstitucion(rs.getString("institucion"));
        a.setDocenteAsignado(rs.getString("docente"));
        a.setHorasRequeridas(rs.getInt("horas_requeridas"));
        a.setHorasAprobadas(rs.getBigDecimal("horas_aprobadas"));
        a.setHorasPendientes(rs.getBigDecimal("horas_pendientes"));
        int idSolicitud = rs.getInt("id_solicitud");
        a.setIdSolicitud(rs.wasNull() ? null : idSolicitud);
        String estado = rs.getString("estado_solicitud");
        a.setEstadoConstancia(estado == null ? null : EstadoSolicitud.desdeBd(estado));
        a.setRutaPdf(rs.getString("ruta_pdf"));
        Timestamp solicitada = rs.getTimestamp("fecha_solicitud");
        a.setFechaSolicitud(solicitada == null ? null : solicitada.toLocalDateTime());
        Timestamp aprobada = rs.getTimestamp("fecha_validacion");
        a.setFechaAprobacion(aprobada == null ? null : aprobada.toLocalDateTime());
        a.setComentarioConstancia(rs.getString("comentario_solicitud"));
        return a;
    }
}
