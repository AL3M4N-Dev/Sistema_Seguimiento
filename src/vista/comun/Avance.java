package vista.comun;

import java.math.BigDecimal;

/**
 * Avance de horas para mostrar en una tabla con AvanceRenderer
 * (barra "120 / 480 h · 25%"). Se ordena por porcentaje.
 */
public final class Avance implements Comparable<Avance> {

    private final BigDecimal aprobadas;
    private final int requeridas;

    public Avance(BigDecimal aprobadas, int requeridas) {
        this.aprobadas = aprobadas == null ? BigDecimal.ZERO : aprobadas;
        this.requeridas = requeridas;
    }

    public int porcentaje() {
        return Formato.porcentaje(aprobadas, requeridas);
    }

    public boolean completo() {
        return aprobadas.compareTo(BigDecimal.valueOf(requeridas)) >= 0;
    }

    @Override
    public int compareTo(Avance otro) {
        int c = Integer.compare(porcentaje(), otro.porcentaje());
        return c != 0 ? c : aprobadas.compareTo(otro.aprobadas);
    }

    @Override
    public String toString() {
        return Formato.horas(aprobadas) + " / " + requeridas + " h · " + porcentaje() + "%";
    }
}
