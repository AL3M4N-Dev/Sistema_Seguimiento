package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import modelo.Inscripcion;
import util.Conexion;
import java.util.ArrayList;
import java.util.List;

public class InscripcionDAO {

    // Devuelve la inscripción del alumno, o null si todavía no se ha inscrito.
    public Inscripcion buscarPorAlumno(int idAlumno) throws SQLException {
        String sql = "SELECT id_inscripcion, id_alumno, id_institucion, id_docente, "
                + "       fecha_inscripcion, estado "
                + "FROM inscripciones WHERE id_alumno = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Inscripcion ins = new Inscripcion();
                ins.setIdInscripcion(rs.getInt("id_inscripcion"));
                ins.setIdAlumno(rs.getInt("id_alumno"));
                ins.setIdInstitucion(rs.getInt("id_institucion"));
                int docente = rs.getInt("id_docente");
                ins.setIdDocente(rs.wasNull() ? null : docente);
                Timestamp fecha = rs.getTimestamp("fecha_inscripcion");
                ins.setFechaInscripcion(fecha == null ? null : fecha.toLocalDateTime());
                ins.setEstado(rs.getString("estado"));
                return ins;
            }
        }
    }

    // True si el alumno ya tiene una inscripción.
    public boolean estaInscrito(int idAlumno) throws SQLException {
        return buscarPorAlumno(idAlumno) != null;
    }

    // Inscribe al alumno en una institución de servicio social y descuenta un cupo.
    // Todo ocurre en una transacción: o se hacen las dos cosas, o ninguna.
    public void inscribir(int idAlumno, int idInstitucion) throws SQLException {
        String sqlCupo = "UPDATE instituciones "
                + "SET cupos_disponibles = cupos_disponibles - 1, "
                + "    estado = IF(cupos_disponibles = 0, 'lleno', estado) "
                + "WHERE id_institucion = ? AND tipo = 'servicio_social' "
                + "  AND estado = 'disponible' AND cupos_disponibles > 0";
        String sqlInscripcion = "INSERT INTO inscripciones (id_alumno, id_institucion) VALUES (?, ?)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlCupo)) {
                    ps.setInt(1, idInstitucion);
                    if (ps.executeUpdate() == 0) {
                        throw new IllegalStateException(
                                "Esta institución ya no tiene cupos disponibles.");
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(sqlInscripcion)) {
                    ps.setInt(1, idAlumno);
                    ps.setInt(2, idInstitucion);
                    ps.executeUpdate();
                } catch (SQLIntegrityConstraintViolationException e) {
                    throw new IllegalStateException("Ya estás inscrito en una institución.");
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }
    
        /** Id de la inscripción activa del alumno, o null si no tiene. */
    public Integer idInscripcionActiva(int idAlumno) throws SQLException {
        String sql = "SELECT id_inscripcion FROM inscripciones "
                + "WHERE id_alumno = ? AND estado = 'activa' ORDER BY fecha_inscripcion DESC LIMIT 1";
        try (Connection con = Conexion.getConexion();
            PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** Correos a avisar cuando llega un turno: el docente asignado o, si no hay, todos los docentes activos. */
    public List<String> correosParaRevision(int idInscripcion) throws SQLException {
        String asignado = "SELECT u.correo FROM inscripciones i "
                + "JOIN usuarios u ON u.id_usuario = i.id_docente "
                + "WHERE i.id_inscripcion = ? AND u.activo = 1";
        String todos = "SELECT correo FROM usuarios WHERE rol = 'docente' AND activo = 1";
        List<String> correos = new ArrayList<>();
        try (Connection con = Conexion.getConexion()) {
            try (PreparedStatement ps = con.prepareStatement(asignado)) {
                ps.setInt(1, idInscripcion);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) correos.add(rs.getString(1));
                }
            }
            if (correos.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement(todos);
                    ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) correos.add(rs.getString(1));
                }
            }
        }
        return correos;
    }
}
