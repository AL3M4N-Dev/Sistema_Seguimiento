package vista.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import modelo.EstadoSolicitud;
import modelo.Programa;
import modelo.SolicitudConstancia;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;

/**
 * Pestaña 3 (solo responsable de área): solicitudes de constancia.
 * Paso 1: validar horas (solicitada → validada). Paso 2: subir PDF (validada → disponible).
 */
public class ConstanciasPanel extends JPanel {

    private static final String[] FILTROS = {"Por revisar", "Validadas", "Disponibles", "Rechazadas", "Todas"};
    private static final EstadoSolicitud[] ESTADOS_FILTRO = {EstadoSolicitud.SOLICITADA, EstadoSolicitud.VALIDADA,
        EstadoSolicitud.DISPONIBLE, EstadoSolicitud.RECHAZADA, null};
    private static final int COL_COMPLETAS = 6;
    private static final int COL_ESTADO = 7;

    private final PanelDocenteService servicio;
    private final JComboBox<String> filtro = new JComboBox<>(FILTROS);
    private final ListaTableModel<SolicitudConstancia> modelo = new ListaTableModel<>(
            new String[]{"Alumno", "Matrícula", "Programa", "Institución", "Solicitada",
                "Horas aprobadas", "¿Horas completas?", "Estado"},
            (s, col) -> switch (col) {
                case 0 -> s.getAlumnoNombre();
                case 1 -> s.getMatricula();
                case 2 -> Programa.texto(s.getTipoPrograma());
                case 3 -> s.getInstitucionNombre();
                case 4 -> Formato.fechaHora(s.getFechaSolicitud());
                case 5 -> Formato.horas(s.getHorasAprobadas()) + " / " + s.getHorasRequeridas() + " h";
                case 6 -> textoHorasCompletas(s);
                default -> s.getEstado();
            });
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = Estilo.secundario(" ");

    public ConstanciasPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JButton actualizar = new JButton("Actualizar");
        JButton guardarCopia = new JButton("Guardar copia del PDF");
        JButton rechazar = Estilo.botonContorno("Rechazar", Estilo.ROJO);
        JButton validar = Estilo.botonPrincipal("1. Validar horas", Estilo.AZUL);
        JButton subir = Estilo.botonPrincipal("2. Subir PDF", Estilo.VERDE);

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Mostrar:"));
        filtros.add(filtro);
        filtros.add(actualizar);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(guardarCopia);
        acciones.add(rechazar);
        acciones.add(validar);
        acciones.add(subir);

        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.add(Estilo.subtitulo("Solicitudes de constancia"), BorderLayout.NORTH);
        textos.add(Estilo.secundario("Primero valida que las horas estén completas; después sube el PDF de la constancia."),
                BorderLayout.SOUTH);

        JPanel barra = new JPanel(new BorderLayout());
        barra.add(filtros, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel arriba = new JPanel(new BorderLayout(0, 12));
        arriba.add(textos, BorderLayout.NORTH);
        arriba.add(barra, BorderLayout.SOUTH);

        tabla.setRowHeight(32);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        EstadoRenderer colores = new EstadoRenderer();
        tabla.getColumnModel().getColumn(COL_COMPLETAS).setCellRenderer(colores);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(colores);
        int[] anchos = {160, 90, 110, 180, 120, 110, 140, 90};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }

        add(arriba, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        filtro.addActionListener(e -> recargar());
        actualizar.addActionListener(e -> recargar());
        validar.addActionListener(e -> validar());
        subir.addActionListener(e -> subir());
        rechazar.addActionListener(e -> rechazar());
        guardarCopia.addActionListener(e -> guardarCopia());
    }

    public final void recargar() {
        try {
            List<SolicitudConstancia> lista = servicio.listarSolicitudes(ESTADOS_FILTRO[filtro.getSelectedIndex()]);
            modelo.setFilas(lista);
            resumen.setText(lista.size() + " solicitud(es).");
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private static String textoHorasCompletas(SolicitudConstancia s) {
        if (s.horasCompletas()) {
            return "Sí";
        }
        BigDecimal faltan = BigDecimal.valueOf(s.getHorasRequeridas()).subtract(s.getHorasAprobadas());
        return "No, faltan " + Formato.horas(faltan) + " h";
    }

    private SolicitudConstancia seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Selecciona una solicitud de la tabla.");
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(fila));
    }

    private void validar() {
        SolicitudConstancia s = seleccionada();
        if (s == null || !Dialogos.confirmar(this, "¿Confirmas que " + s.getAlumnoNombre() + " completó sus "
                + s.getHorasRequeridas() + " horas?")) {
            return;
        }
        try {
            servicio.validarHoras(s.getIdSolicitud());
            Dialogos.info(this, "Horas validadas. Ahora sube el PDF de la constancia (botón 2).");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void subir() {
        SolicitudConstancia s = seleccionada();
        if (s == null) {
            return;
        }
        if (s.getEstado() != EstadoSolicitud.VALIDADA) {
            Dialogos.info(this, s.getEstado() == EstadoSolicitud.SOLICITADA
                    ? "Primero valida las horas de esta solicitud (botón 1)."
                    : "Esta solicitud ya está " + s.getEstado().toString().toLowerCase() + ".");
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige el PDF de la constancia de " + s.getAlumnoNombre());
        selector.setFileFilter(new FileNameExtensionFilter("Archivos PDF", "pdf"));
        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File archivo = selector.getSelectedFile();
        if (!Dialogos.confirmar(this, "¿Subir \"" + archivo.getName() + "\" como constancia de "
                + s.getAlumnoNombre() + "?\nEl alumno podrá descargarla desde su menú.")) {
            return;
        }
        try {
            servicio.subirConstancia(s.getIdSolicitud(), archivo);
            Dialogos.info(this, "Constancia disponible. " + s.getAlumnoNombre() + " ya puede descargarla.");
        } catch (ReglaNegocioException | SQLException | IOException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void rechazar() {
        SolicitudConstancia s = seleccionada();
        if (s == null) {
            return;
        }
        String comentario = Dialogos.pedirComentario(this, "Rechazar solicitud",
                "¿Por qué se rechaza la solicitud de " + s.getAlumnoNombre() + "? El alumno leerá este comentario.");
        if (comentario == null) {
            return;
        }
        try {
            servicio.rechazarSolicitud(s.getIdSolicitud(), comentario);
            Dialogos.info(this, "Solicitud rechazada.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    private void guardarCopia() {
        SolicitudConstancia s = seleccionada();
        if (s == null) {
            return;
        }
        File pdf = PanelDocenteService.archivoDe(s.getRutaPdf());
        if (pdf == null || !pdf.isFile()) {
            Dialogos.info(this, s.getRutaPdf() == null
                    ? "Esta solicitud todavía no tiene PDF."
                    : "No se encontró el PDF en esta computadora:\n" + pdf.getAbsolutePath());
            return;
        }
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar copia de la constancia");
        selector.setSelectedFile(new File(pdf.getName()));
        if (selector.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.copy(pdf.toPath(), selector.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            Dialogos.info(this, "Copia guardada en:\n" + selector.getSelectedFile().getAbsolutePath());
        } catch (IOException e) {
            Dialogos.mostrarError(this, e);
        }
    }
}
