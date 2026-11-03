package vista.comun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.plaf.basic.BasicScrollBarUI;

/**
 * Barra de desplazamiento delgada y redondeada (sin flechas), igual en todas las pantallas.
 * Estilo.scroll(...) ya la usa; para otro JScrollPane: BarraDesplazamiento.aplicar(scroll).
 */
public class BarraDesplazamiento extends BasicScrollBarUI {

    private static final Color PULGAR = new Color(0xCDD1DC);
    private static final Color PULGAR_ENCIMA = new Color(0xB2B7C6);
    private static final Color PULGAR_ARRASTRANDO = new Color(0x9AA0B4);

    public static void aplicar(JScrollPane scroll) {
        scroll.getVerticalScrollBar().setUI(new BarraDesplazamiento());
        scroll.getHorizontalScrollBar().setUI(new BarraDesplazamiento());
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 12));
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    protected JButton createDecreaseButton(int orientacion) {
        return sinFlecha();
    }

    @Override
    protected JButton createIncreaseButton(int orientacion) {
        return sinFlecha();
    }

    private static JButton sinFlecha() {
        JButton b = new JButton();
        Dimension cero = new Dimension(0, 0);
        b.setPreferredSize(cero);
        b.setMinimumSize(cero);
        b.setMaximumSize(cero);
        b.setFocusable(false);
        return b;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(12, 36);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        // Se pinta del color del contenido; si el contenido es transparente, se deja ver el fondo
        if (c.getParent() instanceof JScrollPane) {
            JViewport vista = ((JScrollPane) c.getParent()).getViewport();
            if (!vista.isOpaque()) {
                return;
            }
            g.setColor(vista.getBackground());
        } else {
            g.setColor(Estilo.SUPERFICIE);
        }
        g.fillRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(isDragging ? PULGAR_ARRASTRANDO : isThumbRollover() ? PULGAR_ENCIMA : PULGAR);
        int ancho = r.width - 6;
        int alto = r.height - 6;
        int arco = Math.min(ancho, alto);
        g2.fillRoundRect(r.x + 3, r.y + 3, ancho, alto, arco, arco);
        g2.dispose();
    }
}
