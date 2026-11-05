package vista.comun;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

/**
 * Tarjeta del diseño base: fondo blanco, borde suave y esquinas redondeadas.
 * Lo que pongas adentro debe ser transparente: usa Estilo.panel(...) para sub-paneles.
 *
 *   Tarjeta t = new Tarjeta();                    // BorderLayout, blanca
 *   t.add(Estilo.subtitulo("Mi sección"), BorderLayout.NORTH);
 */
public class Tarjeta extends JPanel {

    private Color fondo;
    private Color borde;
    private final int radio;

    public Tarjeta() {
        this(new BorderLayout(0, 10), Estilo.SUPERFICIE, Estilo.BORDE, 14);
    }

    public Tarjeta(LayoutManager layout) {
        this(layout, Estilo.SUPERFICIE, Estilo.BORDE, 14);
    }

    public Tarjeta(LayoutManager layout, Color fondo, Color borde, int radio) {
        super(layout);
        this.fondo = fondo;
        this.borde = borde;
        this.radio = radio;
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    /** Cambia los colores (por ejemplo, verde suave cuando algo está completo). */
    public void setColores(Color fondo, Color borde) {
        this.fondo = fondo;
        this.borde = borde;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
        if (borde != null) {
            g2.setColor(borde);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
