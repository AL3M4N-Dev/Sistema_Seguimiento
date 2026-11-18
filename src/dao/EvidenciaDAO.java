package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.Evidencia;
import util.Conexion;
import java.sql.Statement;

/** Lee las evidencias de un registro (las guarda el módulo de registro de horas). */
public class EvidenciaDAO {

    public List<Evidencia> listarPorRegistro(int idRegistro) throws SQLException {
        String sql = "SELECT id_evidencia, id_registro, ruta_archivo, nombre_original "
                + "FROM evidencias WHERE id_registro = ? ORDER BY id_evidencia";
        List<Evidencia> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRegistro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Evidencia(
                            rs.getInt("id_evidencia"),
                            rs.getInt("id_registro"),
                            rs.getString("ruta_archivo"),
                            rs.getString("nombre_original")));
                }
            }
        }
        return lista;
    }
    /** Inserta una evidencia usando la conexión de una transacción abierta. */
    public void insertar(Connection con, int idRegistro, Evidencia e) throws SQLException {
        String sql = "INSERT INTO evidencias (id_registro, ruta_archivo, nombre_original) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idRegistro);
            ps.setString(2, e.getRutaArchivo());
            ps.setString(3, e.getNombreOriginal());
            ps.executeUpdate();
        }   
    }

    /** Elimina una evidencia usando la conexión de una transacción abierta. */
    public void eliminar(Connection con, int idEvidencia) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM evidencias WHERE id_evidencia = ?")) {
            ps.setInt(1, idEvidencia);
            ps.executeUpdate();
        }
    }
}
