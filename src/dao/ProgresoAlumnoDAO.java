package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import modelo.ProgresoAlumno;
import util.Conexion;

/** Lee el avance de horas de un alumno desde la vista v_progreso_alumno. */
public class ProgresoAlumnoDAO {

    /** El avance de la inscripción del alumno, o null si todavía no está inscrito. */
    public ProgresoAlumno buscarPorAlumno(int idAlumno) throws SQLException {
        String sql = "SELECT id_inscripcion, institucion, horas_requeridas, horas_aprobadas, "
                + "       horas_pendientes, porcentaje "
                + "FROM v_progreso_alumno WHERE id_alumno = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAlumno);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                ProgresoAlumno p = new ProgresoAlumno();
                p.setIdInscripcion(rs.getInt("id_inscripcion"));
                p.setInstitucion(rs.getString("institucion"));
                p.setHorasRequeridas(rs.getInt("horas_requeridas"));
                p.setHorasAprobadas(rs.getBigDecimal("horas_aprobadas"));
                p.setHorasPendientes(rs.getBigDecimal("horas_pendientes"));
                p.setPorcentaje(rs.getBigDecimal("porcentaje"));
                return p;
            }
        }
    }
}
