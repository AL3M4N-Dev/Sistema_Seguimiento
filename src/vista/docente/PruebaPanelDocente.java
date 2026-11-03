package vista.docente;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;

/**
 * SOLO PARA PROBAR la parte 5 sin pasar por el inicio de sesión.
 * En NetBeans: clic derecho en este archivo → Run File (Shift + F6).
 * Elige un docente o responsable de la base y abre su panel.
 */
public class PruebaPanelDocente {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignorada) {
            // Se queda la apariencia por defecto de Java
        }
        SwingUtilities.invokeLater(PruebaPanelDocente::iniciar);
    }

    private static void iniciar() {
        List<Usuario> personal;
        try {
            personal = docentesYResponsables();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar a la base servicio_social.\n"
                    + "Revisa que MySQL esté encendido en XAMPP.\n\nDetalle: " + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (personal.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay docentes ni responsables en la tabla usuarios.",
                    "Sin usuarios", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] nombres = new String[personal.size()];
        for (int i = 0; i < nombres.length; i++) {
            Usuario u = personal.get(i);
            nombres[i] = u.getNombre() + " (" + u.getRol() + ")";
        }
        JComboBox<String> combo = new JComboBox<>(nombres);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Prueba: ¿con qué usuario quieres entrar?"), BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(null, panel, "Probar panel del docente",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION) {
            Sesion.iniciar(personal.get(combo.getSelectedIndex()));
            new PanelDocenteFrame().setVisible(true);
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
