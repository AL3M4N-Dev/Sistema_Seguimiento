package vista.comun;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * Etiqueta tipo "pastilla" para estados y categorías (Pendiente, Prácticas, Entregada…).
 *
 *   new Chip("Prácticas", Estilo.coloresPrograma("practicas"));
 *   Chip.deEstado("Pendiente");   // toma los colores de Estilo.coloresEstado
 */
public class Chip extends JLabel {

    private Color fondo;

    public Chip(String texto, Color fondo, Color colorTexto) {
        super(texto);
        setFont(Estilo.fuente(Font.BOLD, 12));
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setOpaque(false);
        setColores(fondo, colorTexto);
    }

    public Chip(String texto, Color[] fondoYTexto) {
        this(texto, fondoYTexto[0], fondoYTexto[1]);
    }

    /** Chip con los colores del estado (o gris si no es un estado conocido). */
    public static Chip deEstado(String estado) {
        Color[] c = Estilo.coloresEstado(estado);
        return c == null ? new Chip(estado, Estilo.NEUTRO_FONDO, Estilo.NEUTRO_TEXTO) : new Chip(estado, c);
    }

    public final void setColores(Color fondo, Color colorTexto) {
        this.fondo = fondo;
        setForeground(colorTexto);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
