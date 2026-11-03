package vista.docente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;
import vista.comun.Dialogos;
import vista.comun.Estilo;

/**
 * SOLO PARA PROBAR la parte 5 sin pasar por el inicio de sesión.
 * En NetBeans: clic derecho en este archivo → Run File (Shift + F6).
 * Elige un docente o responsable de la base y abre su panel.
 */
public class PruebaPanelDocente {

    public static void main(String[] args) {
        Estilo.aplicarTema();   // diseño base del proyecto
        SwingUtilities.invokeLater(PruebaPanelDocente::iniciar);
    }

    private static void iniciar() {
        List<Usuario> personal;
        try {
            personal = docentesYResponsables();
        } catch (SQLException e) {
            Dialogos.error(null, "No se pudo conectar a la base de datos",
                    "Revisa que MySQL esté encendido en XAMPP y que exista la base servicio_social.", e.getMessage());
            return;
        }
        if (personal.isEmpty()) {
            Dialogos.aviso(null, "No hay docentes",
                    "No hay docentes ni responsables en la tabla usuarios. Importa bd/datos_prueba_parte5.sql.");
            return;
        }
        Usuario elegido = Dialogos.elegir(null, "Probar panel del docente",
                "Solo para pruebas: elige con qué usuario quieres entrar.", personal,
                Usuario::getNombre,
                u -> ("responsable".equals(u.getRol()) ? "Responsable de área" : "Docente") + "  ·  " + u.getCorreo(),
                "Entrar");
        if (elegido != null) {
            Sesion.iniciar(elegido);
            PanelDocenteFrame panel = new PanelDocenteFrame();          // se abre en pantalla completa
            panel.setAccionCerrarSesion(PruebaPanelDocente::iniciar);   // "Cerrar sesión" vuelve a esta lista
            panel.setVisible(true);
        }
    }

    private static List<Usuario> docentesYResponsables() throws SQLException {
        String sql = "SELECT id_usuario, nombre, correo, rol, activo FROM usuarios "
                + "WHERE rol IN ('docente', 'responsable') AND activo = 1 ORDER BY rol, nombre";
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setCorreo(rs.getString("correo"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                lista.add(u);
            }
        }
        return lista;
    }
}
