package vista.comun;

import dao.UsuarioDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import modelo.Usuario;
import util.Sesion;
import vista.docente.PanelDocenteFrame;
import vista.alumno.InstruccionesFrame;
import vista.alumno.MenuPrincipalFrame;

public class LoginFrame extends JFrame {

    private final JTextField txtCorreo = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JButton btnEntrar = Tema.anchoCompleto(Estilo.botonPrincipal("Iniciar sesión"));
    private final UsuarioDAO dao = new UsuarioDAO();

    public LoginFrame() {
        setTitle("Iniciar sesión · Seguimiento de Servicio Social y Prácticas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ---------- Estilo de los componentes ----------
        Tema.campo(txtCorreo);
        Tema.campo(txtPassword);
        JButton btnRegistro = Tema.anchoCompleto(Estilo.botonSecundario("Crear una cuenta"));
        JButton btnOlvide = Estilo.enlace("¿Olvidaste tu contraseña?");

        // Fila: "Mostrar contraseña" a la izquierda y el enlace a la derecha
        JPanel filaOpciones = new JPanel(new BorderLayout());
        filaOpciones.setOpaque(false);
        filaOpciones.add(VerPassword.crear(txtPassword), BorderLayout.WEST);
        filaOpciones.add(btnOlvide, BorderLayout.EAST);
        filaOpciones.setAlignmentX(Component.LEFT_ALIGNMENT);
        filaOpciones.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        // ---------- Tarjeta central ----------
        Tema.Tarjeta card = new Tema.Tarjeta(440);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(30, 36, 30, 36));

        card.add(Tema.titulo("Iniciar sesión"));
        card.add(Box.createVerticalStrut(4));
        card.add(Tema.subtitulo("Ingresa con tu correo y contraseña"));
        card.add(Box.createVerticalStrut(22));
        card.add(Tema.campoEtiquetado("Correo", txtCorreo));
        card.add(Box.createVerticalStrut(14));
        card.add(Tema.campoEtiquetado("Contraseña", txtPassword));
        card.add(Box.createVerticalStrut(8));
        card.add(filaOpciones);
        card.add(Box.createVerticalStrut(18));
        card.add(btnEntrar);
        card.add(Box.createVerticalStrut(20));
        card.add(Tema.separador());
        card.add(Box.createVerticalStrut(18));
        card.add(Tema.subtitulo("¿Aún no tienes cuenta?"));
        card.add(Box.createVerticalStrut(8));
        card.add(btnRegistro);

        // ---------- Página: barra superior + tarjeta centrada ----------
        JPanel centro = new JPanel(new GridBagLayout());
        centro.setBackground(Estilo.FONDO);
        centro.add(card);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.FONDO);
        raiz.add(Tema.barraSuperior(), BorderLayout.NORTH);
        raiz.add(centro, BorderLayout.CENTER);
        setContentPane(raiz);

        // ---------- Eventos ----------
        btnEntrar.addActionListener(e -> iniciarSesion());
        btnRegistro.addActionListener(e -> {
            new RegistroFrame().setVisible(true);
            dispose();
        });
        btnOlvide.addActionListener(e -> {
            new RecuperarPasswordFrame().setVisible(true);
            dispose();
        });
        getRootPane().setDefaultButton(btnEntrar);   // Enter = iniciar sesión

        setSize(1000, 700);
        setMinimumSize(new Dimension(560, 640));
        setLocationRelativeTo(null);
        Estilo.pantallaCompleta(this);
    }

    private void iniciarSesion() {
        String correo = txtCorreo.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (correo.isEmpty() || pass.isEmpty()) {
            aviso("Escribe tu correo y tu contraseña.");
            return;
        }
        try {
            Usuario u = dao.autenticar(correo, pass);
            if (u == null) {
                aviso("Correo o contraseña incorrectos.");
                txtPassword.setText("");
                return;
            }
            Sesion.iniciar(u);
            if (abrirSegunRol(u)) {
                dispose();
            } else {
                Sesion.cerrar();   // pantalla aún no disponible: no dejar sesión a medias
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo conectar con la base de datos.\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    //  IMPORTANTE: este método es el que ya conectaste con las pantallas de
    //  tus compañeros. Pega aquí TU versión (la de tu archivo anterior) para
    //  reemplazar esta, que solo trae los avisos temporales.
    // =====================================================================
    /** Devuelve true si abrió una ventana; false si la pantalla aún no existe. */
    private boolean abrirSegunRol(Usuario u) throws SQLException {
        if (u.esDocente() || u.esResponsable()) {
            new PanelDocenteFrame().setVisible(true);
            return true;
        }

        // Alumno
        if (!dao.tieneInscripcion(u.getIdUsuario())) {
            new InstruccionesFrame().setVisible(true);   // integrante 2 (nombre por confirmar)
        } else {
            new MenuPrincipalFrame().setVisible(true);   // integrante 3 (nombre por confirmar)
        }
        return true;
    }

    /** Cierra la sesión y vuelve al login. Uso: LoginFrame.cerrarSesion(this); */
    public static void cerrarSesion(Window ventanaActual) {
        int r = JOptionPane.showConfirmDialog(ventanaActual, "¿Cerrar sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        Sesion.cerrar();
        new LoginFrame().setVisible(true);   // primero se muestra el login...
        ventanaActual.dispose();             // ...y después se cierra la ventana actual
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}