package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.Conexion;

/** Consultas de solo lectura que necesita el menú principal del alumno (parte 3). */
public class MenuAlumnoDAO {

    /**
     * Estado de la institución donde está inscrito el alumno: 'disponible', 'lleno',
     * 'en_revision' o 'rechazada'. Devuelve null si la inscripción no existe.
     */
    public String estadoInstitucion(int idInscripcion) throws SQLException {
        String sql = "SELECT ins.estado "
                + "FROM inscripciones i "
                + "JOIN instituciones ins ON ins.id_institucion = i.id_institucion "
                + "WHERE i.id_inscripcion = ?";
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idInscripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("estado") : null;
            }
        }
    }
}