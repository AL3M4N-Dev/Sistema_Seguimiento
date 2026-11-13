package vista.comun;

import dao.UsuarioDAO;
import java.awt.*;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.swing.*;
import modelo.Usuario;

public class RegistroFrame extends JFrame {

    /**
     * Código que debe escribir quien se registre como docente, para que
     * cualquiera no pueda hacerse docente. Ponlo en null para quitarlo.
     */
    private static final String CODIGO_DOCENTE = "DOCENTE2026";

    private static final Pattern CORREO =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final JComboBox<String> cmbRol =
            new JComboBox<>(new String[]{"Alumno", "Docente"});
    private final JTextField txtNombre = new JTextField();
    private final JTextField txtMatricula = new JTextField();
    private final JTextField txtCarrera = new JTextField();
    private final JComboBox<String> cmbPrograma =
            new JComboBox<>(new String[]{"Servicio social", "Prácticas profesionales"});
    private final JTextField txtCorreo = new JTextField();
    private final JPasswordField txtPassword = new JPasswordField();
    private final JPasswordField txtConfirmar = new JPasswordField();
    private final JPasswordField txtCodigo = new JPasswordField();

    private JComponent filaAlumno;   // matrícula + carrera (solo alumnos)
    private JPanel espacioRol;       // programa (alumno) o código (docente)
    private final UsuarioDAO dao = new UsuarioDAO();

    public RegistroFrame() {
        setTitle("Crear cuenta · Seguimiento de Servicio Social y Prácticas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ---------- Estilo de los componentes ----------
        Tema.combo(cmbRol);
        Tema.combo(cmbPrograma);
        Tema.campo(txtNombre);
        Tema.campo(txtMatricula);
        Tema.campo(txtCarrera);
        Tema.campo(txtCorreo);
        Tema.campo(txtPassword);
        Tema.campo(txtConfirmar);
        Tema.campo(txtCodigo);

        JButton btnRegistrar = Tema.anchoCompleto(Estilo.botonPrincipal("Crear cuenta"));
        JButton btnVolver = Tema.anchoCompleto(Estilo.botonSecundario("Ya tengo cuenta · Iniciar sesión"));

        // Al lado del rol: el programa (alumnos) o el código (docentes)
        espacioRol = new JPanel(new CardLayout());
        espacioRol.setOpaque(false);
        espacioRol.add(Tema.campoEtiquetado("Programa", cmbPrograma), "alumno");
        JPanel docente = new JPanel(new BorderLayout());
        docente.setOpaque(false);
        if (CODIGO_DOCENTE != null) {
            docente.add(Tema.campoEtiquetado("Código de docente", txtCodigo));
        }
        espacioRol.add(docente, "docente");

        filaAlumno = Tema.conEspacio(Tema.dos(
                Tema.campoEtiquetado("Matrícula", txtMatricula),
                Tema.campoEtiquetado("Carrera", txtCarrera)), 12);

        // ---------- Tarjeta central ----------
        Tema.Tarjeta card = new Tema.Tarjeta(620);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 36, 24, 36));

        card.add(Tema.titulo("Crear cuenta"));
        card.add(Box.createVerticalStrut(4));
        card.add(Tema.subtitulo("Regístrate para dar seguimiento a tu servicio social o prácticas"));
        card.add(Box.createVerticalStrut(16));
        card.add(Tema.conEspacio(Tema.dos(
                Tema.campoEtiquetado("Me registro como", cmbRol), espacioRol), 12));
        card.add(Tema.conEspacio(Tema.campoEtiquetado("Nombre completo", txtNombre), 12));
        card.add(filaAlumno);
        card.add(Tema.conEspacio(Tema.campoEtiquetado("Correo", txtCorreo), 12));
        card.add(Tema.conEspacio(Tema.dos(
                Tema.campoEtiquetado("Contraseña (mín. 8)", txtPassword),
                Tema.campoEtiquetado("Confirmar contraseña", txtConfirmar)), 6));
        card.add(VerPassword.crear(txtPassword, txtConfirmar, txtCodigo));
        card.add(Box.createVerticalStrut(14));
        card.add(btnRegistrar);
        card.add(Box.createVerticalStrut(8));
        card.add(btnVolver);

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
        cmbRol.addActionListener(e -> actualizarCampos());
        btnRegistrar.addActionListener(e -> registrar());
        btnVolver.addActionListener(e -> volverAlLogin());

        actualizarCampos();
        setSize(1000, 700);
        setMinimumSize(new Dimension(700, 660));
        setLocationRelativeTo(null);
        Estilo.pantallaCompleta(this);
    }

    /** Muestra u oculta campos según el rol elegido. */
    private void actualizarCampos() {
        boolean alumno = cmbRol.getSelectedIndex() == 0;
        filaAlumno.setVisible(alumno);
        ((CardLayout) espacioRol.getLayout()).show(espacioRol, alumno ? "alumno" : "docente");
        revalidate();
        repaint();
    }

    private void registrar() {
        boolean alumno = cmbRol.getSelectedIndex() == 0;
        String nombre = txtNombre.getText().trim();
        String matricula = txtMatricula.getText().trim();
        String carrera = txtCarrera.getText().trim();
        String correo = txtCorreo.getText().trim().toLowerCase();
        String pass = new String(txtPassword.getPassword());
        String confirmar = new String(txtConfirmar.getPassword());

        // ---------- Validaciones ----------
        if (nombre.isEmpty() || correo.isEmpty() || pass.isEmpty()) {
            aviso("Completa nombre, correo y contraseña.");
            return;
        }
        if (alumno && (matricula.isEmpty() || carrera.isEmpty())) {
            aviso("Los alumnos deben escribir su matrícula y su carrera.");
            return;
        }
        if (!CORREO.matcher(correo).matches()) {
            aviso("El correo no tiene un formato válido.");
            return;
        }
        if (pass.length() < 8) {
            aviso("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        if (!pass.equals(confirmar)) {
            aviso("Las contraseñas no coinciden.");
            return;
        }
        if (!alumno && CODIGO_DOCENTE != null
                && !CODIGO_DOCENTE.equals(new String(txtCodigo.getPassword()))) {
            aviso("El código de docente es incorrecto.");
            return;
        }

        try {
            if (dao.existeCorreo(correo)) {
                aviso("Ya existe una cuenta con ese correo.");
                return;
            }
            if (alumno && dao.existeMatricula(matricula)) {
                aviso("Esa matrícula ya está registrada.");
                return;
            }

            Usuario u = new Usuario();
            u.setNombre(nombre);
            u.setCorreo(correo);
            u.setRol(alumno ? "alumno" : "docente");
            if (alumno) {
                u.setMatricula(matricula);
                u.setCarrera(carrera);
                u.setTipoPrograma(cmbPrograma.getSelectedIndex() == 0
                        ? "servicio_social" : "practicas");
            }

            if (dao.registrar(u, pass)) {
                JOptionPane.showMessageDialog(this,
                        "Cuenta creada. Ya puedes iniciar sesión.",
                        "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
                volverAlLogin();
            } else {
                aviso("No se pudo crear la cuenta. Intenta de nuevo.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error con la base de datos.\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void volverAlLogin() {
        new LoginFrame().setVisible(true);
        dispose();
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
}