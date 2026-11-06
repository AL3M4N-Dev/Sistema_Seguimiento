package modelo;

/** Una institución tal como se muestra en el catálogo del alumno (tabla instituciones). */
public class InstitucionCatalogo {

    private int idInstitucion;
    private String nombre;
    private String direccion;
    private String contacto;
    private String tipo;               // servicio_social | practicas
    private int horasRequeridas;
    private Integer cuposTotales;      // puede ser null
    private Integer cuposDisponibles;  // puede ser null
    private String estado;             // disponible | lleno | en_revision | rechazada

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

    public Integer getCuposTotales() {
        return cuposTotales;
    }

    public void setCuposTotales(Integer cuposTotales) {
        this.cuposTotales = cuposTotales;
    }

    public Integer getCuposDisponibles() {
        return cuposDisponibles;
    }

    public void setCuposDisponibles(Integer cuposDisponibles) {
        this.cuposDisponibles = cuposDisponibles;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /** True si el alumno puede inscribirse: está disponible y le quedan cupos. */
    public boolean puedeInscribirse() {
        return "disponible".equals(estado)
                && cuposDisponibles != null
                && cuposDisponibles > 0;
    }

    /** Texto legible del estado para mostrar en la tabla. */
    public String estadoTexto() {
        if ("disponible".equals(estado)) {
            return "Disponible";
        }
        if ("lleno".equals(estado)) {
            return "Lleno";
        }
        if ("en_revision".equals(estado)) {
            return "En revisión";
        }
        if ("rechazada".equals(estado)) {
            return "Rechazada";
        }
        return estado == null ? "" : estado;
    }
}