package servicio;

import dao.EvidenciaDAO;
import dao.InstitucionDAO;
import dao.RegistroHorasDAO;
import dao.SolicitudConstanciaDAO;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import modelo.EstadoRegistro;
import modelo.EstadoSolicitud;
import modelo.Evidencia;
import modelo.Institucion;
import modelo.RegistroHoras;
import modelo.SolicitudConstancia;
import modelo.Usuario;

/**
 * Reglas del panel del docente y del responsable de área (parte 5).
 * Las pantallas llaman a esta clase, nunca directo a los DAO, para que las reglas
 * se cumplan siempre:
 *
 *  - Rechazar un registro o una solicitud de constancia exige comentario.
 *  - Solo se revisa lo que sigue pendiente (si alguien lo revisó antes, se avisa).
 *  - El docente ve los registros de sus alumnos (inscripciones.id_docente) y los
 *    que no tienen docente asignado; el responsable de área ve todos.
 *  - Solo el responsable de área valida y sube constancias. Se valida solo si las
 *    horas aprobadas cubren las requeridas, y el archivo debe ser un PDF de máx. 10 MB.
 */
public class PanelDocenteService {

    public static final int MAX_COMENTARIO = 500;
    public static final long MAX_PDF_BYTES = 10L * 1024 * 1024;
    /** Carpeta (dentro del proyecto) donde se guardan los PDF de las constancias. */
    public static final String CARPETA_CONSTANCIAS = "archivos/constancias";

    private final Usuario usuario;
    private final RegistroHorasDAO registros = new RegistroHorasDAO();
    private final EvidenciaDAO evidencias = new EvidenciaDAO();
    private final InstitucionDAO instituciones = new InstitucionDAO();
    private final SolicitudConstanciaDAO solicitudes = new SolicitudConstanciaDAO();

