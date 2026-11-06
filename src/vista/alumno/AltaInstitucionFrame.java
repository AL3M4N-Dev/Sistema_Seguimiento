package vista.alumno;

import dao.CatalogoDAO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import util.Sesion;
import vista.comun.Boton;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Tarjeta;

public class AltaInstitucionFrame extends JFrame {

    private final CatalogoDAO catalogoDAO = new CatalogoDAO();
    private final JTextField txtNombre = campo();
    private final JTextField txtDireccion = campo();
    private final JTextField txtContacto = campo();
    private final JTextField txtHoras = campo();
    private final Boton btnGuardar = Estilo.botonPrincipal("Enviar a revisión");

    public AltaInstitucionFrame() {
        setTitle("Alta de institución de prácticas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(Estilo.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel encabezado = Estilo.panel(new BorderLayout(0, 4));
        encabezado.add(Estilo.titulo("Registra tu institución de prácticas"), BorderLayout.NORTH);
        encabezado.add(Estilo.secundario(
                "Un docente la revisará. Mientras esté en revisión no podrás registrar horas."),
                BorderLayout.SOUTH);
        raiz.add(encabezado, BorderLayout.NORTH);

        Tarjeta tarjeta = new Tarjeta(new GridBagLayout(), Estilo.SUPERFICIE, Estilo.BORDE, 14);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 0, 4, 0);

        agregar(tarjeta, g, 0, "Nombre de la institución", txtNombre);
        agregar(tarjeta, g, 2, "Dirección", txtDireccion);
        agregar(tarjeta, g, 4, "Contacto (nombre, teléfono o correo)", txtContacto);
        agregar(tarjeta, g, 6, "Horas requeridas", txtHoras);
        raiz.add(tarjeta, BorderLayout.CENTER);

        JPanel botones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.add(btnGuardar);
        raiz.add(botones, BorderLayout.SOUTH);

        btnGuardar.addActionListener(e -> guardar());
        getRootPane().setDefaultButton(btnGuardar);

        setContentPane(raiz);
        setSize(620, 560);
        setMinimumSize(new Dimension(520, 520));
        setLocationRelativeTo(null);
    }

    // Un campo de texto con el borde y la letra del proyecto.
    private static JTextField campo() {
        JTextField t = new JTextField();
        t.setFont(Estilo.fuente(java.awt.Font.PLAIN, 14));
        t.setForeground(Estilo.TEXTO);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE_FUERTE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return t;
    }

    // Pone una etiqueta y su campo, uno debajo del otro.
    private static void agregar(JPanel panel, GridBagConstraints g, int fila,
            String texto, JTextField campo) {
        g.gridy = fila;
        panel.add(Estilo.secundario(texto), g);
        g.gridy = fila + 1;
        panel.add(campo, g);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String contacto = txtContacto.getText().trim();
        String textoHoras = txtHoras.getText().trim();

        if (nombre.isEmpty() || direccion.isEmpty() || contacto.isEmpty() || textoHoras.isEmpty()) {
            Dialogos.aviso(this, "Faltan datos", "Completa todos los campos del formulario.");
            return;
        }
        if (nombre.length() > 150 || contacto.length() > 150 || direccion.length() > 255) {
            Dialogos.aviso(this, "Texto demasiado largo",
                    "El nombre y el contacto admiten hasta 150 letras y la dirección hasta 255.");
            return;
        }
        int horas;
        try {
            horas = Integer.parseInt(textoHoras);
        } catch (NumberFormatException ex) {
            Dialogos.aviso(this, "Horas no válidas", "Escribe las horas con un número entero, por ejemplo 480.");
            txtHoras.requestFocusInWindow();
            return;
        }
        if (horas <= 0 || horas > 5000) {
            Dialogos.aviso(this, "Horas no válidas", "Las horas deben ser un número entre 1 y 5000.");
            txtHoras.requestFocusInWindow();
            return;
        }

        boolean acepta = Dialogos.confirmar(this, "¿Enviar tu institución a revisión?",
                "Se enviará \"" + nombre + "\" con " + horas + " horas. "
                + "No podrás cambiarla mientras un docente la revisa.",
                "Enviar a revisión");
        if (!acepta) {
            return;
        }
        try {
            catalogoDAO.crearInstitucionPracticas(
                    Sesion.getUsuario().getIdUsuario(), nombre, direccion, contacto, horas);
            Dialogos.exito(this, "Institución enviada",
                    "Tu institución quedó en revisión. Podrás registrar horas cuando un docente la apruebe.");
            // TODO (integrante 3): abrir aquí el menú principal del alumno
            dispose();
        } catch (IllegalStateException ex) {
            Dialogos.aviso(this, "No se pudo enviar", ex.getMessage());
        } catch (SQLException ex) {
            Dialogos.mostrarError(this, ex);
        }
    }
}