package vista.docente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import modelo.EstadoRegistro;
import modelo.Evidencia;
import modelo.Programa;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;

/**
 * Detalle de un registro: datos del turno, reporte, evidencias y botones
 * para aprobar o rechazar (el rechazo pide comentario).
 */
public class DetalleRegistroDialog extends JDialog {

    private static final int MINI_ANCHO = 150;
    private static final int MINI_ALTO = 110;

    private final PanelDocenteService servicio;
    private final RegistroHoras registro;
    private final JTextArea comentario = new JTextArea(3, 40);
    private final JPanel contenido = new JPanel(new GridBagLayout());
    private int filaActual = 0;
    private boolean huboCambios = false;

    /**
     * Abre el detalle y espera a que se cierre.
     *
     * @return true si el registro se aprobó o rechazó (hay que recargar la tabla)
     */
    public static boolean abrir(Component padre, PanelDocenteService servicio, int idRegistro) {
        try {
            RegistroHoras r = servicio.obtenerRegistro(idRegistro);
            if (r == null) {
                Dialogos.info(padre, "El registro ya no existe.");
                return true;
            }
            List<Evidencia> evidencias = servicio.evidenciasDe(idRegistro);
            BigDecimal aprobadas = servicio.horasAprobadas(r.getIdInscripcion());
            DetalleRegistroDialog d = new DetalleRegistroDialog(
                    SwingUtilities.getWindowAncestor(padre), servicio, r, evidencias, aprobadas);
            d.setVisible(true);
            return d.huboCambios;
        } catch (SQLException e) {
            Dialogos.mostrarError(padre, e);
            return false;
        }
    }

    private DetalleRegistroDialog(Window padre, PanelDocenteService servicio, RegistroHoras registro,
                                  List<Evidencia> evidencias, BigDecimal horasAprobadas) {
        super(padre, "Revisar registro de horas", ModalityType.APPLICATION_MODAL);
        this.servicio = servicio;
        this.registro = registro;

        contenido.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        agregarAlumno(horasAprobadas);
        agregarDatosTurno();
        agregarReporte();
        agregarEvidencias(evidencias);
        agregarRevision();

        // Relleno para que todo quede pegado arriba
        GridBagConstraints relleno = restricciones(0);
        relleno.weighty = 1;
        contenido.add(new JPanel(), relleno);

        JScrollPane scroll = new JScrollPane(contenido);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        add(crearBotones(), BorderLayout.SOUTH);
        setSize(720, 760);
        setLocationRelativeTo(padre);
    }

    // ------------------------------------------------------------------
    // Secciones
    // ------------------------------------------------------------------

    private void agregarAlumno(BigDecimal horasAprobadas) {
        agregar(Estilo.titulo(registro.getAlumnoNombre()), 0);
        String matricula = registro.getMatricula() == null ? "Sin matrícula" : "Matrícula " + registro.getMatricula();
        agregar(Estilo.secundario(matricula + "  ·  " + Programa.texto(registro.getTipoPrograma())
                + "  ·  " + registro.getInstitucionNombre()), 4);

        int porcentaje = Formato.porcentaje(horasAprobadas, registro.getHorasRequeridas());
        JProgressBar barra = new JProgressBar(0, 100);
        barra.setValue(porcentaje);
        barra.setForeground(Estilo.VERDE);
        barra.setPreferredSize(new Dimension(10, 10));
        agregar(new JLabel("Horas aprobadas: " + Formato.horas(horasAprobadas) + " de "
                + registro.getHorasRequeridas() + " h (" + porcentaje + "%)"), 16);
        agregar(barra, 6);
    }

    private void agregarDatosTurno() {
        JPanel datos = new JPanel(new GridLayout(1, 3, 12, 0));
        datos.add(dato("Fecha", Formato.fecha(registro.getFecha())));
        datos.add(dato("Horario", Formato.horario(registro.getHoraInicio(), registro.getHoraFin())));
        datos.add(dato("Horas del turno", Formato.horas(registro.getHorasCalculadas()) + " h"));
        agregar(datos, 20);
    }

    private void agregarReporte() {
        agregar(Estilo.subtitulo("Reporte de actividades"), 20);
        JTextArea reporte = new JTextArea(registro.getReporte());
        reporte.setEditable(false);
        reporte.setLineWrap(true);
        reporte.setWrapStyleWord(true);
        reporte.setRows(4);
        reporte.setBackground(Estilo.FONDO);
        reporte.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        agregar(new JScrollPane(reporte), 6);
    }

    private void agregarEvidencias(List<Evidencia> evidencias) {
        agregar(Estilo.subtitulo("Evidencias (" + evidencias.size() + ")"), 20);
        if (evidencias.isEmpty()) {
            agregar(Estilo.secundario("El alumno no subió imágenes."), 6);
            return;
        }
        JPanel miniaturas = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (Evidencia ev : evidencias) {
            miniaturas.add(crearMiniatura(ev));
            miniaturas.add(Box.createHorizontalStrut(10));
        }
        agregar(miniaturas, 6);
        agregar(Estilo.secundario("Haz clic en una imagen para verla en grande."), 4);
    }

