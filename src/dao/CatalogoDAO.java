package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import modelo.InstitucionCatalogo;
import util.Conexion;

public class CatalogoDAO {

    // Instituciones de servicio social que se muestran en el catálogo (disponibles y llenas).
    public List<InstitucionCatalogo> listarServicioSocial() throws SQLException {
        String sql = "SELECT id_institucion, nombre, direccion, contacto, tipo, "
                + "       horas_requeridas, cupos_totales, cupos_disponibles, estado "
                + "FROM instituciones "
                + "WHERE tipo = 'servicio_social' AND estado IN ('disponible', 'lleno') "
                + "ORDER BY nombre";
        List<InstitucionCatalogo> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                InstitucionCatalogo i = new InstitucionCatalogo();
                i.setIdInstitucion(rs.getInt("id_institucion"));
                i.setNombre(rs.getString("nombre"));
                i.setDireccion(rs.getString("direccion"));
                i.setContacto(rs.getString("contacto"));
                i.setTipo(rs.getString("tipo"));
                i.setHorasRequeridas(rs.getInt("horas_requeridas"));
                int total = rs.getInt("cupos_totales");
                i.setCuposTotales(rs.wasNull() ? null : total);
                int disponibles = rs.getInt("cupos_disponibles");
                i.setCuposDisponibles(rs.wasNull() ? null : disponibles);
                i.setEstado(rs.getString("estado"));
                lista.add(i);
            }
        }
        return lista;
    }

    // El alumno da de alta su institución de prácticas. Queda "en_revision" y el alumno
    // se inscribe en ella. Todo en una transacción: o se guardan las dos cosas, o ninguna.
    public int crearInstitucionPracticas(int idAlumno, String nombre, String direccion,
            String contacto, int horasRequeridas) throws SQLException {
        String sqlInstitucion = "INSERT INTO instituciones "
                + "(nombre, direccion, contacto, tipo, horas_requeridas, estado, creada_por) "
                + "VALUES (?, ?, ?, 'practicas', ?, 'en_revision', ?)";
        String sqlInscripcion = "INSERT INTO inscripciones (id_alumno, id_institucion) VALUES (?, ?)";

        try (Connection con = Conexion.getConexion()) {
            con.setAutoCommit(false);
            try {
                int idInstitucion;
                try (PreparedStatement ps = con.prepareStatement(
                        sqlInstitucion, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.setString(2, direccion);
                    ps.setString(3, contacto);
                    ps.setInt(4, horasRequeridas);
                    ps.setInt(5, idAlumno);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("No se pudo guardar la institución.");
                        }
                        idInstitucion = keys.getInt(1);
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
                return idInstitucion;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }
}