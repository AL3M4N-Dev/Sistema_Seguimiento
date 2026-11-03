package modelo;

import java.time.LocalDateTime;

/** Una institución (tabla instituciones). En la parte 5 se usan las de prácticas en revisión. */
public class Institucion {

    private int idInstitucion;
    private String nombre;
    private String direccion;
    private String contacto;
    private String tipo;
    private int horasRequeridas;
    private String estado;
    private String creadaPorNombre;
    private LocalDateTime fechaAlta;

    public int getIdInstitucion() {
        return idInstitucion;
    }

    public void setIdInstitucion(int idInstitucion) {
        this.idInstitucion = idInstitucion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public int getHorasRequeridas() {
        return horasRequeridas;
    }

    public void setHorasRequeridas(int horasRequeridas) {
        this.horasRequeridas = horasRequeridas;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** Nombre del alumno que la dio de alta (columna creada_por). */
    public String getCreadaPorNombre() {
        return creadaPorNombre;
    }

    public void setCreadaPorNombre(String creadaPorNombre) {
        this.creadaPorNombre = creadaPorNombre;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }
}
