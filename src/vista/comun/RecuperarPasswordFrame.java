package vista.comun;

import dao.RecuperacionDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import util.CorreoUtil;

public class RecuperarPasswordFrame extends JFrame {

    private final JTextField txtCorreo = new JTextField();
    private final JButton btnEnviar = Tema.anchoCompleto(Estilo.botonPrincipal("Enviar código"));
    private final JTextField txtCodigo = new JTextField();
    private final JPasswordField txtNueva = new JPasswordField();
    private final JPasswordField txtConfirmar = new JPasswordField();
    private final JButton btnCambiar = Tema.anchoCompleto(Estilo.botonPrincipal("Cambiar contraseña"));
    private final JButton btnVolver = Tema.anchoCompleto(Estilo.botonSecundario("Volver al inicio de sesión"));
    private final RecuperacionDAO dao = new RecuperacionDAO();

    public RecuperarPasswordFrame() {
        setTitle("Recuperar contraseña · Seguimiento de Servicio Social y Prácticas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // ---------- Estilo de los componentes ----------
        Tema.campo(txtCorreo);
        Tema.campo(txtCodigo);
        Tema.campo(txtNueva);
        Tema.campo(txtConfirmar);

        // ---------- Tarjeta central ----------
        Tema.Tarjeta card = new Tema.Tarjeta(560);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(26, 36, 26, 36));

        card.add(Tema.titulo("Recuperar contraseña"));
        card.add(Box.createVerticalStrut(4));
        card.add(Tema.subtitulo("Te enviaremos un código a tu correo para crear una contraseña nueva"));
        card.add(Box.createVerticalStrut(18));

        card.add(Tema.seccion("1. Solicita tu código"));
        card.add(Box.createVerticalStrut(10));
        card.add(Tema.conEspacio(Tema.campoEtiquetado("Correo", txtCorreo), 10));
        card.add(btnEnviar);
        card.add(Box.createVerticalStrut(16));
        card.add(Tema.separador());
        card.add(Box.createVerticalStrut(16));

        card.add(Tema.seccion("2. Crea tu nueva contraseña"));
        card.add(Box.createVerticalStrut(10));
        card.add(Tema.conEspacio(Tema.campoEtiquetado("Código recibido", txtCodigo), 10));
        card.add(Tema.conEspacio(Tema.dos(
                Tema.campoEtiquetado("Nueva contraseña (mín. 8)", txtNueva),
                Tema.campoEtiquetado("Confirmar contraseña", txtConfirmar)), 6));
        card.add(VerPassword.crear(txtNueva, txtConfirmar));
        card.add(Box.createVerticalStrut(12));
        card.add(btnCambiar);
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

        // El paso 2 se habilita después de enviar el código
        setPaso2(false);

        // ---------- Eventos ----------
        btnEnviar.addActionListener(e -> enviarCodigo());
        btnCambiar.addActionListener(e -> cambiarPassword());
        btnVolver.addActionListener(e -> volverAlLogin());

        setSize(1000, 700);
        setMinimumSize(new Dimension(640, 680));
        setLocationRelativeTo(null);
        Estilo.pantallaCompleta(this);
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