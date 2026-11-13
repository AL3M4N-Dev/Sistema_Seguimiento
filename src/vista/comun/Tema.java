package vista.comun;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;

/**
 * Piezas visuales de las pantallas de login, registro y recuperación que
 * Estilo no trae (campos de texto, listas, casillas, tarjeta y barra superior).
 * Los colores, la letra, los textos y los botones salen de Estilo, así que si
 * el equipo cambia Estilo, estas pantallas cambian con él.
 */
public final class Tema {

    private Tema() { }

    private static final int ALTO_CAMPO = 38;
    private static final int ALTO_BOTON = 42;

    // =====================================================================
    //  Textos (usan los de Estilo)
    // =====================================================================
    public static JLabel titulo(String texto) {
        JLabel l = Estilo.titulo(texto);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Texto gris pequeño debajo del título. */
    public static JLabel subtitulo(String texto) {
        JLabel l = Estilo.secundario(texto);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Encabezado de sección (por ejemplo "1. Solicita tu código"). */
    public static JLabel seccion(String texto) {
        JLabel l = Estilo.subtitulo(texto);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Etiqueta pequeña que va arriba de cada campo. */
    public static JLabel etiqueta(String texto) {
        return Estilo.etiqueta(texto, Estilo.fuente(Font.BOLD, 13), Estilo.NEUTRO_TEXTO);
    }

    // =====================================================================
    //  Campos de texto, listas y casillas
    // =====================================================================
    public static void campo(final JTextComponent c) {
        final BordeCampo borde = new BordeCampo(Estilo.BORDE_FUERTE);
        c.setFont(Estilo.fuente(Font.PLAIN, 14));
        c.setForeground(Estilo.TEXTO);
        c.setBackground(Estilo.SUPERFICIE);
        c.setCaretColor(Estilo.PRIMARIO);
        c.setDisabledTextColor(Estilo.TEXTO_DESHABILITADO);
        c.setBorder(borde);
        c.setPreferredSize(new Dimension(240, ALTO_CAMPO));
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                borde.setColor(Estilo.PRIMARIO);
                c.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                borde.setColor(Estilo.BORDE_FUERTE);
                c.repaint();
            }
        });
    }

    public static void combo(JComboBox<?> c) {
        c.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                BasicArrowButton b = new BasicArrowButton(SwingConstants.SOUTH,
                        Estilo.SUPERFICIE, Estilo.SUPERFICIE, Estilo.TEXTO_SECUNDARIO, Estilo.SUPERFICIE);
                b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
                return b;
            }
        });
        c.setFont(Estilo.fuente(Font.PLAIN, 14));
        c.setForeground(Estilo.TEXTO);
        c.setBackground(Estilo.SUPERFICIE);
        c.setOpaque(true);
        c.setBorder(new BordeCampo(Estilo.BORDE_FUERTE));
        c.setPreferredSize(new Dimension(160, ALTO_CAMPO));
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void casilla(JCheckBox c) {
        c.setFont(Estilo.fuente(Font.PLAIN, 13));
        c.setForeground(Estilo.TEXTO_SECUNDARIO);
        c.setOpaque(false);
        c.setFocusPainted(false);
        c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    // =====================================================================
    //  Botones: se crean con Estilo (botonPrincipal, botonSecundario, enlace).
    //  Este método solo los deja a lo ancho de la tarjeta.
    // =====================================================================
    public static <T extends JButton> T anchoCompleto(T b) {
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setPreferredSize(new Dimension(b.getPreferredSize().width, ALTO_BOTON));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTO_BOTON));
        return b;
    }

    // =====================================================================
    //  Contenedores y utilidades de diseño
    // =====================================================================

    /** Etiqueta pequeña arriba y el campo debajo. */
    public static JPanel campoEtiquetado(String texto, JComponent campo) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setOpaque(false);
        p.add(etiqueta(texto), BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Dos componentes lado a lado, con el mismo ancho. */
    public static JPanel dos(JComponent a, JComponent b) {
        JPanel p = new JPanel(new GridLayout(1, 2, 14, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    /** Agrega espacio debajo del componente (se oculta junto con él). */
    public static <T extends JComponent> T conEspacio(T c, int px) {
        c.setBorder(BorderFactory.createEmptyBorder(0, 0, px, 0));
        return c;
    }

    public static JSeparator separador() {
        JSeparator s = new JSeparator();
        s.setForeground(Estilo.BORDE);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        return s;
    }

    /** Barra superior blanca con el logo "Seguimiento". */
    public static JPanel barraSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 14));
        barra.setBackground(Estilo.SUPERFICIE);
        barra.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BORDE));
        JLabel logo = new JLabel("Seguimiento", new IconoLogo(26), SwingConstants.LEFT);
        logo.setIconTextGap(9);
        logo.setFont(Estilo.fuente(Font.BOLD, 19));
        logo.setForeground(Estilo.PRIMARIO);
        barra.add(logo);
        return barra;
    }

    // =====================================================================
    //  Clases internas
    // =====================================================================

    /** Panel blanco con esquinas redondeadas y borde fino, de ancho fijo. */
    public static class Tarjeta extends JPanel {
        private final int ancho;

        public Tarjeta(int ancho) {
            this.ancho = ancho;
            setOpaque(false);
        }

        @Override public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(ancho, d.height);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D forma = new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(Estilo.SUPERFICIE);
            g2.fill(forma);
            g2.setColor(Estilo.BORDE);
            g2.draw(forma);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Borde redondeado de los campos; cambia de color cuando el campo tiene el foco. */
    private static class BordeCampo extends AbstractBorder {
        private Color color;

        BordeCampo(Color color) {
            this.color = color;
        }

        void setColor(Color color) {
            this.color = color;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.3f));
            g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, w - 1.5f, h - 1.5f, 10, 10));
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(6, 12, 6, 12);
        }

        @Override public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(6, 12, 6, 12);
            return insets;
        }
    }

    /** Logo: birrete de graduación dibujado con líneas, en el color primario. */
    private static class IconoLogo implements Icon {
        private final int s;

        IconoLogo(int tamano) {
            this.s = tamano;
        }

        @Override public int getIconWidth()  { return s; }
        @Override public int getIconHeight() { return s; }

        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(Estilo.PRIMARIO);
            g2.setStroke(new BasicStroke(s / 13f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Path2D tapa = new Path2D.Float();
            tapa.moveTo(s * 0.50, s * 0.16);
            tapa.lineTo(s * 0.96, s * 0.40);
            tapa.lineTo(s * 0.50, s * 0.64);
            tapa.lineTo(s * 0.04, s * 0.40);
            tapa.closePath();
            g2.draw(tapa);

            Path2D base = new Path2D.Float();
            base.moveTo(s * 0.22, s * 0.52);
            base.lineTo(s * 0.22, s * 0.72);
            base.quadTo(s * 0.50, s * 0.94, s * 0.78, s * 0.72);
            base.lineTo(s * 0.78, s * 0.52);
            g2.draw(base);

            g2.drawLine((int) (s * 0.96), (int) (s * 0.40), (int) (s * 0.96), (int) (s * 0.70));
            g2.dispose();
        }
    }
}