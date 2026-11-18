/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
/**
 *
 * @author luisa
 */
public class ValidarHoras {
    /** Devuelve el mensaje de error, o null si todo está bien. */
    public static String validar(LocalDate fecha, LocalTime inicio, LocalTime fin, String reporte) {
        if (fecha == null) return "Selecciona la fecha del turno.";
        if (inicio == null || fin == null) return "Indica la hora de inicio y la de fin.";
        if (reporte == null || reporte.isBlank()) return "Escribe el reporte de actividades.";
        if (fecha.isAfter(LocalDate.now())) return "La fecha no puede ser futura.";
        if (!fin.isAfter(inicio)) return "La hora de fin debe ser posterior a la de inicio.";
        return null;
    }

    /** Horas del turno con 2 decimales (ej. 09:00 a 13:30 = 4.50). */
    public static BigDecimal calcularHoras(LocalTime inicio, LocalTime fin) {
        long minutos = Duration.between(inicio, fin).toMinutes();
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
