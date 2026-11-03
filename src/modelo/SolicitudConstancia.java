package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una solicitud de constancia (tabla solicitudes_constancia), con las horas
 * aprobadas del alumno tomadas de la vista v_progreso_alumno.
 */
public class SolicitudConstancia {

    private int idSolicitud;
    private int idInscripcion;
    private String alumnoNombre;
    private String matricula;
    private String tipoPrograma;
    private String institucionNombre;
    private LocalDateTime fechaSolicitud;
    private EstadoSolicitud estado;
    private BigDecimal horasAprobadas;
    private int horasRequeridas;
    private String comentario;
    private String rutaPdf;
    private LocalDateTime fechaValidacion;

    /** true cuando las horas aprobadas alcanzan las requeridas. */
    public boolean horasCompletas() {
        return horasAprobadas != null
                && horasAprobadas.compareTo(BigDecimal.valueOf(horasRequeridas)) >= 0;
    }

    public int getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(int idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public int getIdInscripcion() {
        return idInscripcion;
    }

    public void setIdInscripcion(int idInscripcion) {
        this.idInscripcion = idInscripcion;
    }

    public String getAlumnoNombre() {
        return alumnoNombre;
    }

    public void setAlumnoNombre(String alumnoNombre) {
        this.alumnoNombre = alumnoNombre;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getTipoPrograma() {
        return tipoPrograma;
    }

    public void setTipoPrograma(String tipoPrograma) {
        this.tipoPrograma = tipoPrograma;
    }

    public String getInstitucionNombre() {
        return institucionNombre;
    }

    public void setInstitucionNombre(String institucionNombre) {
        this.institucionNombre = institucionNombre;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitud estado) {
        this.estado = estado;
    }

    public BigDecimal getHorasAprobadas() {
        return horasAprobadas;
    }

    public void setHorasAprobadas(BigDecimal horasAprobadas) {
        this.horasAprobadas = horasAprobadas;
    }

    public int getHorasRequeridas() {
        return horasRequeridas;
    }

    public void setHorasRequeridas(int horasRequeridas) {
        this.horasRequeridas = horasRequeridas;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public LocalDateTime getFechaValidacion() {
        return fechaValidacion;
    }

    public void setFechaValidacion(LocalDateTime fechaValidacion) {
        this.fechaValidacion = fechaValidacion;
    }
}
