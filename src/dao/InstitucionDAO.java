package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import modelo.Institucion;
import util.Conexion;

/** Revisión de las instituciones de prácticas que dan de alta los alumnos. */
public class InstitucionDAO {

    public List<Institucion> listarEnRevision() throws SQLException {
        String sql = "SELECT ins.id_institucion, ins.nombre, ins.direccion, ins.contacto, ins.tipo, "
                + "       ins.horas_requeridas, ins.estado, ins.fecha_alta, u.nombre AS alumno "
                + "FROM instituciones ins "
                + "LEFT JOIN usuarios u ON u.id_usuario = ins.creada_por "
                + "WHERE ins.estado = 'en_revision' "
                + "ORDER BY ins.fecha_alta ASC";
        List<Institucion> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Institucion i = new Institucion();
                i.setIdInstitucion(rs.getInt("id_institucion"));
                i.setNombre(rs.getString("nombre"));
                i.setDireccion(rs.getString("direccion"));
                i.setContacto(rs.getString("contacto"));
                i.setTipo(rs.getString("tipo"));
                i.setHorasRequeridas(rs.getInt("horas_requeridas"));
                i.setEstado(rs.getString("estado"));
                Timestamp alta = rs.getTimestamp("fecha_alta");
                i.setFechaAlta(alta == null ? null : alta.toLocalDateTime());
                i.setCreadaPorNombre(rs.getString("alumno"));
                lista.add(i);
            }
        }
        return lista;
    }

    /** La institución pasa a 'disponible' y se guarda quién la aprobó. */
    public boolean aprobar(int idInstitucion, int idDocente) throws SQLException {
        String sql = "UPDATE instituciones SET estado = 'disponible', aprobada_por = ? "
                + "WHERE id_institucion = ? AND estado = 'en_revision'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDocente);
            ps.setInt(2, idInstitucion);
            return ps.executeUpdate() == 1;
        }
    }

    /** La institución pasa a 'rechazada'. */
    public boolean rechazar(int idInstitucion) throws SQLException {
        String sql = "UPDATE instituciones SET estado = 'rechazada' "
                + "WHERE id_institucion = ? AND estado = 'en_revision'";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInstitucion);
            return ps.executeUpdate() == 1;
        }
    }
}
