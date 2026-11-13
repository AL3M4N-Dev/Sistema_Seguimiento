package vista.alumno;

import dao.MenuAlumnoDAO;
import dao.ProgresoAlumnoDAO;
import dao.RegistroHorasDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.EstadoRegistro;
import modelo.ProgresoAlumno;
import modelo.RegistroHoras;
import modelo.Usuario;
import util.Sesion;
import vista.comun.Dialogos;
import vista.comun.EncabezadoApp;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.Tarjeta;
import vista.comun.LoginFrame;

/**
 * Parte 3: Menú principal del alumno.
 *
 * Izquierda: ConstanciaAlumnoPanel (parte 5): institución, barra de horas aprobadas,
 * horas pendientes aparte, Solicitar constancia y Descargar constancia en PDF.
 * Derecha: tabla con todos los registros del alumno (estado por color) y el botón
 * "Añadir horas".
 *
 * Usa Sesion.getUsuario(), así que primero hay que iniciar sesión:
 *     new MenuPrincipalFrame().setVisible(true);
 *
 * Para conectar con el Integrante 4 (registro de horas):
 *     menu.setAccionAnadirHoras(() -> new RegistroHorasDialog(menu).mostrar());
 *     menu.setAccionCorregirRegistro(idRegistro -> ...abrir el registro para corregirlo...);
 * Después de que esas ventanas se cierran, el menú se recarga solo.
 */
public class MenuPrincipalFrame extends JFrame {

    private final Usuario alumno;
    private final ProgresoAlumnoDAO progresos = new ProgresoAlumnoDAO();
    private final RegistroHorasDAO registros = new RegistroHorasDAO();
    private final MenuAlumnoDAO menuDAO = new MenuAlumnoDAO();

    private final ConstanciaAlumnoPanel constancia;
    private final ListaTableModel<RegistroHoras> modelo = new ListaTableModel<>(
            new String[]{"Fecha", "Horario", "Horas", "Estado"}, MenuPrincipalFrame::valorCelda);
    private final JTable tabla = new JTable(modelo);
    private final JButton btnAnadir = Estilo.botonPrincipal("Añadir horas");
    private final Tarjeta cajaAviso = new Tarjeta(new BorderLayout(), Estilo.PENDIENTE_FONDO, null, 10);
    private final JLabel textoAviso = Estilo.etiqueta(" ", Estilo.fuente(Font.BOLD, 13), Estilo.PENDIENTE_TEXTO);

    private Runnable accionAnadirHoras;
    private IntConsumer accionCorregirRegistro;

