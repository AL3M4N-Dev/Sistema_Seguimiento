package vista.comun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/** Círculo con las iniciales de una persona (por ejemplo "DO" para Daniela Ortiz). */
public class Avatar extends JComponent {

    private final int diametro;
    private String iniciales;
    private Color fondo;
    private Color colorTexto;

    public Avatar(String nombre, int diametro) {
        this.diametro = diametro;
        setNombre(nombre);
        setColores(Estilo.PRIMARIO_SUAVE, Estilo.PRIMARIO_OSCURO);
        setPreferredSize(new Dimension(diametro, diametro));
        setMinimumSize(new Dimension(diametro, diametro));
    }

    /** "Daniela Ortiz" → "DO"; con una sola palabra, sus dos primeras letras. */
    public static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "?";
        }
        String[] partes = nombre.trim().split("\\s+");
        String r = partes.length == 1
                ? partes[0].substring(0, Math.min(2, partes[0].length()))
                : "" + partes[0].charAt(0) + partes[1].charAt(0);
        return r.toUpperCase();
    }

    public final void setNombre(String nombre) {
        this.iniciales = iniciales(nombre);
        getAccessibleContext().setAccessibleName(nombre);
        repaint();
    }

    public final void setColores(Color fondo, Color colorTexto) {
        this.fondo = fondo;
        this.colorTexto = colorTexto;
        repaint();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public javax.accessibility.AccessibleRole getAccessibleRole() {
                    return javax.accessibility.AccessibleRole.ICON;
                }
            };
        }
        return accessibleContext;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int x = (getWidth() - diametro) / 2;
        int y = (getHeight() - diametro) / 2;
        g2.setColor(fondo);
        g2.fillOval(x, y, diametro, diametro);
        g2.setFont(Estilo.fuente(Font.BOLD, Math.max(11, diametro * 0.34f)));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(colorTexto);
        g2.drawString(iniciales, x + (diametro - fm.stringWidth(iniciales)) / 2,
                y + (diametro - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
