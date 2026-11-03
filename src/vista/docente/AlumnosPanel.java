package vista.docente;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableRowSorter;
import modelo.AlumnoActivo;
import modelo.Programa;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Avance;
import vista.comun.AvanceRenderer;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;

/**
 * Pestaña "Alumnos y constancias": lista de alumnos activos de servicio social y de
 * prácticas con su avance de horas. Al seleccionar uno se ve su ficha; si ya completó
 * sus horas se habilita "Subir PDF y aprobar" para entregarle su constancia.
 */
public class AlumnosPanel extends JPanel {

    private static final String[] PROGRAMAS = {"Todos los programas", "Servicio social", "Prácticas"};
    private static final String[] MOSTRAR = {"Todos", "Les faltan horas", "Listos para subir constancia",
        "Con constancia entregada", "Con solicitud en espera"};
    private static final int COL_AVANCE = 5;
    private static final int COL_CONSTANCIA = 7;

    private final PanelDocenteService servicio;
    private final ListaTableModel<AlumnoActivo> modelo = new ListaTableModel<>(
            new String[]{"Alumno", "Matrícula", "Carrera", "Programa", "Institución",
                "Horas aprobadas", "En revisión", "Constancia"},
            (a, col) -> switch (col) {
                case 0 -> a.getNombre();
                case 1 -> a.getMatricula();
                case 2 -> a.getCarrera();
                case 3 -> Programa.texto(a.getTipoPrograma());
                case 4 -> a.getInstitucion();
                case 5 -> new Avance(a.getHorasAprobadas(), a.getHorasRequeridas());
                case 6 -> Formato.horas(a.getHorasPendientes()) + " h";
                default -> a.textoConstancia();
            });
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ListaTableModel<AlumnoActivo>> orden = new TableRowSorter<>(modelo);
    private final JComboBox<String> filtroPrograma = new JComboBox<>(PROGRAMAS);
    private final JComboBox<String> filtroMostrar = new JComboBox<>(MOSTRAR);
    private final JTextField buscar = new JTextField(16);
    private final JLabel resumen = Estilo.secundario(" ");

    // Ficha del alumno seleccionado
    private final JLabel fichaNombre = new JLabel("Selecciona un alumno de la lista");
    private final JLabel fichaDatos = Estilo.secundario(" ");
    private final JLabel fichaInscripcion = Estilo.secundario(" ");
    private final JLabel fichaHoras = new JLabel(" ");
    private final JLabel fichaConstancia = new JLabel(" ");
    private final JButton abrirPdf = new JButton("Abrir PDF");
    private final JButton rechazar = Estilo.botonContorno("Rechazar solicitud", Estilo.ROJO);
    private final JButton subir = Estilo.botonPrincipal("Subir PDF y aprobar", Estilo.VERDE);

    private List<AlumnoActivo> todos = new ArrayList<>();

    public AlumnosPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(crearArriba(), BorderLayout.NORTH);
        add(crearTabla(), BorderLayout.CENTER);
        add(crearFicha(), BorderLayout.SOUTH);

