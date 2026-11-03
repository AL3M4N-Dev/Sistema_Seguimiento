package vista.alumno;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;

/**
 * SOLO PARA PROBAR la constancia del alumno sin pasar por el inicio de sesión.
 * En NetBeans: clic derecho en este archivo → Run File (Shift + F6).
 */
public class PruebaConstanciaAlumno {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignorada) {
            // Se queda la apariencia por defecto de Java
        }
        SwingUtilities.invokeLater(PruebaConstanciaAlumno::iniciar);
    }

    private static void iniciar() {
        List<Usuario> alumnos;
        try {
            alumnos = alumnos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar a la base servicio_social.\n"
                    + "Revisa que MySQL esté encendido en XAMPP.\n\nDetalle: " + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (alumnos.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No hay alumnos en la tabla usuarios.",
                    "Sin alumnos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] nombres = new String[alumnos.size()];
        for (int i = 0; i < nombres.length; i++) {
            Usuario u = alumnos.get(i);
            nombres[i] = u.getNombre() + (u.getMatricula() == null ? "" : " (" + u.getMatricula() + ")");
        }
        JComboBox<String> combo = new JComboBox<>(nombres);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Prueba: ¿con qué alumno quieres entrar?"), BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        int r = JOptionPane.showConfirmDialog(null, panel, "Probar constancia del alumno",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return;
        }
        Sesion.iniciar(alumnos.get(combo.getSelectedIndex()));

        ConstanciaAlumnoPanel constancia = new ConstanciaAlumnoPanel();
        JFrame ventana = new JFrame("Mi constancia · " + Sesion.getUsuario().getNombre() + " (prueba)");
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ventana.add(constancia);
        ventana.setSize(680, 380);
        ventana.setLocationRelativeTo(null);
        ventana.setVisible(true);
        constancia.recargar();
    }

    private static List<Usuario> alumnos() throws SQLException {
        String sql = "SELECT id_usuario, nombre, matricula, correo, rol, tipo_programa, activo FROM usuarios "
                + "WHERE rol = 'alumno' AND activo = 1 ORDER BY nombre";
        List<Usuario> lista = new ArrayList<>();
        try (Connection con = Conexion.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setMatricula(rs.getString("matricula"));
                u.setCorreo(rs.getString("correo"));
                u.setRol(rs.getString("rol"));
                u.setTipoPrograma(rs.getString("tipo_programa"));
                u.setActivo(rs.getBoolean("activo"));
                lista.add(u);
            }
        }
        return lista;
    }
}
