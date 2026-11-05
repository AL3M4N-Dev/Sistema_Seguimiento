package vista.comun;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * Barra de avance delgada y redondeada (0 a 100). Verde al llegar a 100.
 *
 *   BarraProgreso b = new BarraProgreso(8);
 *   b.setValor(Formato.porcentaje(horasAprobadas, horasRequeridas));
 */
public class BarraProgreso extends JComponent {

    private int valor;
    private Color pista = new Color(0xE8EAF2);
    private Color relleno = new Color(0x6366F1);
    private Color rellenoCompleto = new Color(0x16A34A);

    public BarraProgreso(int alto) {
        setPreferredSize(new Dimension(80, alto));
        setMinimumSize(new Dimension(20, alto));
    }

    public void setValor(int valor) {
        this.valor = Math.max(0, Math.min(100, valor));
        getAccessibleContext().setAccessibleDescription(this.valor + " por ciento");
        repaint();
    }

    public int getValor() {
        return valor;
    }

    public void setColores(Color pista, Color relleno, Color rellenoCompleto) {
        this.pista = pista;
        this.relleno = relleno;
        this.rellenoCompleto = rellenoCompleto;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int alto = getPreferredSize().height;
        int y = (getHeight() - alto) / 2;
        g2.setColor(pista);
        g2.fillRoundRect(0, y, getWidth(), alto, alto, alto);
        if (valor > 0) {
            int ancho = Math.max(alto, getWidth() * valor / 100);
            g2.setColor(valor >= 100 ? rellenoCompleto : relleno);
            g2.fillRoundRect(0, y, ancho, alto, alto, alto);
        }
        g2.dispose();
    }

    @Override
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return AccessibleRole.PROGRESS_BAR;
                }
            };
        }
        return accessibleContext;
    }
}
