package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import modelo.Evidencia;
import util.Conexion;

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
}
