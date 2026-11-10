/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package vista.comun;

import dao.UsuarioDAO;
import java.awt.*;
import java.sql.SQLException;
import javax.swing.*;
import modelo.Usuario;
import util.Sesion;
import vista.docente.PanelDocenteFrame;
import vista.alumno.InstruccionesFrame;
/**
 *
 * @author luisa
 */
public class LoginFrame extends JFrame {
    private final JTextField txtCorreo = new JTextField(22);
    private final JPasswordField txtPassword = new JPasswordField(22);
    private final JButton btnEntrar = new JButton("Iniciar sesión");
    private final UsuarioDAO dao = new UsuarioDAO();
    
    public LoginFrame() {
        setTitle("Seguimiento de Servicio Social y Prácticas - Inicio de sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
 
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridwidth = 1;
 
        JLabel titulo = new JLabel("Iniciar sesión", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        g.gridy = 0; p.add(titulo, g);
 
        g.gridy = 1; p.add(new JLabel("Correo"), g);
        g.gridy = 2; p.add(txtCorreo, g);
        g.gridy = 3; p.add(new JLabel("Contraseña"), g);
        g.gridy = 4; p.add(txtPassword, g);
 
        g.gridy = 5; g.insets = new Insets(16, 4, 6, 4);
        p.add(btnEntrar, g);
 
        g.insets = new Insets(2, 4, 2, 4);
        JButton btnRegistro = enlace("¿No tienes cuenta? Regístrate");
        JButton btnOlvide = enlace("Olvidé mi contraseña");
        g.gridy = 6; p.add(btnRegistro, g);
        g.gridy = 7; p.add(btnOlvide, g);
 
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
        setContentPane(p);
        pack();
        setLocationRelativeTo(null);
    }
 
    private JButton enlace(String texto) {
        JButton b = new JButton(texto);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setForeground(new Color(25, 90, 180));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
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
 
    /** Devuelve true si abrió una ventana; false si la pantalla aún no existe. */
    private boolean abrirSegunRol(Usuario u) throws SQLException {
        if (u.esDocente() || u.esResponsable()) {
            // TODO (integrante 5): si el responsable tendrá su propio panel, abrirlo aquí
            new PanelDocenteFrame().setVisible(true);
            return true;
        }
 
        // Alumno
        if (!dao.tieneInscripcion(u.getIdUsuario())) {
            new InstruccionesFrame().setVisible(true);
            return true;
        } else {
            // TODO (integrante 3): abrir el menú principal del alumno
            aviso("Menú principal del alumno: pendiente (integrante 3).");
        }
        return false;
    }
 
    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Aviso", JOptionPane.WARNING_MESSAGE);
    }
    
}
