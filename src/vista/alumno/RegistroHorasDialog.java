package vista.alumno;

import dao.EvidenciaDAO;
import dao.ProgresoAlumnoDAO;
import dao.RegistroHorasDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import modelo.EstadoRegistro;
import modelo.Evidencia;
import modelo.ProgresoAlumno;
import modelo.RegistroHoras;
import modelo.Usuario;
import util.NotificadorHoras;
import util.Sesion;
import util.ValidarHoras;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;

/**
 * Parte 4: formulario para registrar un turno de horas (y para corregir uno rechazado).
 *
 * Es un diálogo modal: el menú del alumno se recarga solo cuando se cierra.
 *
 * Conexión con el menú (donde se crea MenuPrincipalFrame):
 *     menu.setAccionAnadirHoras(() -> new RegistroHorasDialog(menu).mostrar());
 *     menu.setAccionCorregirRegistro(id -> new RegistroHorasDialog(menu, id).mostrar());
 */
public class RegistroHorasDialog extends JDialog {

    private static final Path CARPETA_EVIDENCIAS = Paths.get("evidencias");
    private static final String PREFIJO_RUTA = "evidencias/";
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Usuario alumno = Sesion.getUsuario();
    private final RegistroHorasDAO registrosDAO = new RegistroHorasDAO();
    private final EvidenciaDAO evidenciasDAO = new EvidenciaDAO();

    // Datos cargados de la base
    private int idInscripcion;
    private RegistroHoras original;                 // null = turno nuevo
    private List<Evidencia> evidenciasOriginales = new ArrayList<>();
    private boolean listo = false;

    // Componentes
    private final JSpinner spFecha = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JSpinner spInicio = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JSpinner spFin = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JLabel lblHoras = new JLabel("—");
    private final JTextArea txtReporte = new JTextArea(6, 30);
    private final DefaultListModel<Adjunto> adjuntos = new DefaultListModel<>();
    private final JList<Adjunto> listaAdjuntos = new JList<>(adjuntos);
    private final List<Evidencia> evidenciasQuitadas = new ArrayList<>();
    private final JButton btnAgregar = new JButton("Agregar imágenes");
    private final JButton btnQuitar = new JButton("Quitar");
    private final JButton btnCancelar = new JButton("Cancelar");
    private final JButton btnEnviar = Estilo.botonPrincipal("Enviar a revisión");
    private final JLabel lblEstado = new JLabel(" ");

    /** Turno nuevo. */
    public RegistroHorasDialog(Frame owner) {
        this(owner, 0);
    }

    /** Corregir un registro rechazado (idRegistro > 0) o crear uno nuevo (0). */
    public RegistroHorasDialog(Frame owner, int idRegistro) {
        super(owner, idRegistro > 0 ? "Corregir registro rechazado" : "Registrar horas", true);
        try {
            if (!cargarDatos(idRegistro)) {
                return;
            }
        } catch (SQLException e) {
            Dialogos.mostrarError(owner, e);
            return;
        }
        construirPantalla();
        rellenar();
        listo = true;
    }

    /** Muestra el formulario (bloquea hasta que se cierre). No hace nada si no se pudo cargar. */
    public void mostrar() {
        if (listo) {
            setVisible(true);
        }
    }

    // ------------------------------------------------------------------ carga

    private boolean cargarDatos(int idRegistro) throws SQLException {
        ProgresoAlumno p = new ProgresoAlumnoDAO().buscarPorAlumno(alumno.getIdUsuario());
        if (p == null) {
            aviso("Aún no estás inscrito a una institución.");
            return false;
        }
        idInscripcion = p.getIdInscripcion();
        if (idRegistro > 0) {
            original = registrosDAO.buscarPorId(idRegistro);
            if (original == null
                    || original.getIdInscripcion() != idInscripcion
                    || original.getEstado() != EstadoRegistro.RECHAZADO) {
                aviso("Ese registro no se puede corregir.");
                return false;
            }
            evidenciasOriginales = evidenciasDAO.listarPorRegistro(idRegistro);
        }
        return true;
    }

    // ------------------------------------------------------------- pantalla

