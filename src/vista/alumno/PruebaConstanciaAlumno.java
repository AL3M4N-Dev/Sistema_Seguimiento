package vista.alumno;

import java.awt.BorderLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;
import vista.comun.Dialogos;
import vista.comun.Estilo;

/**
 * SOLO PARA PROBAR la constancia del alumno sin pasar por el inicio de sesión.
 * En NetBeans: clic derecho en este archivo → Run File (Shift + F6).
 */
public class PruebaConstanciaAlumno {

    public static void main(String[] args) {
        Estilo.aplicarTema();   // diseño base del proyecto
        SwingUtilities.invokeLater(PruebaConstanciaAlumno::iniciar);
    }

    private static void iniciar() {
        List<Usuario> alumnos;
        try {
            alumnos = alumnos();
        } catch (SQLException e) {
            Dialogos.error(null, "No se pudo conectar a la base de datos",
                    "Revisa que MySQL esté encendido en XAMPP y que exista la base servicio_social.", e.getMessage());
            return;
        }
        if (alumnos.isEmpty()) {
            Dialogos.aviso(null, "No hay alumnos",
                    "No hay alumnos en la tabla usuarios. Importa bd/datos_prueba_parte5.sql.");
            return;
        }
        Usuario elegido = Dialogos.elegir(null, "Probar constancia del alumno",
                "Solo para pruebas: elige con qué alumno quieres entrar.", alumnos,
                Usuario::getNombre,
                u -> (u.getMatricula() == null ? "Sin matrícula" : "Matrícula " + u.getMatricula()) + "  ·  "
                        + ("practicas".equals(u.getTipoPrograma()) ? "Prácticas" : "Servicio social"),
                "Entrar");
        if (elegido == null) {
            return;
        }
        Sesion.iniciar(elegido);

        ConstanciaAlumnoPanel constancia = new ConstanciaAlumnoPanel();
        JFrame ventana = new JFrame("Mi constancia · " + Sesion.getUsuario().getNombre() + " (prueba)");
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel fondo = new JPanel(new BorderLayout());
        fondo.setBackground(Estilo.FONDO);
        fondo.setBorder(javax.swing.BorderFactory.createEmptyBorder(24, 24, 24, 24));
        fondo.add(constancia, BorderLayout.NORTH);
        ventana.setContentPane(fondo);
        ventana.setSize(720, 460);
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
