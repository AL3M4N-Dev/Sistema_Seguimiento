package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import modelo.EstadoRegistro;
import modelo.RegistroHoras;
import util.Conexion;

/** Revisión de la tabla registros_horas (parte 5: panel del docente). */
public class RegistroHorasDAO {

    private static final String SELECT_BASE =
            "SELECT r.id_registro, r.id_inscripcion, u.nombre AS alumno, u.matricula, ins.tipo, "
            + "       ins.nombre AS institucion, r.fecha, r.hora_inicio, r.hora_fin, r.horas_calculadas, "
            + "       r.reporte, r.estado, r.comentario_docente, r.fecha_envio, ins.horas_requeridas "
            + "FROM registros_horas r "
            + "JOIN inscripciones i    ON i.id_inscripcion = r.id_inscripcion "
            + "JOIN usuarios u         ON u.id_usuario = i.id_alumno "
            + "JOIN instituciones ins  ON ins.id_institucion = i.id_institucion ";

    /**
     * Registros con el estado indicado.
     *
     * @param idDocente si no es null, solo los de alumnos asignados a ese docente
     *                  (o sin docente asignado). null = todos (responsable de área).
     */
    public List<RegistroHoras> listarPorEstado(EstadoRegistro estado, Integer idDocente) throws SQLException {
        String filtroDocente = idDocente == null ? "" : "AND (i.id_docente = ? OR i.id_docente IS NULL) ";
        String orden = estado == EstadoRegistro.PENDIENTE
                ? "ORDER BY r.fecha_envio ASC, r.id_registro ASC"
                : "ORDER BY r.fecha DESC, r.id_registro DESC LIMIT 200";
        String sql = SELECT_BASE + "WHERE r.estado = ? " + filtroDocente + orden;
        List<RegistroHoras> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado.getValorBd());
            if (idDocente != null) {
                ps.setInt(2, idDocente);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /**
     * Registros de una inscripción, del más reciente al más antiguo.
     *
     * @param estado si no es null, solo los de ese estado
     * @param limite cuántos como máximo (0 = todos)
     */
    public List<RegistroHoras> listarPorInscripcion(int idInscripcion, EstadoRegistro estado, int limite)
            throws SQLException {
        String sql = SELECT_BASE + "WHERE r.id_inscripcion = ? "
                + (estado == null ? "" : "AND r.estado = ? ")
                + "ORDER BY r.fecha DESC, r.hora_inicio DESC, r.id_registro DESC"
                + (limite > 0 ? " LIMIT " + limite : "");
        List<RegistroHoras> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            if (estado != null) {
                ps.setString(2, estado.getValorBd());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Cuántos turnos aprobados tiene una inscripción. */
    public int contarAprobados(int idInscripcion) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registros_horas WHERE id_inscripcion = ? AND estado = 'aprobado'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public RegistroHoras buscarPorId(int idRegistro) throws SQLException {
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(SELECT_BASE + "WHERE r.id_registro = ?")) {
            ps.setInt(1, idRegistro);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /** Horas aprobadas de una inscripción, según la vista v_progreso_alumno. */
    public BigDecimal horasAprobadas(int idInscripcion) throws SQLException {
        String sql = "SELECT horas_aprobadas FROM v_progreso_alumno WHERE id_inscripcion = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("horas_aprobadas") : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Aprueba o rechaza un registro que sigue pendiente.
     *
     * @return false si ya no estaba pendiente (otra persona lo revisó antes)
     */
    public boolean revisar(int idRegistro, EstadoRegistro nuevoEstado, int idDocente, String comentario)
            throws SQLException {
        String sql = "UPDATE registros_horas "
                + "SET estado = ?, id_docente_revisor = ?, comentario_docente = ?, fecha_revision = NOW() "
                + "WHERE id_registro = ? AND estado = 'pendiente'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.getValorBd());
            ps.setInt(2, idDocente);
            ps.setString(3, comentario);
            ps.setInt(4, idRegistro);
            return ps.executeUpdate() == 1;
        }
    }

    private RegistroHoras mapear(ResultSet rs) throws SQLException {
        RegistroHoras r = new RegistroHoras();
        r.setIdRegistro(rs.getInt("id_registro"));
        r.setIdInscripcion(rs.getInt("id_inscripcion"));
        r.setAlumnoNombre(rs.getString("alumno"));
        r.setMatricula(rs.getString("matricula"));
        r.setTipoPrograma(rs.getString("tipo"));
        r.setInstitucionNombre(rs.getString("institucion"));
        r.setFecha(rs.getObject("fecha", LocalDate.class));
        r.setHoraInicio(rs.getObject("hora_inicio", LocalTime.class));
        r.setHoraFin(rs.getObject("hora_fin", LocalTime.class));
        r.setHorasCalculadas(rs.getBigDecimal("horas_calculadas"));
        r.setReporte(rs.getString("reporte"));
        r.setEstado(EstadoRegistro.desdeBd(rs.getString("estado")));
        r.setComentarioDocente(rs.getString("comentario_docente"));
        Timestamp envio = rs.getTimestamp("fecha_envio");
        r.setFechaEnvio(envio == null ? null : envio.toLocalDateTime());
        r.setHorasRequeridas(rs.getInt("horas_requeridas"));
        return r;
    }
}