    public PanelDocenteService(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public boolean puedeGestionarConstancias() {
        return usuario.esResponsable();
    }

    // ------------------------------------------------------------------
    // Registros de horas
    // ------------------------------------------------------------------

    public List<RegistroHoras> listarRegistros(EstadoRegistro estado) throws SQLException {
        Integer idDocente = usuario.esResponsable() ? null : usuario.getIdUsuario();
        return registros.listarPorEstado(estado, idDocente);
    }

    public RegistroHoras obtenerRegistro(int idRegistro) throws SQLException {
        return registros.buscarPorId(idRegistro);
    }

    public List<Evidencia> evidenciasDe(int idRegistro) throws SQLException {
        return evidencias.listarPorRegistro(idRegistro);
    }

    public BigDecimal horasAprobadas(int idInscripcion) throws SQLException {
        return registros.horasAprobadas(idInscripcion);
    }

    /** El comentario es opcional al aprobar. */
    public void aprobarRegistro(int idRegistro, String comentario) throws ReglaNegocioException, SQLException {
        String limpio = comentarioOpcional(comentario);
        if (!registros.revisar(idRegistro, EstadoRegistro.APROBADO, usuario.getIdUsuario(), limpio)) {
            throw new ReglaNegocioException("Este registro ya fue revisado. Actualiza la lista.");
        }
    }

    /** El comentario es obligatorio: el alumno lo lee para corregir su registro. */
    public void rechazarRegistro(int idRegistro, String comentario) throws ReglaNegocioException, SQLException {
        String limpio = comentarioObligatorio(comentario, "rechazar el registro");
        if (!registros.revisar(idRegistro, EstadoRegistro.RECHAZADO, usuario.getIdUsuario(), limpio)) {
            throw new ReglaNegocioException("Este registro ya fue revisado. Actualiza la lista.");
        }
    }

    /**
     * Devuelve el archivo de una evidencia. Si la ruta guardada es relativa,
     * se busca dentro de la carpeta del proyecto.
     */
    public static File archivoDe(String ruta) {
        if (ruta == null) {
            return null;
        }
        File f = new File(ruta);
        return f.isAbsolute() ? f : new File(System.getProperty("user.dir"), ruta);
    }

    // ------------------------------------------------------------------
    // Instituciones de prácticas
    // ------------------------------------------------------------------

    public List<Institucion> listarInstitucionesEnRevision() throws SQLException {
        return instituciones.listarEnRevision();
    }

    public void aprobarInstitucion(int idInstitucion) throws ReglaNegocioException, SQLException {
        if (!instituciones.aprobar(idInstitucion, usuario.getIdUsuario())) {
            throw new ReglaNegocioException("Esta institución ya fue revisada. Actualiza la lista.");
        }
    }

    public void rechazarInstitucion(int idInstitucion) throws ReglaNegocioException, SQLException {
        if (!instituciones.rechazar(idInstitucion)) {
            throw new ReglaNegocioException("Esta institución ya fue revisada. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------
    // Constancias (solo responsable de área)
    // ------------------------------------------------------------------

    /** @param estado el filtro, o null para ver todas */
    public List<SolicitudConstancia> listarSolicitudes(EstadoSolicitud estado) throws SQLException {
        return solicitudes.listar(estado);
    }

    /**
     * Paso 1: el responsable confirma que las horas están completas.
     * Vuelve a leer las horas de la base en lugar de confiar en lo que muestra la pantalla.
     */
    public void validarHoras(int idSolicitud) throws ReglaNegocioException, SQLException {
        exigirResponsable();
        SolicitudConstancia s = solicitudVigente(idSolicitud);
        if (s.getEstado() != EstadoSolicitud.SOLICITADA) {
            throw new ReglaNegocioException("Esta solicitud ya está " + s.getEstado().toString().toLowerCase() + ".");
        }
        if (!s.horasCompletas()) {
            throw new ReglaNegocioException("No se puede validar: el alumno lleva "
                    + texto(s.getHorasAprobadas()) + " de " + s.getHorasRequeridas() + " horas aprobadas.");
        }
        if (!solicitudes.validar(idSolicitud, usuario.getIdUsuario())) {
            throw new ReglaNegocioException("Esta solicitud ya fue revisada. Actualiza la lista.");
        }
    }

    /**
     * Paso 2: sube el PDF de una solicitud validada. El archivo se copia a
     * archivos/constancias/constancia_<id>.pdf y esa ruta se guarda en la base.
     */
    public void subirConstancia(int idSolicitud, File pdf) throws ReglaNegocioException, SQLException, IOException {
        exigirResponsable();
        SolicitudConstancia s = solicitudVigente(idSolicitud);
        if (s.getEstado() == EstadoSolicitud.SOLICITADA) {
            throw new ReglaNegocioException("Primero valida las horas de esta solicitud.");
        }
        if (s.getEstado() != EstadoSolicitud.VALIDADA) {
            throw new ReglaNegocioException("Esta solicitud ya está " + s.getEstado().toString().toLowerCase() + ".");
        }
        validarPdf(pdf);

        String rutaRelativa = CARPETA_CONSTANCIAS + "/constancia_" + idSolicitud + ".pdf";
        Path destino = archivoDe(rutaRelativa).toPath();
        Files.createDirectories(destino.getParent());
        Files.copy(pdf.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        try {
            if (!solicitudes.marcarDisponible(idSolicitud, s.getIdInscripcion(), usuario.getIdUsuario(), rutaRelativa)) {
                Files.deleteIfExists(destino);
                throw new ReglaNegocioException("Esta solicitud ya fue revisada. Actualiza la lista.");
            }
        } catch (SQLException e) {
            Files.deleteIfExists(destino);
            throw e;
        }
    }

    public void rechazarSolicitud(int idSolicitud, String comentario) throws ReglaNegocioException, SQLException {
        exigirResponsable();
        String limpio = comentarioObligatorio(comentario, "rechazar la solicitud");
        if (!solicitudes.rechazar(idSolicitud, usuario.getIdUsuario(), limpio)) {
            throw new ReglaNegocioException("Esta solicitud ya no se puede rechazar. Actualiza la lista.");
        }
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private void exigirResponsable() throws ReglaNegocioException {
        if (!usuario.esResponsable()) {
            throw new ReglaNegocioException("Solo el responsable de área puede gestionar constancias.");
        }
    }

    private SolicitudConstancia solicitudVigente(int idSolicitud) throws ReglaNegocioException, SQLException {
        SolicitudConstancia s = solicitudes.buscarPorId(idSolicitud);
        if (s == null) {
            throw new ReglaNegocioException("La solicitud ya no existe.");
        }
        return s;
    }

    private static String comentarioObligatorio(String comentario, String accion) throws ReglaNegocioException {
        String limpio = comentario == null ? "" : comentario.trim();
        if (limpio.isEmpty()) {
            throw new ReglaNegocioException("Escribe un comentario para " + accion + ".");
        }
        if (limpio.length() > MAX_COMENTARIO) {
            throw new ReglaNegocioException("El comentario debe tener máximo " + MAX_COMENTARIO + " caracteres.");
        }
        return limpio;
    }

    private static String comentarioOpcional(String comentario) throws ReglaNegocioException {
        String limpio = comentario == null ? "" : comentario.trim();
        if (limpio.length() > MAX_COMENTARIO) {
            throw new ReglaNegocioException("El comentario debe tener máximo " + MAX_COMENTARIO + " caracteres.");
        }
        return limpio.isEmpty() ? null : limpio;
    }

    private static void validarPdf(File pdf) throws ReglaNegocioException, IOException {
        if (pdf == null || !pdf.isFile()) {
            throw new ReglaNegocioException("No se encontró el archivo.");
        }
        if (!pdf.getName().toLowerCase().endsWith(".pdf")) {
            throw new ReglaNegocioException("El archivo debe ser un PDF.");
        }
        if (pdf.length() == 0) {
            throw new ReglaNegocioException("El archivo está vacío.");
        }
        if (pdf.length() > MAX_PDF_BYTES) {
            throw new ReglaNegocioException("El PDF pesa más de 10 MB.");
        }
        byte[] firma = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        byte[] inicio = new byte[firma.length];
        try (InputStream in = Files.newInputStream(pdf.toPath())) {
            if (in.readNBytes(inicio, 0, inicio.length) < inicio.length || !Arrays.equals(inicio, firma)) {
                throw new ReglaNegocioException("El archivo no es un PDF válido.");
            }
        }
    }

    private static String texto(BigDecimal horas) {
        return horas == null || horas.signum() == 0 ? "0" : horas.stripTrailingZeros().toPlainString();
    }

    /** Ruta de la carpeta de constancias, para mostrarla en mensajes. */
    public static String carpetaConstanciasAbsoluta() {
        return Paths.get(System.getProperty("user.dir"), CARPETA_CONSTANCIAS).toString();
    }
}
