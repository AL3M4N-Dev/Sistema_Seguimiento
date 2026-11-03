package vista.comun;

import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/** Dibuja una celda de tipo Avance como barra de progreso: verde al 100%, azul antes. */
public class AvanceRenderer extends JProgressBar implements TableCellRenderer {

    public AvanceRenderer() {
        super(0, 100);
        setStringPainted(true);
        setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean conFoco, int fila, int columna) {
        Avance a = valor instanceof Avance av ? av : new Avance(null, 0);
        setValue(a.porcentaje());
        setString(a.toString());
        setForeground(a.completo() ? Estilo.VERDE : Estilo.AZUL);
        setBackground(seleccionada ? tabla.getSelectionBackground() : tabla.getBackground());
        return this;
    }
}
