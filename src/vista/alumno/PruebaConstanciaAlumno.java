package vista.alumno;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import modelo.Usuario;
import util.Conexion;
import util.Sesion;
import vista.comun.Dialogos;
import vista.comun.EncabezadoApp;
import vista.comun.Estilo;

/**
 * SOLO PARA PROBAR la constancia del alumno sin pasar por el inicio de sesión.
 * En NetBeans: clic derecho en este archivo → Run File (Shift + F6).
 */
public class PruebaConstanciaAlumno {

    private static final int ANCHO_COLUMNA = 840;

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
        // "Cerrar sesión" vuelve a mostrar la lista para entrar con otro alumno
        JFrame ventana = crearVentana(elegido, constancia, PruebaConstanciaAlumno::iniciar);
        ventana.setVisible(true);
        constancia.recargar();
    }

    /**
     * Ventana de prueba con el diseño base: barra de arriba y la sección del alumno al centro.
     * Se abre en pantalla completa (maximizada).
     */
    static JFrame crearVentana(Usuario alumno, ConstanciaAlumnoPanel constancia, Runnable despuesDeCerrarSesion) {
        JFrame ventana = new JFrame("Seguimiento · Mi constancia (prueba)");
        ventana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        EncabezadoApp encabezado = new EncabezadoApp(alumno.getNombre() + " · Alumno");
        encabezado.agregarSeccion("constancia", "Mi constancia");
        encabezado.seleccionar("constancia");
        encabezado.alCerrarSesion(() -> {
            Sesion.cerrar();
            ventana.dispose();
            despuesDeCerrarSesion.run();
        });

        JPanel textos = Estilo.panel(new BorderLayout(0, 4));
        textos.add(Estilo.titulo("Mi constancia"), BorderLayout.NORTH);
        textos.add(Estilo.secundario("Prueba de la sección que irá en el menú principal del alumno."), BorderLayout.SOUTH);
        JPanel columna = new JPanel(new BorderLayout(0, 16)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(ANCHO_COLUMNA, super.getPreferredSize().height);
            }
        };
        columna.setOpaque(false);
        columna.add(textos, BorderLayout.NORTH);
        columna.add(constancia, BorderLayout.CENTER);

        // La columna queda centrada y arriba, aunque la ventana sea muy ancha
        JPanel fondo = new JPanel(new GridBagLayout());
        fondo.setBackground(Estilo.FONDO);
        fondo.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTH;
        c.weightx = 1;
        c.weighty = 1;
        fondo.add(columna, c);

        ventana.setLayout(new BorderLayout());
        ventana.add(encabezado, BorderLayout.NORTH);
        ventana.add(fondo, BorderLayout.CENTER);
        ventana.getContentPane().setBackground(Estilo.FONDO);
        ventana.setMinimumSize(new Dimension(920, 620));
        ventana.setSize(1280, 800);
        ventana.setLocationRelativeTo(null);
        Estilo.pantallaCompleta(ventana);
        return ventana;
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
