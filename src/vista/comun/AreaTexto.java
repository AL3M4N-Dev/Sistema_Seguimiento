package vista.comun;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Campo de texto de varias líneas con el diseño base: caja redondeada, texto de ayuda,
 * contador de caracteres y mensaje de error en rojo.
 *
 *   AreaTexto comentario = new AreaTexto(4, 500);
 *   comentario.setPlaceholder("Ej. Falta la foto de la actividad");
 *   if (comentario.getTexto().isBlank()) comentario.setError("Escribe un comentario.");
 *
 * Para solo mostrar un texto largo (sin editar): AreaTexto.soloLectura(texto, 4).
 */
public class AreaTexto extends JPanel {

    private static final int RADIO = 10;

    private final JTextArea area;
    private final Caja caja = new Caja();
    private final JPanel filaAyuda = Estilo.panel(new BorderLayout(12, 0));
    private final JLabel ayuda = Estilo.etiqueta(" ", Estilo.fuente(Font.PLAIN, 12), Estilo.TEXTO_SECUNDARIO);
    private final JLabel contador = Estilo.etiqueta("", Estilo.fuente(Font.PLAIN, 12), Estilo.TEXTO_SECUNDARIO);
    private final int maximo;
    private final boolean editable;
    private String placeholder;
    private String textoAyuda = " ";
    private boolean conError;

    /**
     * @param filas  alto aproximado en renglones
     * @param maximo máximo de caracteres (0 = sin límite ni contador)
     */
    public AreaTexto(int filas, int maximo) {
        this(filas, maximo, true);
    }

    /** Caja gris de solo lectura para mostrar textos largos (por ejemplo, el reporte del alumno). */
    public static AreaTexto soloLectura(String texto, int filas) {
        AreaTexto a = new AreaTexto(filas, 0, false);
        a.setTexto(texto == null || texto.isBlank() ? "(Sin texto)" : texto);
        a.area.setCaretPosition(0);
        return a;
    }

    private AreaTexto(int filas, int maximo, boolean editable) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        this.maximo = maximo;
        this.editable = editable;

        area = new JTextArea(filas, 20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (placeholder != null && getDocument().getLength() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(getFont());
                    g2.setColor(Estilo.TEXTO_DESHABILITADO);
                    Insets in = getInsets();
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(placeholder, in.left, in.top + fm.getAscent());
                    g2.dispose();
                }
            }
        };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(editable);
        area.setFont(Estilo.fuente(Font.PLAIN, 14));
        area.setForeground(Estilo.TEXTO);
        area.setBackground(editable ? Estilo.SUPERFICIE : Estilo.FONDO);
        area.setCaretColor(Estilo.TEXTO);
        area.setSelectionColor(Estilo.VALIDADA_FONDO);
        area.setSelectedTextColor(Estilo.TEXTO);
        area.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        // Tab pasa al siguiente campo en lugar de escribir un tabulador
        area.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        area.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        if (maximo > 0) {
            ((AbstractDocument) area.getDocument()).setDocumentFilter(new Limite(maximo));
        }
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                alCambiar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                alCambiar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                alCambiar();
            }
        });
        area.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                caja.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                caja.repaint();
            }
        });

        JScrollPane scroll = new JScrollPane(area, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(area.getBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        BarraDesplazamiento.aplicar(scroll);
        caja.add(scroll, BorderLayout.CENTER);

        filaAyuda.add(ayuda, BorderLayout.CENTER);
        filaAyuda.add(contador, BorderLayout.EAST);
        filaAyuda.setVisible(editable);

        add(caja, BorderLayout.CENTER);
        add(filaAyuda, BorderLayout.SOUTH);
        actualizarContador();
    }

    public String getTexto() {
        return area.getText();
    }

    public void setTexto(String texto) {
        area.setText(texto);
    }

    /** Texto gris que se ve mientras el campo está vacío. */
    public void setPlaceholder(String texto) {
        this.placeholder = texto;
        area.repaint();
    }

    /** Texto de ayuda pequeño debajo del campo. */
    public void setAyuda(String texto) {
        this.textoAyuda = texto == null || texto.isEmpty() ? " " : texto;
        if (!conError) {
            ayuda.setText(textoAyuda);
        }
    }

    /** Marca el campo en rojo con un mensaje (null lo quita). Se quita solo cuando el usuario escribe. */
    public void setError(String mensaje) {
        conError = mensaje != null;
        ayuda.setText(conError ? mensaje : textoAyuda);
        ayuda.setForeground(conError ? Estilo.PELIGRO : Estilo.TEXTO_SECUNDARIO);
        ayuda.setFont(Estilo.fuente(conError ? Font.BOLD : Font.PLAIN, 12));
        area.getAccessibleContext().setAccessibleDescription(mensaje);
        caja.repaint();
    }

    public boolean tieneError() {
        return conError;
    }

    /** Nombre que leen los lectores de pantalla. */
    public void setNombreAccesible(String nombre) {
        area.getAccessibleContext().setAccessibleName(nombre);
    }

    /** El JTextArea de adentro, por si necesitas algo más (atajos de teclado, etc.). */
    public JTextArea getAreaTexto() {
        return area;
    }

    @Override
    public boolean requestFocusInWindow() {
        return area.requestFocusInWindow();
    }

    @Override
    public void requestFocus() {
        area.requestFocus();
    }

    private void alCambiar() {
        actualizarContador();
        if (conError) {
            setError(null);
        }
        area.repaint();
    }

    private void actualizarContador() {
        if (maximo <= 0) {
            contador.setText("");
            return;
        }
        int usados = area.getDocument().getLength();
        contador.setText(usados + " / " + maximo);
        contador.setForeground(usados >= maximo * 0.9 ? Estilo.PENDIENTE_TEXTO : Estilo.TEXTO_SECUNDARIO);
    }

    /** Caja redondeada: el borde se pinta índigo con el foco y rojo si hay error. */
    private class Caja extends JPanel {
        Caja() {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            boolean foco = area.isFocusOwner();
            g2.setColor(area.getBackground());
            g2.fillRoundRect(0, 0, w - 1, h - 1, RADIO, RADIO);
            Color borde = conError ? Estilo.PELIGRO
                    : foco ? (editable ? Estilo.PRIMARIO : Estilo.PRIMARIO_BORDE)
                    : editable ? Estilo.BORDE_FUERTE : Estilo.BORDE;
            g2.setColor(borde);
            g2.setStroke(new BasicStroke(foco || conError ? 2f : 1f));
            g2.drawRoundRect(1, 1, w - 3, h - 3, RADIO, RADIO);
            g2.dispose();
        }
    }

    /** No deja escribir más de 'maximo' caracteres (también al pegar). */
    private static class Limite extends DocumentFilter {
        private final int maximo;

        Limite(int maximo) {
            this.maximo = maximo;
        }

        @Override
        public void insertString(FilterBypass fb, int pos, String texto, AttributeSet attr) throws BadLocationException {
            replace(fb, pos, 0, texto, attr);
        }

        @Override
        public void replace(FilterBypass fb, int pos, int largo, String texto, AttributeSet attr) throws BadLocationException {
            if (texto == null || texto.isEmpty()) {
                super.replace(fb, pos, largo, texto, attr);
                return;
            }
            int libres = maximo - (fb.getDocument().getLength() - largo);
            if (libres <= 0) {
                super.replace(fb, pos, largo, "", attr);
                return;
            }
            super.replace(fb, pos, largo, texto.length() > libres ? texto.substring(0, libres) : texto, attr);
        }
    }
}
