package modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un alumno inscrito (servicio social o prácticas) con su avance de horas
 * (vista v_progreso_alumno) y el estado de su constancia. Lo usa la pestaña
 * "Alumnos y constancias" del panel del docente.
 */
public class AlumnoActivo {

    private int idAlumno;
    private String nombre;
    private String matricula;
    private String carrera;
    private String correo;
    private String tipoPrograma;
    private int idInscripcion;
    private String estadoInscripcion;
    private String institucion;
    private String docenteAsignado;
    private int horasRequeridas;
    private BigDecimal horasAprobadas;
    private BigDecimal horasPendientes;
    private Integer idSolicitud;
    private EstadoSolicitud estadoConstancia;
    private String rutaPdf;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAprobacion;
    private String comentarioConstancia;

    /** true cuando las horas aprobadas alcanzan las requeridas (100%). */
    public boolean horasCompletas() {
        return horasAprobadas != null
                && horasAprobadas.compareTo(BigDecimal.valueOf(horasRequeridas)) >= 0;
    }

    /** true si ya se le subió su constancia en PDF. */
    public boolean tieneConstancia() {
        return estadoConstancia == EstadoSolicitud.DISPONIBLE;
    }

    /** true si se rechazó su solicitud y todavía no la vuelve a pedir. */
    public boolean solicitudRechazada() {
        return estadoConstancia == EstadoSolicitud.RECHAZADA;
    }

    /**
     * true si ya se le puede subir su constancia: completó sus horas y su solicitud
     * no está rechazada (si se rechazó, primero tiene que volver a pedirla).
     */
    public boolean puedeRecibirConstancia() {
        return horasCompletas() && !solicitudRechazada();
    }

    /** true si el alumno pidió su constancia y está esperando respuesta. */
    public boolean solicitudEnEspera() {
        return estadoConstancia == EstadoSolicitud.SOLICITADA || estadoConstancia == EstadoSolicitud.VALIDADA;
    }

    /** Horas que faltan por aprobar (nunca negativo). */
    public BigDecimal horasFaltantes() {
        BigDecimal aprobadas = horasAprobadas == null ? BigDecimal.ZERO : horasAprobadas;
        BigDecimal faltan = BigDecimal.valueOf(horasRequeridas).subtract(aprobadas);
        return faltan.signum() < 0 ? BigDecimal.ZERO : faltan;
    }

    /** Texto corto del estado de la constancia para la tabla. */
    public String textoConstancia() {
        if (estadoConstancia == null) {
            return horasCompletas() ? "Lista para subir" : "En curso";
        }
        return switch (estadoConstancia) {
            case SOLICITADA, VALIDADA -> "Solicitada";
            case DISPONIBLE -> "Entregada";
            case RECHAZADA -> "Rechazada";
        };
    }

    public int getIdAlumno() {
        return idAlumno;
    }

    public void setIdAlumno(int idAlumno) {
        this.idAlumno = idAlumno;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTipoPrograma() {
        return tipoPrograma;
    }

    public void setTipoPrograma(String tipoPrograma) {
        this.tipoPrograma = tipoPrograma;
    }

    public int getIdInscripcion() {
        return idInscripcion;
    }

    public void setIdInscripcion(int idInscripcion) {
        this.idInscripcion = idInscripcion;
    }

    public String getEstadoInscripcion() {
        return estadoInscripcion;
    }

    public void setEstadoInscripcion(String estadoInscripcion) {
        this.estadoInscripcion = estadoInscripcion;
    }

    public String getInstitucion() {
        return institucion;
    }

    public void setInstitucion(String institucion) {
        this.institucion = institucion;
    }

    public String getDocenteAsignado() {
        return docenteAsignado;
    }

    public void setDocenteAsignado(String docenteAsignado) {
        this.docenteAsignado = docenteAsignado;
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

    public Integer getIdSolicitud() {
        return idSolicitud;
    }

    public void setIdSolicitud(Integer idSolicitud) {
        this.idSolicitud = idSolicitud;
    }

    public EstadoSolicitud getEstadoConstancia() {
        return estadoConstancia;
    }

    public void setEstadoConstancia(EstadoSolicitud estadoConstancia) {
        this.estadoConstancia = estadoConstancia;
    }

    public String getRutaPdf() {
        return rutaPdf;
    }

    public void setRutaPdf(String rutaPdf) {
        this.rutaPdf = rutaPdf;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    /** Motivo que escribió el docente al rechazar la solicitud (o null). */
    public String getComentarioConstancia() {
        return comentarioConstancia;
    }

    public void setComentarioConstancia(String comentarioConstancia) {
        this.comentarioConstancia = comentarioConstancia;
    }

    /** Fecha en que se revisó la constancia: cuando se aprobó o cuando se rechazó. */
    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }
}
