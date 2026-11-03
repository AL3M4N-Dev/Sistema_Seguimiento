package servicio;

import dao.AlumnoActivoDAO;
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
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Arrays;
import java.util.List;
import modelo.AlumnoActivo;
import modelo.EstadoRegistro;
import modelo.Evidencia;
import modelo.Institucion;
import modelo.RegistroHoras;
import modelo.Usuario;

/**
 * Reglas del panel del docente y del responsable de área (parte 5).
 * Las pantallas llaman a esta clase, nunca directo a los DAO, para que las reglas
 * se cumplan siempre:
 *
 *  - El docente ve a sus alumnos (inscripciones.id_docente) y a los que no tienen
 *    docente asignado; el responsable de área ve a todos.
 *  - Rechazar un registro o una solicitud de constancia exige comentario.
 *  - Solo se revisa lo que sigue pendiente (si alguien lo revisó antes, se avisa).
 *  - "Subir PDF y aprobar" solo se permite si las horas APROBADAS cubren las
 *    requeridas (se revisa en la base), y el archivo debe ser un PDF de máx. 10 MB.
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
    private final AlumnoActivoDAO alumnos = new AlumnoActivoDAO();
    private final SolicitudConstanciaDAO solicitudes = new SolicitudConstanciaDAO();

    public PanelDocenteService(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    /** null = ver a todos (responsable); si no, el id del docente para filtrar. */
    private Integer filtroDocente() {
        return usuario.esResponsable() ? null : usuario.getIdUsuario();
    }

    // ------------------------------------------------------------------
    // Registros de horas
    // ------------------------------------------------------------------

    public List<RegistroHoras> listarRegistros(EstadoRegistro estado) throws SQLException {
        return registros.listarPorEstado(estado, filtroDocente());
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
    // Alumnos y constancias
    // ------------------------------------------------------------------

    /** Alumnos activos (servicio social y prácticas) con su avance y su constancia. */
    public List<AlumnoActivo> listarAlumnos() throws SQLException {
        return alumnos.listar(filtroDocente());
    }

    /** Los últimos turnos APROBADOS de un alumno (para su ficha). */
    public List<RegistroHoras> ultimosTurnosAprobados(int idInscripcion, int cuantos) throws SQLException {
        return registros.listarPorInscripcion(idInscripcion, EstadoRegistro.APROBADO, cuantos);
    }

    /** Cuántos turnos aprobados tiene un alumno. */
    public int turnosAprobados(int idInscripcion) throws SQLException {
        return registros.contarAprobados(idInscripcion);
    }

    /** Todos los registros de un alumno, de cualquier estado ("Ver todos sus registros"). */
    public List<RegistroHoras> registrosDe(int idInscripcion) throws SQLException {
        return registros.listarPorInscripcion(idInscripcion, null, 0);
    }

    /** Cuántos registros pendientes hay (para el contador de la pestaña). */
    public int contarRegistrosPendientes() throws SQLException {
        return listarRegistros(EstadoRegistro.PENDIENTE).size();
    }

    /** Cuántas instituciones esperan revisión (para el contador de la pestaña). */
    public int contarInstitucionesEnRevision() throws SQLException {
        return listarInstitucionesEnRevision().size();
    }

    /**
     * "Subir PDF y aprobar": guarda el PDF de la constancia y la deja disponible
     * para que el alumno la descargue. Funciona igual para servicio social y prácticas,
     * y aunque el alumno no la haya pedido. Si ya tenía una, la reemplaza.
     *
     * El PDF se copia a archivos/constancias/constancia_<id_inscripcion>.pdf y esa
     * ruta se guarda en solicitudes_constancia.ruta_pdf.
     */
    public void subirPdfYAprobar(int idInscripcion, File pdf) throws ReglaNegocioException, SQLException, IOException {
        // Se vuelve a leer de la base: no se confía en lo que muestra la tabla
        AlumnoActivo a = alumnos.buscarPorInscripcion(idInscripcion, filtroDocente());
        if (a == null) {
            throw new ReglaNegocioException("No se encontró al alumno, o no lo tienes asignado.");
        }
        if (!a.horasCompletas()) {
            throw new ReglaNegocioException("Todavía no se puede: " + a.getNombre() + " lleva "
                    + texto(a.getHorasAprobadas()) + " de " + a.getHorasRequeridas() + " horas aprobadas.");
        }
        validarPdf(pdf);

        String rutaRelativa = CARPETA_CONSTANCIAS + "/constancia_" + idInscripcion + ".pdf";
        Path destino = archivoDe(rutaRelativa).toPath();
        Files.createDirectories(destino.getParent());
        // Primero se copia a un temporal: si falla la base, el PDF anterior (si había) no se pierde
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        Files.copy(pdf.toPath(), temporal, StandardCopyOption.REPLACE_EXISTING);
        try {
            solicitudes.aprobarConPdf(idInscripcion, usuario.getIdUsuario(), rutaRelativa);
        } catch (SQLIntegrityConstraintViolationException e) {
            Files.deleteIfExists(temporal);
            throw new ReglaNegocioException("Alguien más acaba de aprobar esta constancia. Actualiza la lista.");
        } catch (SQLException e) {
            Files.deleteIfExists(temporal);
            throw e;
        }
        Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Rechaza la solicitud que hizo el alumno; el comentario es obligatorio. */
    public void rechazarSolicitud(int idSolicitud, String comentario) throws ReglaNegocioException, SQLException {
        String limpio = comentarioObligatorio(comentario, "rechazar la solicitud");
        if (!solicitudes.rechazar(idSolicitud, usuario.getIdUsuario(), limpio)) {
            throw new ReglaNegocioException("Esta solicitud ya no se puede rechazar. Actualiza la lista.");
        }
    }

    /**
     * Devuelve el archivo de una ruta guardada en la base (evidencias o constancias).
     * Si la ruta es relativa, se busca dentro de la carpeta del proyecto.
     */
    public static File archivoDe(String ruta) {
        if (ruta == null) {
            return null;
        }
        File f = new File(ruta);
        return f.isAbsolute() ? f : new File(System.getProperty("user.dir"), ruta);
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

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
}
