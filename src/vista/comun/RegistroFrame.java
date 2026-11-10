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
    private final JTextField txtNombre = new JTextField(22);
    private final JTextField txtMatricula = new JTextField(22);
    private final JTextField txtCarrera = new JTextField(22);
    private final JComboBox<String> cmbPrograma =
            new JComboBox<>(new String[]{"Servicio social", "Prácticas profesionales"});
    private final JTextField txtCorreo = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JPasswordField txtConfirmar = new JPasswordField(22);
    private final JPasswordField txtCodigo = new JPasswordField(22);

    private JLabel lblMatricula, lblCarrera, lblPrograma, lblCodigo;
    private final UsuarioDAO dao = new UsuarioDAO();

    public RegistroFrame() {
        setTitle("Seguimiento de Servicio Social y Prácticas - Registro");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));

        JLabel titulo = new JLabel("Crear cuenta", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        GridBagConstraints gt = new GridBagConstraints();
        gt.gridx = 0; gt.gridy = 0; gt.gridwidth = 2;
        gt.insets = new Insets(0, 0, 12, 0);
        gt.fill = GridBagConstraints.HORIZONTAL;
        p.add(titulo, gt);

        int f = 1;
        fila(p, f++, "Me registro como", cmbRol);
        fila(p, f++, "Nombre completo", txtNombre);
        lblMatricula = fila(p, f++, "Matrícula", txtMatricula);
        lblCarrera = fila(p, f++, "Carrera", txtCarrera);
        lblPrograma = fila(p, f++, "Programa", cmbPrograma);
        fila(p, f++, "Correo", txtCorreo);
        fila(p, f++, "Contraseña (mín. 8)", txtPassword);
        fila(p, f++, "Confirmar contraseña", txtConfirmar);
        lblCodigo = fila(p, f++, "Código de docente", txtCodigo);

        JButton btnRegistrar = new JButton("Registrarme");
        JButton btnVolver = new JButton("Atras");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        botones.add(btnVolver);
        botones.add(btnRegistrar);
        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0; gb.gridy = f; gb.gridwidth = 2;
        gb.insets = new Insets(16, 0, 0, 0);
        p.add(botones, gb);

        cmbRol.addActionListener(e -> actualizarCampos());
        btnRegistrar.addActionListener(e -> registrar());
        btnVolver.addActionListener(e -> volverAlLogin());

        setContentPane(p);
        actualizarCampos();
        setLocationRelativeTo(null);
    }

    /** Agrega etiqueta + campo en la fila indicada y devuelve la etiqueta. */
    private JLabel fila(JPanel p, int y, String texto, JComponent campo) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = y;

        JLabel l = new JLabel(texto);
        g.gridx = 0; g.weightx = 0;
        p.add(l, g);
        g.gridx = 1; g.weightx = 1;
        p.add(campo, g);
        campo.putClientProperty("etiqueta", l);
        return l;
    }

    /** Muestra u oculta campos según el rol elegido. */
    private void actualizarCampos() {
        boolean alumno = cmbRol.getSelectedIndex() == 0;
        mostrar(lblMatricula, txtMatricula, alumno);
        mostrar(lblCarrera, txtCarrera, alumno);
        mostrar(lblPrograma, cmbPrograma, alumno);
        mostrar(lblCodigo, txtCodigo, !alumno && CODIGO_DOCENTE != null);
        pack();
    }

    private void mostrar(JLabel l, JComponent c, boolean visible) {
        l.setVisible(visible);
        c.setVisible(visible);
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