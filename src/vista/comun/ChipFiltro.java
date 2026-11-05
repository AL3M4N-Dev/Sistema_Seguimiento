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
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;

/**
 * Botón de filtro tipo pastilla (Todos / Servicio social / Prácticas).
 * El seleccionado se pinta en índigo. Agrúpalos con ChipFiltro.grupo(...).
 */
public class ChipFiltro extends JToggleButton {

    public ChipFiltro(String texto) {
        super(texto);
        setFont(Estilo.fuente(Font.BOLD, 13));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /** Agrupa los chips para que solo uno quede seleccionado; selecciona el primero. */
    public static ButtonGroup grupo(ChipFiltro... chips) {
        ButtonGroup g = new ButtonGroup();
        for (ChipFiltro c : chips) {
            g.add(c);
        }
        if (chips.length > 0) {
            chips[0].setSelected(true);
        }
        return g;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(getText()) + 26, 36);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean sel = isSelected();
        g2.setColor(sel ? Estilo.PRIMARIO : getModel().isRollover() ? new Color(0xF4F5FA) : Color.WHITE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 9, 9);
        if (!sel) {
            g2.setColor(Estilo.BORDE_FUERTE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 9, 9);
        }
        if (isFocusOwner()) {
            g2.setColor(Estilo.PRIMARIO_BORDE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, 9, 9);
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(sel ? Color.WHITE : Estilo.TEXTO);
        g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
