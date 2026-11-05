package vista.docente;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import modelo.EstadoRegistro;
import modelo.Evidencia;
import modelo.Programa;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.AreaTexto;
import vista.comun.Avatar;
import vista.comun.BarraProgreso;
import vista.comun.Boton;
import vista.comun.Chip;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.Tarjeta;
import vista.comun.TextoAjustado;
import vista.comun.VentanaDialogo;

/**
 * Detalle de un registro: datos del turno, reporte, evidencias y botones
 * para aprobar o rechazar (el rechazo pide comentario).
 */
public class DetalleRegistroDialog extends VentanaDialogo {

    private static final int MINI_ANCHO = 150;
    private static final int MINI_ALTO = 110;

    private final PanelDocenteService servicio;
    private final RegistroHoras registro;
    private final AreaTexto comentario = new AreaTexto(3, PanelDocenteService.MAX_COMENTARIO);
    private final JPanel contenido = new PanelDesplazable(new GridBagLayout());
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
                Dialogos.aviso(padre, "Registro no encontrado", "Es posible que lo hayan eliminado. La tabla se va a actualizar.");
                return true;
            }
            List<Evidencia> evidencias = servicio.evidenciasDe(idRegistro);
            BigDecimal aprobadas = servicio.horasAprobadas(r.getIdInscripcion());
            DetalleRegistroDialog d = new DetalleRegistroDialog(padre, servicio, r, evidencias, aprobadas);
            d.mostrar();
            return d.huboCambios;
        } catch (SQLException e) {
            Dialogos.mostrarError(padre, e);
            return false;
        }
    }

    private DetalleRegistroDialog(Component padre, PanelDocenteService servicio, RegistroHoras registro,
                                  List<Evidencia> evidencias, BigDecimal horasAprobadas) {
        super(padre, registro.getAlumnoNombre(), datosAlumno(registro));
        setTitle("Revisar registro de horas · " + registro.getAlumnoNombre());
        this.servicio = servicio;
        this.registro = registro;
        setIcono(new Avatar(registro.getAlumnoNombre(), 44));
        setSeparador(true);

        contenido.setOpaque(false);
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 24, 22, 24));
        agregarAvance(horasAprobadas);
        agregarDatosTurno();
        agregarReporte();
        agregarEvidencias(evidencias);
        agregarRevision();

        // Relleno para que todo quede pegado arriba
        GridBagConstraints relleno = restricciones(0);
        relleno.weighty = 1;
        contenido.add(Estilo.panel(new BorderLayout()), relleno);

        setMargenContenido(0, 0, 0, 0);
        setContenido(Estilo.scroll(contenido));
        crearBotones();
        setTamano(740, 760);
    }

    private static String datosAlumno(RegistroHoras r) {
        String matricula = r.getMatricula() == null ? "Sin matrícula" : "Matrícula " + r.getMatricula();
        return matricula + "  ·  " + Programa.texto(r.getTipoPrograma()) + "  ·  " + r.getInstitucionNombre();
    }

    // ------------------------------------------------------------------
    // Secciones
    // ------------------------------------------------------------------

    private void agregarAvance(BigDecimal horasAprobadas) {
        int porcentaje = Formato.porcentaje(horasAprobadas, registro.getHorasRequeridas());
        JPanel fila = Estilo.panel(new BorderLayout(12, 0));
        fila.add(Estilo.etiqueta("Horas aprobadas del alumno", Estilo.fuente(Font.BOLD, 13), Estilo.TEXTO), BorderLayout.WEST);
        fila.add(Estilo.etiqueta(Formato.horas(horasAprobadas) + " de " + registro.getHorasRequeridas() + " h  ·  "
                + porcentaje + "%", Estilo.fuente(Font.PLAIN, 13), Estilo.TEXTO_SECUNDARIO), BorderLayout.EAST);
        BarraProgreso barra = new BarraProgreso(8);
        barra.setValor(porcentaje);
        agregar(fila, 0);
        agregar(barra, 8);
    }

    private void agregarDatosTurno() {
        JPanel datos = Estilo.panel(new GridLayout(1, 3, 12, 0));
        datos.add(dato("Fecha", Formato.fecha(registro.getFecha())));
        datos.add(dato("Horario", Formato.horario(registro.getHoraInicio(), registro.getHoraFin())));
        datos.add(dato("Horas del turno", Formato.horas(registro.getHorasCalculadas()) + " h"));
        agregar(datos, 20);
    }

    private void agregarReporte() {
        agregar(Estilo.subtitulo("Reporte de actividades"), 22);
        AreaTexto reporte = AreaTexto.soloLectura(registro.getReporte(), 4);
        reporte.setNombreAccesible("Reporte de actividades");
        agregar(reporte, 8);
    }

    private void agregarEvidencias(List<Evidencia> evidencias) {
        agregar(Estilo.subtitulo("Evidencias (" + evidencias.size() + ")"), 22);
        if (evidencias.isEmpty()) {
            agregar(Estilo.secundario("El alumno no subió imágenes."), 6);
            return;
        }
        JPanel miniaturas = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        for (Evidencia ev : evidencias) {
            miniaturas.add(crearMiniatura(ev));
            miniaturas.add(Box.createHorizontalStrut(10));
        }
        agregar(miniaturas, 8);
        agregar(Estilo.secundario("Haz clic en una imagen para verla en grande."), 6);
    }

    private void agregarRevision() {
        if (registro.getEstado() != EstadoRegistro.PENDIENTE) {
            Color[] colores = Estilo.coloresEstado(registro.getEstado().toString());
            Tarjeta caja = new Tarjeta(new BorderLayout(0, 4), colores[0], null, 10);
            caja.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            caja.add(Estilo.etiqueta("Este registro ya fue " + registro.getEstado().toString().toLowerCase() + ".",
                    Estilo.fuente(Font.BOLD, 14), colores[1]), BorderLayout.NORTH);
            if (registro.getComentarioDocente() != null && !registro.getComentarioDocente().isBlank()) {
                TextoAjustado texto = new TextoAjustado("Comentario: " + registro.getComentarioDocente(),
                        Estilo.fuente(Font.PLAIN, 13), colores[1], true);
                texto.setAnchoAjuste(640);
                caja.add(texto, BorderLayout.CENTER);
            }
            agregar(caja, 24);
            return;
        }
        agregar(Estilo.subtitulo("Comentario para el alumno"), 24);
        agregar(Estilo.secundario("Opcional si apruebas. Obligatorio si rechazas: el alumno lo leerá para corregir su registro."), 4);
        comentario.setPlaceholder("Ej. Agrega una foto donde se vea la actividad");
        comentario.setNombreAccesible("Comentario para el alumno");
        agregar(comentario, 8);
    }

    private void crearBotones() {
        setPie(Chip.deEstado(registro.getEstado().toString()));
        Boton cerrar = agregarBotonCancelar("Cerrar");
        if (registro.getEstado() == EstadoRegistro.PENDIENTE) {
            Boton rechazar = agregarBoton(Estilo.botonPeligro("Rechazar"));
            Boton aprobar = agregarBoton(Estilo.botonExito("Aprobar"));
            rechazar.addActionListener(e -> rechazar());
            aprobar.addActionListener(e -> aprobar());
            setFocoInicial(comentario.getAreaTexto());
        } else {
            setBotonPorDefecto(cerrar);
        }
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void aprobar() {
        try {
            servicio.aprobarRegistro(registro.getIdRegistro(), comentario.getTexto());
            huboCambios = true;
            Dialogos.exito(this, "Registro aprobado", "Las " + Formato.horas(registro.getHorasCalculadas())
                    + " h ya cuentan en el porcentaje de " + registro.getAlumnoNombre() + ".");
            cerrar();
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void rechazar() {
        if (comentario.getTexto().isBlank()) {
            comentario.setError("Escribe un comentario para rechazar el registro.");
            comentario.requestFocusInWindow();
            return;
        }
        try {
            servicio.rechazarRegistro(registro.getIdRegistro(), comentario.getTexto());
            huboCambios = true;
            Dialogos.exito(this, "Registro rechazado", registro.getAlumnoNombre()
                    + " verá tu comentario para corregirlo.");
            cerrar();
        } catch (ReglaNegocioException e) {
            comentario.setError(e.getMessage());
            comentario.requestFocusInWindow();
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    /** Muestra una evidencia en grande, con el mismo diseño de las demás ventanas. */
    private void verImagen(Evidencia ev, File archivo, BufferedImage imagen) {
        VentanaDialogo v = new VentanaDialogo(this, ev.getNombreParaMostrar(),
                "Evidencia de " + registro.getAlumnoNombre() + "  ·  " + Formato.fecha(registro.getFecha()));
        Rectangle pantalla = getGraphicsConfiguration().getBounds();
        int maxAncho = Math.min(960, pantalla.width - 160);
        int maxAlto = Math.min(560, pantalla.height - 300);
        ImageIcon grande = new ImageIcon(escalar(imagen, maxAncho, maxAlto));
        JLabel foto = new JLabel(grande, SwingConstants.CENTER);
        foto.getAccessibleContext().setAccessibleName("Imagen " + ev.getNombreParaMostrar());

        v.setSeparador(true);
        v.setFondoContenido(Estilo.FONDO);
        v.setMargenContenido(18, 18, 18, 18);
        v.setContenido(foto);
        v.setAncho(Math.max(520, grande.getIconWidth() + 36));
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Boton abrir = v.agregarBoton(Estilo.botonSecundario("Abrir en el visor de imágenes"));
            abrir.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(archivo);
                } catch (IOException ex) {
                    Dialogos.mostrarError(v, ex);
                }
            });
        }
        Boton cerrar = v.agregarBoton(Estilo.botonPrincipal("Cerrar"));
        cerrar.addActionListener(e -> v.cerrar());
        v.setBotonPorDefecto(cerrar);
        v.mostrar();
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    private JButton crearMiniatura(Evidencia ev) {
        File archivo = PanelDocenteService.archivoDe(ev.getRutaArchivo());
        BufferedImage imagen = leerImagen(archivo);
        if (imagen == null) {
            Miniatura vacia = new Miniatura(null);
            vacia.setToolTipText("Ruta guardada: " + ev.getRutaArchivo());
            vacia.getAccessibleContext().setAccessibleName("No se encontró la imagen " + ev.getNombreParaMostrar());
            vacia.setEnabled(false);
            return vacia;
        }
        Miniatura boton = new Miniatura(new ImageIcon(escalar(imagen, MINI_ANCHO - 10, MINI_ALTO - 10)).getImage());
        boton.setToolTipText(ev.getNombreParaMostrar());
        boton.getAccessibleContext().setAccessibleName("Ver " + ev.getNombreParaMostrar());
        boton.addActionListener(e -> verImagen(ev, archivo, imagen));
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
        Tarjeta p = new Tarjeta(new BorderLayout(0, 2), Estilo.FONDO, null, 10);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.add(Estilo.secundario(etiqueta), BorderLayout.NORTH);
        p.add(Estilo.etiqueta(valor, Estilo.fuente(Font.BOLD, 16), Estilo.TEXTO), BorderLayout.CENTER);
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

    /** Miniatura redondeada de una evidencia; el borde se pinta índigo al pasar el mouse. */
    private static class Miniatura extends JButton {
        private final Image imagen;

        Miniatura(Image imagen) {
            this.imagen = imagen;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(imagen == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(MINI_ANCHO, MINI_ALTO);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 12, 12);
            g2.setColor(Estilo.FONDO);
            g2.fill(forma);
            if (imagen != null) {
                int iw = imagen.getWidth(null);
                int ih = imagen.getHeight(null);
                g2.clip(forma);
                g2.drawImage(imagen, (w - iw) / 2, (h - ih) / 2, null);
                g2.setClip(null);
            } else {
                g2.setFont(Estilo.fuente(Font.PLAIN, 12));
                g2.setColor(Estilo.TEXTO_SECUNDARIO);
                FontMetrics fm = g2.getFontMetrics();
                String[] lineas = {"No se encontró", "la imagen"};
                int y = h / 2 - fm.getHeight() + fm.getAscent();
                for (String linea : lineas) {
                    g2.drawString(linea, (w - fm.stringWidth(linea)) / 2, y);
                    y += fm.getHeight();
                }
            }
            boolean resaltada = isEnabled() && (getModel().isRollover() || isFocusOwner());
            g2.setColor(resaltada ? Estilo.PRIMARIO : Estilo.BORDE);
            g2.setStroke(new BasicStroke(resaltada ? 2f : 1f));
            g2.drawRoundRect(resaltada ? 1 : 0, resaltada ? 1 : 0, w - (resaltada ? 3 : 1), h - (resaltada ? 3 : 1), 12, 12);
            g2.dispose();
        }
    }

    /** Panel que se ajusta al ancho de la ventana (así no aparece barra horizontal). */
    private static class PanelDesplazable extends JPanel implements Scrollable {
        PanelDesplazable(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientacion, int direccion) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientacion, int direccion) {
            return Math.max(16, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

}
