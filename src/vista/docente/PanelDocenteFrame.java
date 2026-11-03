package vista.docente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import modelo.Usuario;
import servicio.PanelDocenteService;
import util.Sesion;
import vista.comun.Estilo;

/**
 * Ventana principal del docente y del responsable de área (parte 5).
 * Pestañas: Registros de horas · Alumnos y constancias · Instituciones por aprobar.
 *
 * Desde el inicio de sesión se abre así, cuando el usuario es docente o responsable:
 *     new PanelDocenteFrame().setVisible(true);      // usa Sesion.getUsuario()
 */
public class PanelDocenteFrame extends JFrame {

    /** Abre el panel para el usuario que inició sesión (util.Sesion). */
    public PanelDocenteFrame() {
        this(Sesion.getUsuario());
    }

    public PanelDocenteFrame(Usuario usuario) {
        super("Panel del docente · Servicio Social y Prácticas");
        if (usuario == null || !(usuario.esDocente() || usuario.esResponsable())) {
            throw new IllegalStateException("El panel es solo para docentes y responsables de área.");
        }
        PanelDocenteService servicio = new PanelDocenteService(usuario);

        RegistrosPanel registros = new RegistrosPanel(servicio);
        AlumnosPanel alumnos = new AlumnosPanel(servicio);
        InstitucionesPanel instituciones = new InstitucionesPanel(servicio);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Registros de horas", registros);
        pestanas.addTab("Alumnos y constancias", alumnos);
        pestanas.addTab("Instituciones por aprobar", instituciones);

        // Cada vez que se cambia de pestaña, se vuelve a leer la base
        pestanas.addChangeListener(e -> {
            Object actual = pestanas.getSelectedComponent();
            if (actual == registros) {
                registros.recargar();
            } else if (actual == alumnos) {
                alumnos.recargar();
            } else if (actual == instituciones) {
                instituciones.recargar();
            }
        });

        setLayout(new BorderLayout());
        add(crearEncabezado(usuario), BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);

        registros.recargar();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1240, 780);
        setLocationRelativeTo(null);
    }

    private JPanel crearEncabezado(Usuario usuario) {
        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.setBackground(Color.WHITE);
        encabezado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BORDE),
                BorderFactory.createEmptyBorder(16, 20, 14, 20)));
        encabezado.add(Estilo.titulo(usuario.esResponsable() ? "Panel del responsable de área" : "Panel del docente"),
                BorderLayout.NORTH);
        String rol = usuario.esResponsable() ? "Responsable de área" : "Docente";
        encabezado.add(Estilo.secundario("Sesión: " + usuario.getNombre() + "  ·  " + rol), BorderLayout.SOUTH);
        return encabezado;
    }
}