    private void construirPantalla() {
        spFecha.setEditor(new JSpinner.DateEditor(spFecha, "dd/MM/yyyy"));
        spInicio.setEditor(new JSpinner.DateEditor(spInicio, "HH:mm"));
        spFin.setEditor(new JSpinner.DateEditor(spFin, "HH:mm"));
        for (JSpinner s : new JSpinner[]{spFecha, spInicio, spFin}) {
            s.setPreferredSize(new Dimension(120, 28));
        }
        txtReporte.setLineWrap(true);
        txtReporte.setWrapStyleWord(true);
        listaAdjuntos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(14, 18, 8, 18));
        int y = 0;
        if (original != null) {
            JTextArea comentario = new JTextArea(textoComentario(), 3, 30);
            comentario.setEditable(false);
            comentario.setLineWrap(true);
            comentario.setWrapStyleWord(true);
            fila(form, y++, "Comentario del docente", new JScrollPane(comentario), 0);
        }
        fila(form, y++, "Fecha", izquierda(spFecha), 0);
        fila(form, y++, "Hora de inicio", izquierda(spInicio), 0);
        fila(form, y++, "Hora de fin", izquierda(spFin), 0);
        fila(form, y++, "Horas del turno", lblHoras, 0);
        fila(form, y++, "Reporte de actividades", new JScrollPane(txtReporte), 1);
        fila(form, y++, "Evidencias (imágenes)", panelEvidencias(), 1);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        botones.add(lblEstado);
        botones.add(btnCancelar);
        botones.add(btnEnviar);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(form, BorderLayout.CENTER);
        raiz.add(botones, BorderLayout.SOUTH);
        setContentPane(raiz);

