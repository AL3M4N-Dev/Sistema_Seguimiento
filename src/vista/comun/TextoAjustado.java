package vista.comun;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.text.View;

/**
 * Texto de varias líneas que se acomoda solo a un ancho fijo (para mensajes y rutas largas).
 * A diferencia de un JLabel, también corta palabras muy largas, como las rutas de archivos.
 *
 *   TextoAjustado t = new TextoAjustado("Mensaje largo…", Estilo.fuente(Font.PLAIN, 14), Estilo.TEXTO, false);
 *   t.setAnchoAjuste(360);
 */
public class TextoAjustado extends JTextArea {

    private static final int ANCHO_MINIMO = 240;

    private int anchoAjuste;
    private boolean ajusteAutomatico;
    private java.awt.Container contenedorObservado;
    private final java.awt.event.ComponentAdapter alCambiarTamano = new java.awt.event.ComponentAdapter() {
        @Override
        public void componentResized(java.awt.event.ComponentEvent e) {
            ajustarAlContenedor();
        }
    };

    /**
     * @param seleccionable true si el usuario puede seleccionarlo y copiarlo (rutas, detalles de error)
     */
    public TextoAjustado(String texto, Font fuente, Color color, boolean seleccionable) {
        super(texto == null ? "" : texto);
        setFont(fuente);
        setForeground(color);
        setLineWrap(true);
        setWrapStyleWord(true);
        setEditable(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setAlignmentX(LEFT_ALIGNMENT);
        if (seleccionable) {
            setSelectionColor(Estilo.VALIDADA_FONDO);
            setSelectedTextColor(Estilo.TEXTO);
        } else {
            setFocusable(false);
            setHighlighter(null);
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /** Ancho (en píxeles) al que se acomoda el texto. Con 0 usa el ancho natural. */
    public void setAnchoAjuste(int ancho) {
        this.anchoAjuste = ancho;
        revalidate();
    }

    public int getAnchoAjuste() {
        return anchoAjuste;
    }

    /**
     * El texto toma el ancho del panel donde está y se acomoda solo cuando la ventana
     * cambia de tamaño (para textos dentro de pantallas que se estiran). Úsalo dentro de
     * un BorderLayout o un GridBagLayout que lo estire a lo ancho.
     */
    public void setAjusteAutomatico(boolean automatico) {
        this.ajusteAutomatico = automatico;
        if (automatico && anchoAjuste <= 0) {
            anchoAjuste = ANCHO_MINIMO;
        }
        observarContenedor();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        observarContenedor();
    }

    @Override
    public void removeNotify() {
        if (contenedorObservado != null) {
            contenedorObservado.removeComponentListener(alCambiarTamano);
            contenedorObservado = null;
        }
        super.removeNotify();
    }

    private void observarContenedor() {
        if (!ajusteAutomatico || getParent() == null || getParent() == contenedorObservado) {
            return;
        }
        if (contenedorObservado != null) {
            contenedorObservado.removeComponentListener(alCambiarTamano);
        }
        contenedorObservado = getParent();
        contenedorObservado.addComponentListener(alCambiarTamano);
        ajustarAlContenedor();
    }

    private void ajustarAlContenedor() {
        if (contenedorObservado == null) {
            return;
        }
        Insets in = contenedorObservado.getInsets();
        int ancho = contenedorObservado.getWidth() - in.left - in.right;
        if (ancho > 0 && ancho != anchoAjuste) {
            anchoAjuste = ancho;
            revalidate();
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (anchoAjuste <= 0) {
            return super.getPreferredSize();
        }
        Insets in = getInsets();
        View vista = getUI().getRootView(this);
        vista.setSize(Math.max(1, anchoAjuste - in.left - in.right), Integer.MAX_VALUE);
        int alto = (int) Math.ceil(vista.getPreferredSpan(View.Y_AXIS));
        // En automático pide poco ancho: así nunca obliga a la pantalla a ser más ancha
        return new Dimension(ajusteAutomatico ? ANCHO_MINIMO : anchoAjuste, alto + in.top + in.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
        if (ajusteAutomatico) {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
        return anchoAjuste > 0 ? getPreferredSize() : super.getMaximumSize();
    }
}
