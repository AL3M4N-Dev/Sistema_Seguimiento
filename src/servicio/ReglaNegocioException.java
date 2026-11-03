package servicio;

/**
 * Se lanza cuando una acción no cumple una regla de la app
 * (por ejemplo, rechazar sin comentario). El mensaje se muestra tal cual al usuario.
 */
public class ReglaNegocioException extends Exception {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
