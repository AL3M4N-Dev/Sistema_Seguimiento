package modelo;

/** Una imagen de evidencia de un registro (tabla evidencias). */
public class Evidencia {

    private final int idEvidencia;
    private final int idRegistro;
    private final String rutaArchivo;
    private final String nombreOriginal;

    public Evidencia(int idEvidencia, int idRegistro, String rutaArchivo, String nombreOriginal) {
        this.idEvidencia = idEvidencia;
        this.idRegistro = idRegistro;
        this.rutaArchivo = rutaArchivo;
        this.nombreOriginal = nombreOriginal;
    }

    public int getIdEvidencia() {
        return idEvidencia;
    }

    public int getIdRegistro() {
        return idRegistro;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    /** Nombre para mostrar: el original si existe, si no la ruta. */
    public String getNombreParaMostrar() {
        return nombreOriginal != null && !nombreOriginal.isBlank() ? nombreOriginal : rutaArchivo;
    }
}
