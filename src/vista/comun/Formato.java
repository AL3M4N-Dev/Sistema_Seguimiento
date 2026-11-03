package vista.comun;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Formatos de fecha, hora y horas para mostrar en pantalla. */
public final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private Formato() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }

    public static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : FECHA_HORA.format(fecha);
    }

    public static String horario(LocalTime inicio, LocalTime fin) {
        if (inicio == null || fin == null) {
            return "";
        }
        return HORA.format(inicio) + "–" + HORA.format(fin);
    }

    /** 480.00 → "480", 3.50 → "3.5" */
    public static String horas(BigDecimal horas) {
        if (horas == null || horas.signum() == 0) {
            return "0";
        }
        return horas.stripTrailingZeros().toPlainString();
    }

    /** Porcentaje de avance, de 0 a 100. */
    public static int porcentaje(BigDecimal aprobadas, int requeridas) {
        if (aprobadas == null || requeridas <= 0) {
            return 0;
        }
        int p = aprobadas.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requeridas), 0, RoundingMode.DOWN)
                .intValue();
        return Math.min(100, Math.max(0, p));
    }
}