        // Eventos
        javax.swing.event.ChangeListener alCambiar = e -> actualizarHoras();
        spInicio.addChangeListener(alCambiar);
        spFin.addChangeListener(alCambiar);
        btnAgregar.addActionListener(e -> agregarImagenes());
        btnQuitar.addActionListener(e -> quitarSeleccionada());
        btnCancelar.addActionListener(e -> dispose());
        btnEnviar.addActionListener(e -> enviar());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(580, 700);
        setLocationRelativeTo(getOwner());
    }

    private JPanel panelEvidencias() {
        JScrollPane scroll = new JScrollPane(listaAdjuntos);
        scroll.setPreferredSize(new Dimension(300, 110));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 6));
        botones.add(btnAgregar);
        botones.add(javax.swing.Box.createHorizontalStrut(8));
        botones.add(btnQuitar);
        JPanel p = new JPanel(new BorderLayout());
        p.add(scroll, BorderLayout.CENTER);
        p.add(botones, BorderLayout.SOUTH);
        return p;
    }

    private static void fila(JPanel panel, int y, String etiqueta, Component campo, double pesoY) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = y;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(6, 0, 6, 14);
        panel.add(new JLabel(etiqueta), g);

        g = new GridBagConstraints();
        g.gridx = 1;
        g.gridy = y;
        g.weightx = 1;
        g.weighty = pesoY;
        g.fill = pesoY > 0 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);
        panel.add(campo, g);
    }

    private static JPanel izquierda(Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.add(c);
        return p;
    }

    private String textoComentario() {
        String c = original.getComentarioDocente();
        return c == null || c.isBlank() ? "Tu docente no escribió un comentario." : c;
    }

    /** Pone los datos del registro (si se está corrigiendo) o valores iniciales (si es nuevo). */
    private void rellenar() {
        if (original != null) {
            spFecha.setValue(aDate(original.getFecha(), LocalTime.MIDNIGHT));
            spInicio.setValue(aDate(LocalDate.now(), original.getHoraInicio()));
            spFin.setValue(aDate(LocalDate.now(), original.getHoraFin()));
            txtReporte.setText(original.getReporte());
            for (Evidencia e : evidenciasOriginales) {
                adjuntos.addElement(new Adjunto(e, null));
            }
        } else {
            spInicio.setValue(aDate(LocalDate.now(), LocalTime.of(9, 0)));
            spFin.setValue(aDate(LocalDate.now(), LocalTime.of(13, 0)));
        }
        actualizarHoras();
    }

    private void actualizarHoras() {
        LocalTime inicio = aHora(spInicio.getValue());
        LocalTime fin = aHora(spFin.getValue());
        lblHoras.setText(fin.isAfter(inicio)
                ? Formato.horas(ValidarHoras.calcularHoras(inicio, fin)) + " h"
                : "—");
    }

    // ------------------------------------------------------------ evidencias

    private void agregarImagenes() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setFileFilter(new FileNameExtensionFilter("Imágenes (png, jpg, jpeg)", "png", "jpg", "jpeg"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        for (File f : fc.getSelectedFiles()) {
            String nombre = f.getName().toLowerCase();
            if (!(nombre.endsWith(".png") || nombre.endsWith(".jpg") || nombre.endsWith(".jpeg"))) {
                aviso("«" + f.getName() + "» no es una imagen png o jpg y no se agregó.");
            } else if (f.length() > MAX_BYTES) {
                aviso("«" + f.getName() + "» pesa más de 5 MB y no se agregó.");
            } else {
                adjuntos.addElement(new Adjunto(null, f));
            }
        }
    }

    private void quitarSeleccionada() {
        int i = listaAdjuntos.getSelectedIndex();
        if (i < 0) {
            aviso("Selecciona una imagen de la lista para quitarla.");
            return;
        }
        Adjunto a = adjuntos.remove(i);
        if (a.existente != null) {
            evidenciasQuitadas.add(a.existente);
        }
    }

    // ----------------------------------------------------------------- envío

    private void enviar() {
        try {
            spFecha.commitEdit();
            spInicio.commitEdit();
            spFin.commitEdit();
        } catch (ParseException ex) {
            aviso("La fecha o la hora no tienen un formato válido.");
            return;
        }
        LocalDate fecha = aFecha(spFecha.getValue());
        LocalTime inicio = aHora(spInicio.getValue());
        LocalTime fin = aHora(spFin.getValue());
        String reporte = txtReporte.getText().trim();

        String error = ValidarHoras.validar(fecha, inicio, fin, reporte);
        if (error == null && adjuntos.isEmpty()) {
            error = "Agrega al menos una imagen como evidencia.";
        }
        if (error != null) {
            aviso(error);
            return;
        }
        boolean esReenvio = original != null;
        try {
            int idExcluir = esReenvio ? original.getIdRegistro() : 0;
            if (registrosDAO.existeTraslape(idInscripcion, fecha, inicio, fin, idExcluir)) {
                aviso("Ese horario se encima con otro turno que ya registraste ese día.");
                return;
            }
        } catch (SQLException ex) {
            Dialogos.mostrarError(this, ex);
            return;
        }

        BigDecimal horas = ValidarHoras.calcularHoras(inicio, fin);
        RegistroHoras r = new RegistroHoras();
        r.setIdRegistro(esReenvio ? original.getIdRegistro() : 0);
        r.setIdInscripcion(idInscripcion);
        r.setFecha(fecha);
        r.setHoraInicio(inicio);
        r.setHoraFin(fin);
        r.setHorasCalculadas(horas);
        r.setReporte(reporte);
        r.setEstado(EstadoRegistro.PENDIENTE);

        List<File> archivosNuevos = new ArrayList<>();
        for (int i = 0; i < adjuntos.size(); i++) {
            if (adjuntos.get(i).nuevo != null) {
                archivosNuevos.add(adjuntos.get(i).nuevo);
            }
        }
        List<Evidencia> quitadas = new ArrayList<>(evidenciasQuitadas);
        List<Integer> idsQuitadas = new ArrayList<>();
        for (Evidencia q : quitadas) {
            idsQuitadas.add(q.getIdEvidencia());
        }
        String nombreAlumno = alumno.getNombre();

        setEnviando(true);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                List<Evidencia> nuevas = copiarArchivos(archivosNuevos);
                try {
                    if (esReenvio) {
                        if (!registrosDAO.reenviar(r, nuevas, idsQuitadas)) {
                            borrarArchivos(nuevas);
                            throw new IllegalStateException(
                                    "Este registro ya no está rechazado, así que no se puede reenviar.");
                        }
                    } else {
                        registrosDAO.insertar(r, nuevas);
                    }
                } catch (SQLException ex) {
                    borrarArchivos(nuevas);
                    throw ex;
                }
                // Ya quedó guardado: ahora sí se borran del disco las imágenes que el alumno quitó
                for (Evidencia q : quitadas) {
                    borrarArchivo(q.getRutaArchivo());
                }
                try {
                    return NotificadorHoras.avisarTurnoPendiente(idInscripcion, nombreAlumno, esReenvio,
                            fecha, inicio, fin, horas, reporte);
                } catch (Exception ex) {
                    return false; // el turno ya está guardado; solo falló el aviso
                }
            }

            @Override
            protected void done() {
                try {
                    boolean correoOk = get();
                    JOptionPane.showMessageDialog(RegistroHorasDialog.this,
                            correoOk
                                    ? "Tu turno se envió a revisión y se avisó a tu docente por correo."
                                    : "Tu turno se guardó como pendiente, pero no se pudo avisar al docente por correo.",
                            "Registro de horas", JOptionPane.INFORMATION_MESSAGE);
                    RegistroHorasDialog.this.dispose();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    setEnviando(false);
                } catch (ExecutionException ex) {
                    setEnviando(false);
                    Throwable causa = ex.getCause();
                    if (causa instanceof SQLException) {
                        Dialogos.mostrarError(RegistroHorasDialog.this, (SQLException) causa);
                    } else {
                        JOptionPane.showMessageDialog(RegistroHorasDialog.this,
                                causa == null ? "Error desconocido." : causa.getMessage(),
                                "No se pudo guardar el turno", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }.execute();
    }

    private void setEnviando(boolean enviando) {
        btnEnviar.setEnabled(!enviando);
        btnCancelar.setEnabled(!enviando);
        btnAgregar.setEnabled(!enviando);
        btnQuitar.setEnabled(!enviando);
        lblEstado.setText(enviando ? "Guardando y avisando al docente..." : " ");
    }

    // ---------------------------------------------------- archivos en disco

    /** Copia las imágenes a la carpeta evidencias/ con nombre único y devuelve sus Evidencia (sin ids). */
    private List<Evidencia> copiarArchivos(List<File> archivos) throws IOException {
        Files.createDirectories(CARPETA_EVIDENCIAS);
        List<Evidencia> copiadas = new ArrayList<>();
        long marca = System.currentTimeMillis();
        int n = 0;
        try {
            for (File f : archivos) {
                String nombre = f.getName();
                int punto = nombre.lastIndexOf('.');
                String ext = punto >= 0 ? nombre.substring(punto).toLowerCase() : "";
                String destino = "insc" + idInscripcion + "_" + marca + "_" + (++n) + ext;
                Files.copy(f.toPath(), CARPETA_EVIDENCIAS.resolve(destino), StandardCopyOption.REPLACE_EXISTING);
                copiadas.add(new Evidencia(0, 0, PREFIJO_RUTA + destino, nombre));
            }
        } catch (IOException ex) {
            borrarArchivos(copiadas);
            throw ex;
        }
        return copiadas;
    }

    private static void borrarArchivos(List<Evidencia> evidencias) {
        for (Evidencia e : evidencias) {
            borrarArchivo(e.getRutaArchivo());
        }
    }

    private static void borrarArchivo(String ruta) {
        if (ruta == null || !ruta.startsWith(PREFIJO_RUTA)) {
            return; // solo se borra dentro de la carpeta evidencias/
        }
        try {
            Files.deleteIfExists(Paths.get(ruta));
        } catch (IOException ignorado) {
            // un archivo huérfano en disco no es grave
        }
    }

    // --------------------------------------------------------------- ayudas

    private void aviso(String texto) {
        Component padre = isShowing() ? this : getOwner();
        JOptionPane.showMessageDialog(padre, texto, "Registro de horas", JOptionPane.WARNING_MESSAGE);
    }

    private static Date aDate(LocalDate fecha, LocalTime hora) {
        return Date.from(fecha.atTime(hora).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate aFecha(Object valor) {
        return ((Date) valor).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalTime aHora(Object valor) {
        return ((Date) valor).toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
                .withSecond(0).withNano(0);
    }

    /** Elemento de la lista de evidencias: una ya guardada o un archivo nuevo por subir. */
    private static final class Adjunto {
        final Evidencia existente;
        final File nuevo;

        Adjunto(Evidencia existente, File nuevo) {
            this.existente = existente;
            this.nuevo = nuevo;
        }

        @Override
        public String toString() {
            return existente != null ? existente.getNombreOriginal() + " (ya guardada)" : nuevo.getName();
        }
    }
}