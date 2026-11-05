package vista.docente;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import modelo.Usuario;
import servicio.PanelDocenteService;
import util.Sesion;
import vista.comun.EncabezadoApp;
import vista.comun.Estilo;

/**
 * Ventana principal del docente y del responsable de área (parte 5), con el diseño base:
 * encabezado con pestañas (Registros · Alumnos y constancias · Historial de constancias · Instituciones).
 *
 * Desde el inicio de sesión se abre así, cuando el usuario es docente o responsable:
 *     new PanelDocenteFrame().setVisible(true);      // usa Sesion.getUsuario()
 *
 * Para que "Cerrar sesión" regrese a la pantalla de login:
 *     panel.setAccionCerrarSesion(() -> new LoginFrame().setVisible(true));
 */
public class PanelDocenteFrame extends JFrame {

    private static final String REGISTROS = "registros";
    private static final String ALUMNOS = "alumnos";
    private static final String HISTORIAL = "historial";
    private static final String INSTITUCIONES = "instituciones";

    private final PanelDocenteService servicio;
    private final EncabezadoApp encabezado;
    private Runnable despuesDeCerrarSesion = () -> { };

    /** Abre el panel para el usuario que inició sesión (util.Sesion). */
    public PanelDocenteFrame() {
        this(Sesion.getUsuario());
    }

    public PanelDocenteFrame(Usuario usuario) {
        super("Seguimiento · Panel del docente");
        if (usuario == null || !(usuario.esDocente() || usuario.esResponsable())) {
            throw new IllegalStateException("El panel es solo para docentes y responsables de área.");
        }
        servicio = new PanelDocenteService(usuario);

        RegistrosPanel registros = new RegistrosPanel(servicio);
        AlumnosPanel alumnos = new AlumnosPanel(servicio);
        HistorialConstanciasPanel historial = new HistorialConstanciasPanel(servicio);
        InstitucionesPanel instituciones = new InstitucionesPanel(servicio);

        CardLayout tarjetas = new CardLayout();
        JPanel contenido = new JPanel(tarjetas);
        contenido.setBackground(Estilo.FONDO);
        contenido.add(registros, REGISTROS);
        contenido.add(alumnos, ALUMNOS);
        contenido.add(historial, HISTORIAL);
        contenido.add(instituciones, INSTITUCIONES);

        String rol = usuario.esResponsable() ? "Responsable de área" : "Docente";
        encabezado = new EncabezadoApp(usuario.getNombre() + " · " + rol);
        encabezado.agregarSeccion(REGISTROS, "Registros");
        encabezado.agregarSeccion(ALUMNOS, "Alumnos y constancias");
        encabezado.agregarSeccion(HISTORIAL, "Historial de constancias");
        encabezado.agregarSeccion(INSTITUCIONES, "Instituciones");
        // Al cambiar de sección se vuelve a leer la base y se actualizan los contadores
        encabezado.alCambiarSeccion(clave -> {
            tarjetas.show(contenido, clave);
            switch (clave) {
                case REGISTROS -> registros.recargar();
                case ALUMNOS -> alumnos.recargar();
                case HISTORIAL -> historial.recargar();
                default -> instituciones.recargar();
            }
            actualizarContadores();
        });
        encabezado.alCerrarSesion(this::cerrarSesion);

        setLayout(new BorderLayout());
        add(encabezado, BorderLayout.NORTH);
        add(contenido, BorderLayout.CENTER);
        getContentPane().setBackground(Estilo.FONDO);

        encabezado.seleccionar(REGISTROS);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 700));
        setSize(1280, 800);                 // tamaño al "restaurar" la ventana
        setLocationRelativeTo(null);
        // Se abre en pantalla completa (maximizada), con la barra de Windows para minimizar o cerrar
        Estilo.pantallaCompleta(this);
    }

    /** Qué hacer después de cerrar sesión (por ejemplo, abrir otra vez el login). */
    public void setAccionCerrarSesion(Runnable accion) {
        this.despuesDeCerrarSesion = accion;
    }

    private void cerrarSesion() {
        Sesion.cerrar();
        dispose();
        despuesDeCerrarSesion.run();
    }

    /** Números junto a las pestañas: registros pendientes e instituciones por revisar. */
    private void actualizarContadores() {
        try {
            encabezado.setContador(REGISTROS, servicio.contarRegistrosPendientes());
            encabezado.setContador(INSTITUCIONES, servicio.contarInstitucionesEnRevision());
        } catch (SQLException e) {
            // Si falla, la pestaña ya mostró el error; los contadores se quedan como estaban
        }
    }
}