    public MenuPrincipalFrame() {
        alumno = Sesion.getUsuario();
        if (alumno == null || !alumno.esAlumno()) {
            throw new IllegalStateException("El menú principal es solo para alumnos con sesión iniciada.");
        }
        constancia = new ConstanciaAlumnoPanel();

        setTitle("Menú principal · Seguimiento de Servicio Social y Prácticas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Barra de arriba del diseño base
        EncabezadoApp encabezado = new EncabezadoApp(alumno.getNombre() + " · Alumno");
        encabezado.agregarSeccion("menu", "Menú principal");
        encabezado.seleccionar("menu");
        encabezado.alCerrarSesion(() -> {
            Sesion.cerrar();
            new LoginFrame().setVisible(true);
            dispose();
        });

        // Tarjeta de registros
        cajaAviso.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        cajaAviso.add(textoAviso, BorderLayout.CENTER);
        cajaAviso.setVisible(false);

        JPanel fila = Estilo.panel(new BorderLayout());
        fila.add(Estilo.subtitulo("Mis registros de horas"), BorderLayout.WEST);
        fila.add(btnAnadir, BorderLayout.EAST);

        JPanel debajo = Estilo.panel(new BorderLayout(0, 8));
        debajo.add(cajaAviso, BorderLayout.NORTH);
        debajo.add(Estilo.secundario("Haz doble clic en un registro rechazado para ver el comentario de tu docente."),
                BorderLayout.SOUTH);

        JPanel cabecera = Estilo.panel(new BorderLayout(0, 10));
        cabecera.add(fila, BorderLayout.NORTH);
        cabecera.add(debajo, BorderLayout.CENTER);

        Estilo.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new EstadoRenderer());

        Tarjeta tarjetaRegistros = new Tarjeta();
        tarjetaRegistros.add(cabecera, BorderLayout.NORTH);
        tarjetaRegistros.add(Estilo.scroll(tabla), BorderLayout.CENTER);

        // Pantalla
        JPanel contenido = new JPanel(new BorderLayout(16, 0));
        contenido.setBackground(Estilo.FONDO);
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));
        contenido.add(constancia, BorderLayout.WEST);
        contenido.add(tarjetaRegistros, BorderLayout.CENTER);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.FONDO);
        raiz.add(encabezado, BorderLayout.NORTH);
        raiz.add(contenido, BorderLayout.CENTER);
        setContentPane(raiz);

        // Acciones
        btnAnadir.addActionListener(e -> anadirHoras());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirRegistroSeleccionado();
                }
            }
        });

        setSize(1200, 720);
        setLocationRelativeTo(null);
        Estilo.pantallaCompleta(this);
        recargar();
    }

    /** Qué pasa al pulsar "Añadir horas" (formulario del Integrante 4). */
    public void setAccionAnadirHoras(Runnable accion) {
        this.accionAnadirHoras = accion;
    }

    /** Qué pasa al corregir un registro rechazado; recibe el id_registro. */
    public void setAccionCorregirRegistro(IntConsumer accion) {
        this.accionCorregirRegistro = accion;
    }

    /** Vuelve a leer todo desde la base. Llámalo cuando se cierre otra pantalla. */
    public final void recargar() {
        constancia.recargar();
        try {
            ProgresoAlumno p = progresos.buscarPorAlumno(alumno.getIdUsuario());
            if (p == null) {
                modelo.setFilas(List.of());
                btnAnadir.setEnabled(false);
                mostrarAviso("Aún no estás inscrito a una institución.", Estilo.NEUTRO_FONDO, Estilo.NEUTRO_TEXTO);
                return;
            }
            modelo.setFilas(registros.listarPorInscripcion(p.getIdInscripcion(), null, 0));
            aplicarEstadoInstitucion(menuDAO.estadoInstitucion(p.getIdInscripcion()));
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    // Con la institución "en revisión" o "rechazada" el alumno no puede registrar horas.
    // ("lleno" sí deja registrar: significa que ya no hay cupos, no que la suya esté cerrada).
    private void aplicarEstadoInstitucion(String estado) {
        if ("en_revision".equals(estado)) {
            btnAnadir.setEnabled(false);
            mostrarAviso("Tu institución está en revisión. Podrás registrar horas cuando un docente la apruebe.",
                    Estilo.PENDIENTE_FONDO, Estilo.PENDIENTE_TEXTO);
        } else if ("rechazada".equals(estado)) {
            btnAnadir.setEnabled(false);
            mostrarAviso("Tu institución fue rechazada. Registra otra para poder añadir horas.",
                    Estilo.RECHAZADO_FONDO, Estilo.RECHAZADO_TEXTO);
        } else {
            btnAnadir.setEnabled(true);
            cajaAviso.setVisible(false);
        }
    }

    private void mostrarAviso(String texto, Color fondo, Color letra) {
        textoAviso.setText("<html><body style='width: 460px'>" + texto + "</body></html>");
        textoAviso.setForeground(letra);
        cajaAviso.setColores(fondo, null);
        cajaAviso.setVisible(true);
    }

    private void anadirHoras() {
        if (accionAnadirHoras == null) {
            Dialogos.info(this, "Próximamente",
                    "El formulario para registrar horas lo conecta el integrante 4.");
            return;
        }
        accionAnadirHoras.run();
        recargar();
    }

    private void abrirRegistroSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return;
        }
        RegistroHoras r = modelo.getFila(fila);
        if (r.getEstado() != EstadoRegistro.RECHAZADO) {
            return;
        }
        String comentario = r.getComentarioDocente() == null || r.getComentarioDocente().isBlank()
                ? "Tu docente no escribió un comentario."
                : r.getComentarioDocente();
        String mensaje = "Turno del " + Formato.fecha(r.getFecha()) + " (" + Formato.horario(r.getHoraInicio(), r.getHoraFin())
                + "). Esto escribió tu docente:";
        if (accionCorregirRegistro == null) {
            Dialogos.aviso(this, "Registro rechazado", mensaje, comentario);
        } else if (Dialogos.confirmarAprobar(this, "Registro rechazado", mensaje, comentario, "Corregir registro")) {
            accionCorregirRegistro.accept(r.getIdRegistro());
            recargar();
        }
    }

    private static Object valorCelda(RegistroHoras r, int columna) {
        return switch (columna) {
            case 0 -> Formato.fecha(r.getFecha());
            case 1 -> Formato.horario(r.getHoraInicio(), r.getHoraFin());
            case 2 -> Formato.horas(r.getHorasCalculadas()) + " h";
            default -> r.getEstado().toString();
        };
    }
}