    private void agregarRevision() {
        if (registro.getEstado() != EstadoRegistro.PENDIENTE) {
            Color[] colores = Estilo.coloresEstado(registro.getEstado().toString());
            JLabel estado = new JLabel("Este registro ya fue " + registro.getEstado().toString().toLowerCase() + ".");
            estado.setOpaque(true);
            estado.setBackground(colores[0]);
            estado.setForeground(colores[1]);
            estado.setFont(estado.getFont().deriveFont(Font.BOLD));
            estado.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            agregar(estado, 24);
            if (registro.getComentarioDocente() != null) {
                agregar(new JLabel("Comentario: " + registro.getComentarioDocente()), 8);
            }
            return;
        }
        agregar(Estilo.subtitulo("Comentario para el alumno"), 24);
        agregar(Estilo.secundario("Obligatorio si rechazas: el alumno lo leerá para corregir su registro."), 2);
        comentario.setLineWrap(true);
        comentario.setWrapStyleWord(true);
        agregar(new JScrollPane(comentario), 6);
    }

    private JPanel crearBotones() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        barra.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Estilo.BORDE));
        JButton cerrar = new JButton("Cerrar");
        cerrar.addActionListener(e -> dispose());
        barra.add(cerrar);
        if (registro.getEstado() == EstadoRegistro.PENDIENTE) {
            JButton rechazar = Estilo.botonContorno("Rechazar", Estilo.ROJO);
            JButton aprobar = Estilo.botonPrincipal("Aprobar", Estilo.VERDE);
            rechazar.addActionListener(e -> rechazar());
            aprobar.addActionListener(e -> aprobar());
            barra.add(rechazar);
            barra.add(aprobar);
        }
        return barra;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void aprobar() {
        try {
            servicio.aprobarRegistro(registro.getIdRegistro(), comentario.getText());
            huboCambios = true;
            Dialogos.info(this, "Registro aprobado. Las " + Formato.horas(registro.getHorasCalculadas())
                    + " h ya cuentan en el porcentaje de " + registro.getAlumnoNombre() + ".");
            dispose();
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void rechazar() {
        try {
            servicio.rechazarRegistro(registro.getIdRegistro(), comentario.getText());
            huboCambios = true;
            Dialogos.info(this, "Registro rechazado. " + registro.getAlumnoNombre()
                    + " verá tu comentario para corregirlo.");
            dispose();
        } catch (ReglaNegocioException e) {
            Dialogos.mostrarError(this, e);
            comentario.requestFocusInWindow();
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    private JButton crearMiniatura(Evidencia ev) {
        JButton boton = new JButton();
        boton.setPreferredSize(new Dimension(MINI_ANCHO, MINI_ALTO));
        boton.setToolTipText(ev.getNombreParaMostrar());
        File archivo = PanelDocenteService.archivoDe(ev.getRutaArchivo());
        BufferedImage imagen = leerImagen(archivo);
        if (imagen == null) {
            boton.setText("<html><center>No se encontró<br>la imagen</center></html>");
            boton.setToolTipText("Ruta guardada: " + ev.getRutaArchivo());
            boton.setEnabled(false);
            return boton;
        }
        boton.setIcon(new ImageIcon(escalar(imagen, MINI_ANCHO - 8, MINI_ALTO - 8)));
        boton.getAccessibleContext().setAccessibleName("Ver " + ev.getNombreParaMostrar());
        boton.addActionListener(e -> {
            JLabel grande = new JLabel(new ImageIcon(escalar(imagen, 900, 600)));
            JOptionPane.showMessageDialog(this, new JScrollPane(grande), ev.getNombreParaMostrar(),
                    JOptionPane.PLAIN_MESSAGE);
        });
        return boton;
    }

    private static BufferedImage leerImagen(File archivo) {
        try {
            return archivo == null || !archivo.isFile() ? null : ImageIO.read(archivo);
        } catch (IOException e) {
            return null;
        }
    }

    /** Reduce la imagen para que quepa en el tamaño dado sin deformarla. */
    private static Image escalar(BufferedImage img, int maxAncho, int maxAlto) {
        double factor = Math.min(1.0, Math.min((double) maxAncho / img.getWidth(), (double) maxAlto / img.getHeight()));
        int ancho = Math.max(1, (int) (img.getWidth() * factor));
        int alto = Math.max(1, (int) (img.getHeight() * factor));
        return img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
    }

    private static JPanel dato(String etiqueta, String valor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBackground(Estilo.FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel v = new JLabel(valor);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 15f));
        p.add(Estilo.secundario(etiqueta), BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private void agregar(JComponent componente, int espacioArriba) {
        contenido.add(componente, restricciones(espacioArriba));
    }

    private GridBagConstraints restricciones(int espacioArriba) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = filaActual++;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(espacioArriba, 0, 0, 0);
        return c;
    }
}
