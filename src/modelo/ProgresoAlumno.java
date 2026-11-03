package modelo;

import java.math.BigDecimal;

/** Avance de horas de un alumno (vista v_progreso_alumno). */
public class ProgresoAlumno {

    private int idInscripcion;
    private String institucion;
    private int horasRequeridas;
    private BigDecimal horasAprobadas;
    private BigDecimal horasPendientes;
    private BigDecimal porcentaje;

    /** true cuando las horas aprobadas alcanzan las requeridas (100%). */
    public boolean completo() {
        return horasAprobadas != null
                && horasAprobadas.compareTo(BigDecimal.valueOf(horasRequeridas)) >= 0;
    }

    /** Horas que faltan por aprobar (nunca negativo). */
    public BigDecimal horasFaltantes() {
        BigDecimal aprobadas = horasAprobadas == null ? BigDecimal.ZERO : horasAprobadas;
        BigDecimal faltan = BigDecimal.valueOf(horasRequeridas).subtract(aprobadas);
        return faltan.signum() < 0 ? BigDecimal.ZERO : faltan;
    }

    public int getIdInscripcion() {
        return idInscripcion;
    }

    public void setIdInscripcion(int idInscripcion) {
        this.idInscripcion = idInscripcion;
    }

    public String getInstitucion() {
        return institucion;
    }

    public void setInstitucion(String institucion) {
        this.institucion = institucion;
    }

    public int getHorasRequeridas() {
        return horasRequeridas;
    }

    public void setHorasRequeridas(int horasRequeridas) {
        this.horasRequeridas = horasRequeridas;
    }

    public BigDecimal getHorasAprobadas() {
        return horasAprobadas;
    }

    public void setHorasAprobadas(BigDecimal horasAprobadas) {
        this.horasAprobadas = horasAprobadas;
    }

    public BigDecimal getHorasPendientes() {
        return horasPendientes;
    }

    public void setHorasPendientes(BigDecimal horasPendientes) {
        this.horasPendientes = horasPendientes;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public void setPorcentaje(BigDecimal porcentaje) {
        this.porcentaje = porcentaje;
    }
}
