package vista.comun;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** Dibuja la celda de estado como pastilla de color (Pendiente, Aprobado, Rechazado…). */
public class EstadoRenderer extends DefaultTableCellRenderer {

    private Color fondoFila;
    private Color fondoPastilla;
    private Color textoPastilla;

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean conFoco, int fila, int columna) {
        super.getTableCellRendererComponent(tabla, valor, seleccionada, false, fila, columna);
        setFont(Estilo.fuente(Font.BOLD, 12));
        String texto = valor == null ? "" : valor.toString();
        Color[] colores = Estilo.coloresEstado(texto);
        fondoPastilla = colores == null ? Estilo.NEUTRO_FONDO : colores[0];
        textoPastilla = colores == null ? Estilo.NEUTRO_TEXTO : colores[1];
        fondoFila = seleccionada ? tabla.getSelectionBackground() : tabla.getBackground();
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(fondoFila);
        g2.fillRect(0, 0, getWidth(), getHeight());
        String texto = getText();
        if (!texto.isEmpty()) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int ancho = fm.stringWidth(texto) + 20;
            int alto = fm.getHeight() + 6;
            int x = 14;
            int y = (getHeight() - alto) / 2;
            g2.setColor(fondoPastilla);
            g2.fillRoundRect(x, y, ancho, alto, alto, alto);
            g2.setColor(textoPastilla);
            g2.drawString(texto, x + 10, y + 3 + fm.getAscent());
        }
        g2.dispose();
    }
}
