package vista.comun;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Zona punteada para ARRASTRAR un archivo o hacer clic y elegirlo.
 * Sirve para cualquier archivo del proyecto (PDF de constancias, fotos de evidencias…).
 *
 *   ZonaArchivo zona = new ZonaArchivo("Arrastra aquí el PDF", "o haz clic para elegirlo", "Archivos PDF", "pdf");
 *   zona.alElegirArchivo(archivo -> System.out.println(archivo));
 */
public class ZonaArchivo extends JComponent {

    private final String tituloVacio;
    private final String detalleVacio;
    private final String descripcionFiltro;
    private final String[] extensiones;
    private String titulo;
    private String detalle;
    private File archivo;
    private Consumer<File> alElegir = f -> { };

    public ZonaArchivo(String titulo, String detalle, String descripcionFiltro, String... extensiones) {
        this.tituloVacio = titulo;
        this.detalleVacio = detalle;
        this.descripcionFiltro = descripcionFiltro;
        this.extensiones = extensiones;
        this.titulo = titulo;
        this.detalle = detalle;
        setPreferredSize(new Dimension(300, 136));
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        getAccessibleContext().setAccessibleName(titulo + ". " + detalle);
        setTransferHandler(new ManejadorArrastre());
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (isEnabled()) {
                    requestFocusInWindow();
                    elegirConDialogo();
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isEnabled() && (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE)) {
                    elegirConDialogo();
                }
            }
        });
    }

    /** Acción cuando el usuario suelta o elige un archivo válido. */
    public void alElegirArchivo(Consumer<File> accion) {
        this.alElegir = accion;
    }

    public File getArchivo() {
        return archivo;
    }

    /** Muestra el archivo elegido (o vuelve a los textos iniciales con null). */
    public void setArchivo(File archivo) {
        this.archivo = archivo;
        if (archivo == null) {
            setTextos(tituloVacio, detalleVacio);
        } else {
            setTextos(archivo.getName(), tamano(archivo.length()) + " · listo para subir · clic para cambiarlo");
        }
    }

    /** Cambia los textos (por ejemplo, para explicar por qué está deshabilitada). */
    public void setTextos(String titulo, String detalle) {
        this.titulo = titulo;
        this.detalle = detalle;
        getAccessibleContext().setAccessibleName(titulo + ". " + detalle);
        repaint();
    }

    /** Abre el selector de archivos de la computadora. */
    public void elegirConDialogo() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter(descripcionFiltro, extensiones));
        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            recibir(selector.getSelectedFile());
        }
    }

    private boolean extensionValida(File f) {
        String nombre = f.getName().toLowerCase();
        for (String ext : extensiones) {
            if (nombre.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return extensiones.length == 0;
    }

    private void recibir(File f) {
        if (f == null || !f.isFile()) {
            return;
        }
        if (!extensionValida(f)) {
            setTextos("Ese archivo no se puede usar", "Solo se aceptan " + descripcionFiltro.toLowerCase()
                    + " · arrastra o elige otro");
            return;
        }
        setArchivo(f);
        alElegir.accept(f);
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.PUSH_BUTTON;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    public void setEnabled(boolean activo) {
        super.setEnabled(activo);
        setCursor(Cursor.getPredefinedCursor(activo ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean activo = isEnabled();
        boolean listo = archivo != null;

        g2.setColor(!activo ? new Color(0xF7F8FA) : listo ? Estilo.EXITO_SUAVE : new Color(0xF5F7FF));
        g2.fillRoundRect(1, 1, w - 3, h - 3, 14, 14);
        g2.setColor(!activo ? Estilo.BORDE_FUERTE : listo ? Estilo.EXITO_BORDE : Estilo.PRIMARIO_BORDE);
        g2.setStroke(new BasicStroke(isFocusOwner() ? 2.6f : 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{7f, 6f}, 0f));
        g2.drawRoundRect(1, 1, w - 3, h - 3, 14, 14);

        Color tinta = !activo ? Estilo.TEXTO_DESHABILITADO : listo ? Estilo.EXITO : Estilo.PRIMARIO_OSCURO;
        dibujarIcono(g2, w / 2 - 14, 18, tinta, listo);

        g2.setFont(Estilo.fuente(Font.BOLD, 15));
        dibujarCentrado(g2, titulo, 82, w, tinta);
        g2.setFont(Estilo.fuente(Font.PLAIN, 13));
        dibujarCentrado(g2, detalle, 104, w, !activo ? Estilo.TEXTO_SECUNDARIO : listo ? Estilo.EXITO : new Color(0x4F46E5));
        g2.dispose();
    }

    /** Hoja con la esquina doblada y una flecha hacia arriba (o una palomita si ya hay archivo). */
    private static void dibujarIcono(Graphics2D g2, int x, int y, Color color, boolean listo) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D hoja = new Path2D.Float();
        hoja.moveTo(x + 17, y);
        hoja.lineTo(x + 5, y);
        hoja.quadTo(x, y, x, y + 5);
        hoja.lineTo(x, y + 37);
        hoja.quadTo(x, y + 42, x + 5, y + 42);
        hoja.lineTo(x + 23, y + 42);
        hoja.quadTo(x + 28, y + 42, x + 28, y + 37);
        hoja.lineTo(x + 28, y + 11);
        hoja.closePath();
        g2.draw(hoja);
        g2.drawLine(x + 17, y, x + 17, y + 11);
        g2.drawLine(x + 17, y + 11, x + 28, y + 11);
        if (listo) {
            g2.drawLine(x + 8, y + 27, x + 13, y + 32);
            g2.drawLine(x + 13, y + 32, x + 21, y + 22);
        } else {
            g2.drawLine(x + 14, y + 34, x + 14, y + 20);
            g2.drawLine(x + 9, y + 25, x + 14, y + 20);
            g2.drawLine(x + 14, y + 20, x + 19, y + 25);
        }
    }

    private static void dibujarCentrado(Graphics2D g2, String texto, int base, int anchoTotal, Color color) {
        FontMetrics fm = g2.getFontMetrics();
        int anchoMax = anchoTotal - 24;
        String t = texto;
        if (fm.stringWidth(t) > anchoMax) {
            while (fm.stringWidth(t + "…") > anchoMax && t.length() > 4) {
                t = t.substring(0, t.length() - 1);
            }
            t = t + "…";
        }
        g2.setColor(color);
        g2.drawString(t, (anchoTotal - fm.stringWidth(t)) / 2, base);
    }

    private static String tamano(long bytes) {
        return bytes < 1024 * 1024 ? Math.max(1, bytes / 1024) + " KB" : String.format("%.1f MB", bytes / 1048576.0);
    }

    /** Recibe archivos arrastrados desde el explorador de Windows. */
    private class ManejadorArrastre extends TransferHandler {
        @Override
        public boolean canImport(TransferSupport soporte) {
            return isEnabled() && soporte.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean importData(TransferSupport soporte) {
            if (!canImport(soporte)) {
                return false;
            }
            try {
                List<File> archivos = (List<File>) soporte.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (archivos.isEmpty()) {
                    return false;
                }
                recibir(archivos.get(0));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
