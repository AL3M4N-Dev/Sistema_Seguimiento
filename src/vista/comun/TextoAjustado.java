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

    private int anchoAjuste;

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

    @Override
    public Dimension getPreferredSize() {
        if (anchoAjuste <= 0) {
            return super.getPreferredSize();
        }
        Insets in = getInsets();
        View vista = getUI().getRootView(this);
        vista.setSize(Math.max(1, anchoAjuste - in.left - in.right), Integer.MAX_VALUE);
        int alto = (int) Math.ceil(vista.getPreferredSpan(View.Y_AXIS));
        return new Dimension(anchoAjuste, alto + in.top + in.bottom);
    }

    @Override
    public Dimension getMaximumSize() {
        return anchoAjuste > 0 ? getPreferredSize() : super.getMaximumSize();
    }
}
