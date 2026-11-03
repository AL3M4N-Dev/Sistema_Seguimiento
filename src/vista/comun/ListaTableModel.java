package vista.comun;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de tabla de solo lectura para una lista de objetos.
 * Ejemplo: new ListaTableModel<>(columnas, (registro, col) -> col == 0 ? registro.getAlumnoNombre() : ...)
 */
public class ListaTableModel<T> extends AbstractTableModel {

    private final String[] columnas;
    private final BiFunction<T, Integer, Object> valorDe;
    private List<T> filas = new ArrayList<>();

    public ListaTableModel(String[] columnas, BiFunction<T, Integer, Object> valorDe) {
        this.columnas = columnas;
        this.valorDe = valorDe;
    }

    public void setFilas(List<T> nuevas) {
        filas = new ArrayList<>(nuevas);
        fireTableDataChanged();
    }

    public T getFila(int indice) {
        return filas.get(indice);
    }

    @Override
    public int getRowCount() {
        return filas.size();
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int columna) {
        return columnas[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return valorDe.apply(filas.get(fila), columna);
    }
}
