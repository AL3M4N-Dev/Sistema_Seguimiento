package vista.comun;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;
import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Caja de búsqueda redondeada con lupa y texto de ayuda.
 *
 *   CampoBusqueda buscar = new CampoBusqueda("Buscar alumno o matrícula");
 *   buscar.alCambiar(texto -> filtrar(texto));
 */
public class CampoBusqueda extends JTextField {

    private final String ayuda;

    public CampoBusqueda(String ayuda) {
        this.ayuda = ayuda;
        setFont(Estilo.fuente(Font.PLAIN, 14));
        setForeground(Estilo.TEXTO);
        setBackground(Estilo.SUPERFICIE);
        setOpaque(false);
        setBorder(new BordeRedondo());
        setPreferredSize(new Dimension(240, 42));
        getAccessibleContext().setAccessibleName(ayuda);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    /** Llama a la acción cada vez que cambia el texto. */
    public void alCambiar(Consumer<String> accion) {
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accion.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accion.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accion.accept(getText());
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
        // Lupa
        g2.setColor(Estilo.TEXTO_SECUNDARIO);
        g2.setStroke(new BasicStroke(1.8f));
        int cy = getHeight() / 2;
        g2.drawOval(13, cy - 7, 11, 11);
        g2.drawLine(22, cy + 2, 26, cy + 6);
        g2.dispose();
        super.paintComponent(g);
        if (getText().isEmpty() && !isFocusOwner()) {
            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g3.setColor(Estilo.TEXTO_DESHABILITADO);
            g3.setFont(getFont());
            Insets in = getInsets();
            g3.drawString(ayuda, in.left, (getHeight() - g3.getFontMetrics().getHeight()) / 2 + g3.getFontMetrics().getAscent());
            g3.dispose();
        }
    }

    private class BordeRedondo extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean foco = c.isFocusOwner();
            g2.setColor(foco ? Estilo.PRIMARIO : Estilo.BORDE_FUERTE);
            g2.setStroke(new BasicStroke(foco ? 1.6f : 1f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(8, 34, 8, 12);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(8, 34, 8, 12);
            return insets;
        }
    }

    /** Para que el fondo blanco se vea aunque el panel de atrás sea gris. */
    @Override
    public Color getBackground() {
        return Estilo.SUPERFICIE;
    }
}
