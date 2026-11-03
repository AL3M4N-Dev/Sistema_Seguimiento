package vista.comun;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/** Pinta la celda de estado: amarillo pendiente, verde aprobado, rojo rechazado. */
public class EstadoRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean conFoco, int fila, int columna) {
        JLabel celda = (JLabel) super.getTableCellRendererComponent(
                tabla, valor, seleccionada, conFoco, fila, columna);
        celda.setHorizontalAlignment(SwingConstants.CENTER);
        celda.setFont(celda.getFont().deriveFont(Font.BOLD));
        if (!seleccionada) {
            Color[] colores = Estilo.coloresEstado(valor == null ? "" : valor.toString());
            celda.setBackground(colores == null ? tabla.getBackground() : colores[0]);
            celda.setForeground(colores == null ? tabla.getForeground() : colores[1]);
        }
        return celda;
    }
}