        filtroPrograma.addActionListener(e -> aplicarFiltros());
        filtroMostrar.addActionListener(e -> aplicarFiltros());
        buscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                aplicarFiltros();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                aplicarFiltros();
            }
        });
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarFicha();
            }
        });
        subir.addActionListener(e -> subirPdfYAprobar());
        abrirPdf.addActionListener(e -> abrirPdf());
        rechazar.addActionListener(e -> rechazarSolicitud());
        mostrarFicha();
    }

    // ------------------------------------------------------------------
    // Armado de la pantalla
    // ------------------------------------------------------------------

    private JPanel crearArriba() {
        String alcance = servicio.getUsuario().esResponsable()
                ? "Todos los alumnos inscritos, de servicio social y de prácticas."
                : "Tus alumnos asignados (y los que aún no tienen docente), de servicio social y de prácticas.";
        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.add(Estilo.subtitulo("Alumnos y constancias"), BorderLayout.NORTH);
        textos.add(Estilo.secundario(alcance + " Cuando un alumno completa sus horas aprobadas, "
                + "se habilita \"Subir PDF y aprobar\"."), BorderLayout.SOUTH);

        JButton actualizar = new JButton("Actualizar");
        actualizar.addActionListener(e -> recargar());
        buscar.setToolTipText("Escribe parte del nombre o de la matrícula");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Programa:"));
        filtros.add(filtroPrograma);
        filtros.add(new JLabel("Mostrar:"));
        filtros.add(filtroMostrar);
        filtros.add(new JLabel("Buscar:"));
        filtros.add(buscar);
        filtros.add(actualizar);

        JPanel arriba = new JPanel(new BorderLayout(0, 12));
        arriba.add(textos, BorderLayout.NORTH);
        arriba.add(filtros, BorderLayout.SOUTH);
        return arriba;
    }

    private JPanel crearTabla() {
        tabla.setRowHeight(32);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowSorter(orden);
        orden.setComparator(COL_AVANCE, (x, y) -> ((Avance) x).compareTo((Avance) y));
        tabla.getColumnModel().getColumn(COL_AVANCE).setCellRenderer(new AvanceRenderer());
        tabla.getColumnModel().getColumn(COL_CONSTANCIA).setCellRenderer(new EstadoRenderer());
        int[] anchos = {160, 80, 140, 105, 180, 190, 80, 120};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        JPanel centro = new JPanel(new BorderLayout(0, 6));
        centro.add(new JScrollPane(tabla), BorderLayout.CENTER);
        centro.add(resumen, BorderLayout.SOUTH);
        return centro;
    }

    private JPanel crearFicha() {
        fichaNombre.setFont(fichaNombre.getFont().deriveFont(Font.BOLD, 16f));
        fichaConstancia.setFont(fichaConstancia.getFont().deriveFont(Font.BOLD));
        JPanel datos = new JPanel(new GridLayout(0, 1, 0, 4));
        datos.setOpaque(false);
        datos.add(fichaNombre);
        datos.add(fichaDatos);
        datos.add(fichaInscripcion);
        datos.add(fichaHoras);
        datos.add(fichaConstancia);

        subir.setPreferredSize(new Dimension(200, 36));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botones.setOpaque(false);
        botones.add(abrirPdf);
        botones.add(rechazar);
        botones.add(subir);

        JPanel ficha = new JPanel(new BorderLayout(16, 0));
        ficha.setBackground(Estilo.FONDO);
        ficha.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Estilo.BORDE),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        ficha.add(datos, BorderLayout.CENTER);
        ficha.add(botones, BorderLayout.EAST);
        return ficha;
    }

    // ------------------------------------------------------------------
    // Datos y filtros
    // ------------------------------------------------------------------

    public final void recargar() {
        AlumnoActivo antes = seleccionado();
        try {
            todos = servicio.listarAlumnos();
            modelo.setFilas(todos);
            aplicarFiltros();
            if (antes != null) {
                seleccionarInscripcion(antes.getIdInscripcion());
            }
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        mostrarFicha();
    }

    private void aplicarFiltros() {
        int programa = filtroPrograma.getSelectedIndex();
        int mostrar = filtroMostrar.getSelectedIndex();
        String texto = buscar.getText().trim().toLowerCase();
        orden.setRowFilter(new RowFilter<ListaTableModel<AlumnoActivo>, Integer>() {
            @Override
            public boolean include(Entry<? extends ListaTableModel<AlumnoActivo>, ? extends Integer> fila) {
                AlumnoActivo a = modelo.getFila(fila.getIdentifier());
                boolean okPrograma = programa == 0
                        || (programa == 1 && "servicio_social".equals(a.getTipoPrograma()))
                        || (programa == 2 && "practicas".equals(a.getTipoPrograma()));
                boolean okMostrar = switch (mostrar) {
                    case 1 -> !a.horasCompletas();
                    case 2 -> a.horasCompletas() && !a.tieneConstancia();
                    case 3 -> a.tieneConstancia();
                    case 4 -> a.solicitudEnEspera();
                    default -> true;
                };
                boolean okTexto = texto.isEmpty()
                        || a.getNombre().toLowerCase().contains(texto)
                        || (a.getMatricula() != null && a.getMatricula().toLowerCase().contains(texto));
                return okPrograma && okMostrar && okTexto;
            }
        });
        actualizarResumen();
    }

    private void actualizarResumen() {
        long listos = todos.stream().filter(a -> a.horasCompletas() && !a.tieneConstancia()).count();
        long entregadas = todos.stream().filter(AlumnoActivo::tieneConstancia).count();
        long enEspera = todos.stream().filter(AlumnoActivo::solicitudEnEspera).count();
        resumen.setText("Mostrando " + tabla.getRowCount() + " de " + todos.size() + " alumno(s)  ·  "
                + listos + " listo(s) para subir constancia  ·  " + enEspera + " solicitud(es) en espera  ·  "
                + entregadas + " constancia(s) entregada(s)");
    }

    private AlumnoActivo seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : modelo.getFila(tabla.convertRowIndexToModel(fila));
    }

    private void seleccionarInscripcion(int idInscripcion) {
        for (int i = 0; i < tabla.getRowCount(); i++) {
            if (modelo.getFila(tabla.convertRowIndexToModel(i)).getIdInscripcion() == idInscripcion) {
                tabla.setRowSelectionInterval(i, i);
                tabla.scrollRectToVisible(tabla.getCellRect(i, 0, true));
                return;
            }
        }
    }

    /** Llena la ficha del alumno seleccionado y habilita los botones que aplican. */
    private void mostrarFicha() {
        AlumnoActivo a = seleccionado();
        if (a == null) {
            fichaNombre.setText("Selecciona un alumno de la lista");
            fichaDatos.setText("Aquí verás sus datos, su avance y su constancia.");
            fichaInscripcion.setText(" ");
            fichaHoras.setText(" ");
            fichaConstancia.setText(" ");
            subir.setText("Subir PDF y aprobar");
            subir.setEnabled(false);
            abrirPdf.setEnabled(false);
            rechazar.setEnabled(false);
            return;
        }
        fichaNombre.setText(a.getNombre());
        fichaDatos.setText("Matrícula: " + vacio(a.getMatricula()) + "   ·   Carrera: " + vacio(a.getCarrera())
                + "   ·   Correo: " + vacio(a.getCorreo()));
        fichaInscripcion.setText(Programa.texto(a.getTipoPrograma()) + " en " + a.getInstitucion()
                + "   ·   Docente asignado: " + (a.getDocenteAsignado() == null ? "sin asignar" : a.getDocenteAsignado()));
        fichaHoras.setText("Horas aprobadas: " + Formato.horas(a.getHorasAprobadas()) + " de " + a.getHorasRequeridas()
                + " (" + Formato.porcentaje(a.getHorasAprobadas(), a.getHorasRequeridas()) + "%)"
                + (a.horasCompletas() ? "   ·   ¡Horas completas!" : "   ·   Le faltan " + Formato.horas(a.horasFaltantes()) + " h")
                + "   ·   En revisión: " + Formato.horas(a.getHorasPendientes()) + " h");
        fichaConstancia.setText("Constancia: " + descripcionConstancia(a));

        subir.setEnabled(a.horasCompletas());
        subir.setText(a.tieneConstancia() ? "Reemplazar PDF" : "Subir PDF y aprobar");
        subir.setToolTipText(a.horasCompletas() ? null
                : "Se habilita cuando el alumno completa sus horas aprobadas.");
        abrirPdf.setEnabled(a.tieneConstancia());
        rechazar.setEnabled(a.solicitudEnEspera());
    }

    private static String descripcionConstancia(AlumnoActivo a) {
        if (a.getEstadoConstancia() == null) {
            return a.horasCompletas() ? "lista para subir (el alumno no la ha pedido)" : "todavía no";
        }
        return switch (a.getEstadoConstancia()) {
            case SOLICITADA, VALIDADA -> "el alumno la solicitó el " + Formato.fechaHora(a.getFechaSolicitud());
            case DISPONIBLE -> "entregada el " + Formato.fechaHora(a.getFechaAprobacion());
            case RECHAZADA -> "su solicitud fue rechazada";
        };
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void subirPdfYAprobar() {
        AlumnoActivo a = seleccionado();
        if (a == null) {
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("PDF de la constancia de " + Programa.texto(a.getTipoPrograma()).toLowerCase()
                + " de " + a.getNombre());
        selector.setFileFilter(new FileNameExtensionFilter("Archivos PDF", "pdf"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        String pregunta = a.tieneConstancia()
                ? a.getNombre() + " ya tiene su constancia entregada.\n¿Reemplazarla por \"" + archivo.getName() + "\"?"
                : "¿Subir \"" + archivo.getName() + "\" como constancia de " + Programa.texto(a.getTipoPrograma()).toLowerCase()
                  + " de " + a.getNombre() + " y aprobarla?\nEl alumno podrá descargarla desde su menú.";
        if (!Dialogos.confirmar(this, pregunta)) {
            return;
        }
        try {
            servicio.subirPdfYAprobar(a.getIdInscripcion(), archivo);
            Dialogos.info(this, "Constancia aprobada. " + a.getNombre() + " ya puede descargarla.");
        } catch (ReglaNegocioException | SQLException | IOException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void abrirPdf() {
        AlumnoActivo a = seleccionado();
        if (a == null) {
            return;
        }
        File pdf = PanelDocenteService.archivoDe(a.getRutaPdf());
        if (pdf == null || !pdf.isFile()) {
            Dialogos.info(this, "No se encontró el PDF en esta computadora:\n"
                    + (pdf == null ? "(sin ruta)" : pdf.getAbsolutePath()));
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf);
            } else {
                Dialogos.info(this, "El PDF está en:\n" + pdf.getAbsolutePath());
            }
        } catch (IOException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void rechazarSolicitud() {
        AlumnoActivo a = seleccionado();
        if (a == null || a.getIdSolicitud() == null) {
            return;
        }
        String comentario = Dialogos.pedirComentario(this, "Rechazar solicitud de constancia",
                "¿Por qué se rechaza la solicitud de " + a.getNombre() + "? El alumno leerá este comentario.");
        if (comentario == null) {
            return;
        }
        try {
            servicio.rechazarSolicitud(a.getIdSolicitud(), comentario);
            Dialogos.info(this, "Solicitud rechazada.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }
}
