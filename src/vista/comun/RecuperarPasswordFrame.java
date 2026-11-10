package vista.comun;

import dao.RecuperacionDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import util.CorreoUtil;

public class RecuperarPasswordFrame extends JFrame {

    private final JTextField txtCorreo = new JTextField(22);
    private final JButton btnEnviar = new JButton("Enviar código");
    private final JTextField txtCodigo = new JTextField(22);
    private final JPasswordField txtNueva = new JPasswordField(22);
    private final JPasswordField txtConfirmar = new JPasswordField(22);
    private final JButton btnCambiar = new JButton("Cambiar contraseña");
    private final JButton btnVolver = new JButton("Volver al inicio de sesión");
    private final RecuperacionDAO dao = new RecuperacionDAO();

    public RecuperarPasswordFrame() {
        setTitle("Seguimiento de Servicio Social y Prácticas - Recuperar contraseña");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 32, 20, 32));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;

        JLabel titulo = new JLabel("Recuperar contraseña", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        g.gridy = 0; p.add(titulo, g);

        g.gridy = 1; p.add(new JLabel("1. Escribe tu correo y te enviaremos un código"), g);
        g.gridy = 2; p.add(txtCorreo, g);
        g.gridy = 3; p.add(btnEnviar, g);

        g.gridy = 4; g.insets = new Insets(18, 4, 5, 4);
        p.add(new JLabel("2. Escribe el código y tu nueva contraseña"), g);
        g.insets = new Insets(5, 4, 5, 4);
        g.gridy = 5; p.add(etiquetado("Código recibido", txtCodigo), g);
        g.gridy = 6; p.add(etiquetado("Nueva contraseña (mín. 8)", txtNueva), g);
        g.gridy = 7; p.add(etiquetado("Confirmar contraseña", txtConfirmar), g);
        g.gridy = 8; p.add(btnCambiar, g);
        g.gridy = 9; p.add(btnVolver, g);

        // El paso 2 se habilita después de enviar el código
        setPaso2(false);

        btnEnviar.addActionListener(e -> enviarCodigo());
        btnCambiar.addActionListener(e -> cambiarPassword());
        btnVolver.addActionListener(e -> volverAlLogin());

        setContentPane(p);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel etiquetado(String texto, JComponent campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 3));
        panel.add(new JLabel(texto), BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return panel;
    }

    private void setPaso2(boolean activo) {
        txtCodigo.setEnabled(activo);
        txtNueva.setEnabled(activo);
        txtConfirmar.setEnabled(activo);
        btnCambiar.setEnabled(activo);
    }

    private void enviarCodigo() {
        final String correo = txtCorreo.getText().trim().toLowerCase();
        if (correo.isEmpty()) {
            aviso("Escribe tu correo.");
            return;
        }
        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        // El envío de correo tarda unos segundos: se hace fuera del hilo de la ventana
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String codigo = dao.generarCodigo(correo);
                if (codigo == null) {
                    return "";                      // no existe la cuenta
                }
                if (CorreoUtil.configurado()) {
                    CorreoUtil.enviar(correo, "Código para recuperar tu contraseña",
                            "Tu código de recuperación es: " + codigo
                          + "\n\nEs válido por 15 minutos y solo se puede usar una vez."
                          + "\nSi no lo solicitaste, ignora este mensaje.");
                    return null;                    // enviado por correo
                }
                return codigo;                      // correo no configurado: modo de prueba
            }

            @Override
            protected void done() {
                btnEnviar.setEnabled(true);
                btnEnviar.setText("Enviar código");
                try {
                    String r = get();
                    if (r != null && !r.isEmpty()) {
                        JOptionPane.showMessageDialog(RecuperarPasswordFrame.this,
                                "MODO DE PRUEBA: el correo no está configurado.\n"
                              + "Tu código es: " + r,
                                "Código de recuperación", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // Mismo mensaje exista o no la cuenta, para no revelar qué correos están registrados
                        JOptionPane.showMessageDialog(RecuperarPasswordFrame.this,
                                "Si el correo está registrado, te enviamos un código.\n"
                              + "Revisa tu bandeja (y spam). Es válido por 15 minutos.",
                                "Código enviado", JOptionPane.INFORMATION_MESSAGE);
                    }
                    setPaso2(true);
                    txtCodigo.requestFocus();
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(RecuperarPasswordFrame.this,
                            "No se pudo enviar el código.\n" + causa.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void cambiarPassword() {
        String correo = txtCorreo.getText().trim().toLowerCase();
        String codigo = txtCodigo.getText().trim();
        String nueva = new String(txtNueva.getPassword());
        String confirmar = new String(txtConfirmar.getPassword());

        if (codigo.isEmpty() || nueva.isEmpty()) {
            aviso("Escribe el código y la nueva contraseña.");
            return;
        }
        if (nueva.length() < 8) {
            aviso("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        if (!nueva.equals(confirmar)) {
            aviso("Las contraseñas no coinciden.");
            return;
        }
        try {
            if (dao.restablecer(correo, codigo, nueva)) {
                JOptionPane.showMessageDialog(this,
                        "Contraseña actualizada. Ya puedes iniciar sesión.",
                        "Listo", JOptionPane.INFORMATION_MESSAGE);
                volverAlLogin();
            } else {
                aviso("El código es incorrecto o ya venció. Solicita uno nuevo.");
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