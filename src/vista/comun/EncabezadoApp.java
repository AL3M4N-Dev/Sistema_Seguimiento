package vista.comun;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Barra superior del diseño base: marca, pestañas subrayadas y usuario.
 * Cualquier ventana del equipo puede usarla:
 *
 *   EncabezadoApp enc = new EncabezadoApp("Mtra. Laura Gómez · Docente");
 *   enc.agregarSeccion("registros", "Registros");
 *   enc.agregarSeccion("alumnos", "Alumnos y constancias");
 *   enc.alCambiarSeccion(clave -> tarjetas.show(contenido, clave));
 *   enc.seleccionar("alumnos");
 */
public class EncabezadoApp extends JPanel {

    private final Map<String, Pestana> pestanas = new LinkedHashMap<>();
    private final JPanel navegacion = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 2, 0));
    private final Boton cerrarSesion = Boton.enlace("Cerrar sesión");
    private Consumer<String> alCambiar = clave -> { };

    public EncabezadoApp(String usuario) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(Estilo.SUPERFICIE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Estilo.BORDE),
                BorderFactory.createEmptyBorder(0, 24, 0, 24)));

        JLabel marca = Estilo.etiqueta("Seguimiento", Estilo.fuente(Font.BOLD, 17), Estilo.PRIMARIO);
        JPanel izquierda = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel bloqueMarca = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 8, 18));
        bloqueMarca.add(new IconoMarca());
        bloqueMarca.add(marca);
        izquierda.add(bloqueMarca);
        izquierda.add(javax.swing.Box.createHorizontalStrut(22));
        izquierda.add(navegacion);

        // El nombre usa el espacio que sobra: si la ventana es angosta se recorta con "…"
        JLabel nombre = Estilo.etiqueta(usuario, Estilo.fuente(Font.BOLD, 14), Estilo.TEXTO_SECUNDARIO);
        nombre.setHorizontalAlignment(JLabel.RIGHT);
        nombre.setToolTipText(usuario);
        JPanel enlace = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 0, 18));
        enlace.add(cerrarSesion);
        JPanel derecha = Estilo.panel(new BorderLayout(14, 0));
        derecha.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        derecha.add(nombre, BorderLayout.CENTER);
        derecha.add(enlace, BorderLayout.EAST);

        add(izquierda, BorderLayout.WEST);
        add(derecha, BorderLayout.CENTER);
    }

    /** Agrega una pestaña. La clave es la que recibe alCambiarSeccion. */
    public void agregarSeccion(String clave, String texto) {
        Pestana p = new Pestana(texto);
        p.addActionListener(e -> seleccionar(clave));
        pestanas.put(clave, p);
        navegacion.add(p);
        revalidate();
    }

    /** Marca la pestaña como activa y avisa al que escucha. */
    public void seleccionar(String clave) {
        pestanas.forEach((k, p) -> p.setActiva(k.equals(clave)));
        alCambiar.accept(clave);
    }

    /** Muestra un número junto al nombre, por ejemplo "Registros (3)". null o 0 lo quita. */
    public void setContador(String clave, Integer cantidad) {
        Pestana p = pestanas.get(clave);
        if (p != null) {
            p.setText(p.base + (cantidad == null || cantidad == 0 ? "" : " (" + cantidad + ")"));
            revalidate();
        }
    }

    public void alCambiarSeccion(Consumer<String> accion) {
        this.alCambiar = accion;
    }

    public void alCerrarSesion(Runnable accion) {
        cerrarSesion.addActionListener(e -> accion.run());
    }

    /** Pestaña con subrayado índigo cuando está activa. */
    private static class Pestana extends JButton {
        private final String base;
        private boolean activa;

        Pestana(String texto) {
            super(texto);
            this.base = texto;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Estilo.fuente(Font.BOLD, 14));
        }

        void setActiva(boolean activa) {
            this.activa = activa;
            getAccessibleContext().setAccessibleDescription(activa ? "Sección actual" : null);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(getText()) + 28, 62);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(activa || getModel().isRollover() ? Estilo.TEXTO : Estilo.TEXTO_SECUNDARIO);
            g2.drawString(getText(), 14, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            if (activa) {
                g2.setColor(Estilo.PRIMARIO);
                g2.fillRect(0, getHeight() - 3, getWidth(), 3);
            }
            if (isFocusOwner()) {
                g2.setColor(Estilo.PRIMARIO_BORDE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(2, 12, getWidth() - 5, getHeight() - 24, 8, 8);
            }
            g2.dispose();
        }
    }

    /** Birrete dibujado (no necesita archivos de imagen). */
    private static class IconoMarca extends JComponent {
        IconoMarca() {
            setPreferredSize(new Dimension(24, 24));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Estilo.PRIMARIO);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D birrete = new Path2D.Float();
            birrete.moveTo(3, 9);
            birrete.lineTo(12, 4);
            birrete.lineTo(21, 9);
            birrete.lineTo(12, 14);
            birrete.closePath();
            g2.draw(birrete);
            Path2D base = new Path2D.Float();
            base.moveTo(7, 11.5);
            base.lineTo(7, 16);
            base.curveTo(7, 17.5, 9.2, 19, 12, 19);
            base.curveTo(14.8, 19, 17, 17.5, 17, 16);
            base.lineTo(17, 11.5);
            g2.draw(base);
            g2.dispose();
        }
    }
}
