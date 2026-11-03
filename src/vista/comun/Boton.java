package vista.comun;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;

/**
 * Botón del diseño base: esquinas redondeadas, mismo aspecto en cualquier computadora.
 * Se crea con Estilo.botonPrincipal / botonExito / botonSecundario / botonPeligro,
 * o con Boton.relleno / Boton.contorno / Boton.enlace.
 */
public class Boton extends JButton {

    private static final int RADIO = 10;

    private final Color relleno;   // null = botón con borde
    private final Color acento;    // color del texto (y del borde si no tiene relleno)
    private final boolean enlace;

    public static Boton relleno(String texto, Color color) {
        return new Boton(texto, color, color, false);
    }

    public static Boton contorno(String texto, Color color) {
        return new Boton(texto, null, color, false);
    }

    public static Boton enlace(String texto) {
        return new Boton(texto, null, Estilo.PRIMARIO, true);
    }

    private Boton(String texto, Color relleno, Color acento, boolean enlace) {
        super(texto);
        this.relleno = relleno;
        this.acento = acento;
        this.enlace = enlace;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Estilo.fuente(Font.BOLD, enlace ? 13 : 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(enlace ? BorderFactory.createEmptyBorder(4, 2, 4, 2) : BorderFactory.createEmptyBorder(10, 18, 10, 18));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        Insets in = getInsets();
        int ancho = fm.stringWidth(getText()) + in.left + in.right;
        int alto = fm.getHeight() + in.top + in.bottom;
        return new Dimension(ancho, enlace ? alto : Math.max(40, alto));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        ButtonModel m = getModel();
        boolean activo = isEnabled();
        boolean resaltado = activo && (m.isRollover() || m.isPressed());
        Color colorTexto;

        if (enlace) {
            colorTexto = !activo ? Estilo.TEXTO_DESHABILITADO : resaltado ? Estilo.PRIMARIO_OSCURO : acento;
        } else if (relleno != null) {
            g2.setColor(!activo ? Estilo.DESHABILITADO_FONDO : m.isPressed() ? oscurecer(relleno, 0.22f)
                    : resaltado ? oscurecer(relleno, 0.12f) : relleno);
            g2.fillRoundRect(0, 0, w - 1, h - 1, RADIO, RADIO);
            colorTexto = activo ? Color.WHITE : Estilo.TEXTO_DESHABILITADO;
        } else {
            g2.setColor(!activo ? new Color(0xF6F7FA) : resaltado ? mezclar(acento, Color.WHITE, 0.07f) : Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, RADIO, RADIO);
            g2.setColor(!activo ? Estilo.BORDE : acento.equals(Estilo.TEXTO) ? Estilo.BORDE_FUERTE : acento);
            g2.drawRoundRect(0, 0, w - 1, h - 1, RADIO, RADIO);
            colorTexto = activo ? acento : Estilo.TEXTO_DESHABILITADO;
        }

        if (isFocusOwner() && activo) {
            g2.setColor(Estilo.PRIMARIO_BORDE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, RADIO, RADIO);
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String texto = getText();
        int x = (w - fm.stringWidth(texto)) / 2;
        int y = (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(colorTexto);
        g2.drawString(texto, x, y);
        if (enlace && resaltado) {
            g2.drawLine(x, y + 2, x + fm.stringWidth(texto), y + 2);
        }
        g2.dispose();
    }

    private static Color oscurecer(Color c, float cuanto) {
        return mezclar(c, Color.BLACK, cuanto);
    }

    private static Color mezclar(Color a, Color b, float cuantoDeB) {
        float k = 1f - cuantoDeB;
        return new Color(
                Math.round(a.getRed() * k + b.getRed() * cuantoDeB),
                Math.round(a.getGreen() * k + b.getGreen() * cuantoDeB),
                Math.round(a.getBlue() * k + b.getBlue() * cuantoDeB));
    }
}
