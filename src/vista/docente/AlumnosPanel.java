package vista.docente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import modelo.AlumnoActivo;
import modelo.Programa;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.Avatar;
import vista.comun.BarraDesplazamiento;
import vista.comun.BarraProgreso;
import vista.comun.Boton;
import vista.comun.CampoBusqueda;
import vista.comun.Chip;
import vista.comun.ChipFiltro;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.Tarjeta;
import vista.comun.TextoAjustado;
import vista.comun.ZonaArchivo;

/**
 * Pestaña "Alumnos y constancias" (diseño base, opción E):
 * a la izquierda la lista de alumnos con su avance; a la derecha el detalle del
 * seleccionado, sus últimos turnos y la constancia con zona para arrastrar el PDF.
 * Solo muestra constancias POR RESOLVER: al aprobarla o rechazarla, el alumno pasa
 * a la pestaña "Historial de constancias".
 */
public class AlumnosPanel extends JPanel {

    private static final int ULTIMOS_TURNOS = 4;

    private final PanelDocenteService servicio;
    private final DefaultListModel<AlumnoActivo> modeloLista = new DefaultListModel<>();
    private final JList<AlumnoActivo> lista = new JList<>(modeloLista);
    private final CampoBusqueda buscar = new CampoBusqueda("Buscar alumno o matrícula");
    private final ChipFiltro filtroTodos = new ChipFiltro("Todos");
    private final ChipFiltro filtroServicio = new ChipFiltro("Servicio social");
    private final ChipFiltro filtroPracticas = new ChipFiltro("Prácticas");
    private final JLabel resumen = Estilo.secundario(" ");
    private final JPanel detalle = Estilo.panel(new BorderLayout());

    private List<AlumnoActivo> todos = new ArrayList<>();
    private File archivoElegido;
    private boolean recargando;

    public AlumnosPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(20, 0));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        add(crearColumnaLista(), BorderLayout.WEST);
        JScrollPane scrollDetalle = new JScrollPane(detalle);
        scrollDetalle.setBorder(BorderFactory.createEmptyBorder());
        scrollDetalle.setOpaque(false);
        scrollDetalle.getViewport().setOpaque(false);
        scrollDetalle.getVerticalScrollBar().setUnitIncrement(16);
        BarraDesplazamiento.aplicar(scrollDetalle);
        add(scrollDetalle, BorderLayout.CENTER);

        buscar.alCambiar(texto -> filtrar());
        filtroTodos.addActionListener(e -> filtrar());
        filtroServicio.addActionListener(e -> filtrar());
        filtroPracticas.addActionListener(e -> filtrar());
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !recargando) {
                mostrarDetalle();
            }
        });
        mostrarDetalle();
    }

    // ------------------------------------------------------------------
    // Columna izquierda: búsqueda, filtros y lista
    // ------------------------------------------------------------------

    private JPanel crearColumnaLista() {
        ChipFiltro.grupo(filtroTodos, filtroServicio, filtroPracticas);
        JPanel filtros = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filtros.add(filtroTodos);
        filtros.add(filtroServicio);
        filtros.add(filtroPracticas);

        lista.setCellRenderer(new FilaAlumno());
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setFixedCellHeight(64);
        lista.getAccessibleContext().setAccessibleName("Lista de alumnos");
        Tarjeta marcoLista = new Tarjeta(new BorderLayout(), Estilo.SUPERFICIE, Estilo.BORDE, 14);
        marcoLista.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        marcoLista.add(Estilo.scroll(lista), BorderLayout.CENTER);

        JPanel arriba = Estilo.panel(new BorderLayout(0, 12));
        arriba.add(buscar, BorderLayout.NORTH);
        arriba.add(filtros, BorderLayout.SOUTH);

        JPanel columna = Estilo.panel(new BorderLayout(0, 12));
        columna.setPreferredSize(new Dimension(350, 10));
        columna.add(arriba, BorderLayout.NORTH);
        columna.add(marcoLista, BorderLayout.CENTER);
        columna.add(resumen, BorderLayout.SOUTH);
        return columna;
    }

    /** Vuelve a leer los alumnos desde la base y conserva el que estaba seleccionado. */
    public final void recargar() {
        AlumnoActivo antes = lista.getSelectedValue();
        try {
            todos = new ArrayList<>(servicio.listarAlumnos());
            todos.removeIf(a -> !constanciaPendiente(a));
            todos.sort(Comparator.comparingInt(AlumnosPanel::prioridad)
                    .thenComparing(a -> -Formato.porcentaje(a.getHorasAprobadas(), a.getHorasRequeridas()))
                    .thenComparing(AlumnoActivo::getNombre));
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        filtrar(antes == null ? null : antes.getIdInscripcion());
    }

    /**
     * Aquí solo quedan las constancias por resolver. En cuanto se APRUEBA o se RECHAZA,
     * el alumno sale de esta lista y queda en "Historial de constancias".
     * Si un alumno rechazado vuelve a pedirla, regresa aquí (su solicitud queda en espera).
     */
    static boolean constanciaPendiente(AlumnoActivo a) {
        return !a.tieneConstancia() && !a.solicitudRechazada();
    }

    /** Primero los que puedes atender: listos para constancia, luego solicitudes en espera y en curso. */
    private static int prioridad(AlumnoActivo a) {
        if (a.horasCompletas()) {
            return 0;
        }
        return a.solicitudEnEspera() ? 1 : 2;
    }

    private void filtrar() {
        AlumnoActivo actual = lista.getSelectedValue();
        filtrar(actual == null ? null : actual.getIdInscripcion());
    }

    private void filtrar(Integer conservarInscripcion) {
        String texto = buscar.getText().trim().toLowerCase();
        recargando = true;
        modeloLista.clear();
        int aSeleccionar = -1;
        for (AlumnoActivo a : todos) {
            boolean okPrograma = filtroTodos.isSelected()
                    || (filtroServicio.isSelected() && "servicio_social".equals(a.getTipoPrograma()))
                    || (filtroPracticas.isSelected() && "practicas".equals(a.getTipoPrograma()));
            boolean okTexto = texto.isEmpty() || a.getNombre().toLowerCase().contains(texto)
                    || (a.getMatricula() != null && a.getMatricula().toLowerCase().contains(texto));
            if (okPrograma && okTexto) {
                if (conservarInscripcion != null && a.getIdInscripcion() == conservarInscripcion) {
                    aSeleccionar = modeloLista.size();
                }
                modeloLista.addElement(a);
            }
        }
        if (aSeleccionar < 0 && !modeloLista.isEmpty()) {
            aSeleccionar = 0;
        }
        if (aSeleccionar >= 0) {
            lista.setSelectedIndex(aSeleccionar);
            lista.ensureIndexIsVisible(aSeleccionar);
        }
        recargando = false;
        long listos = todos.stream().filter(AlumnoActivo::horasCompletas).count();
        resumen.setText(modeloLista.size() + " de " + todos.size() + " alumno(s)  ·  " + listos + " listo(s) para constancia");
        mostrarDetalle();
    }

    // ------------------------------------------------------------------
    // Columna derecha: detalle del alumno
    // ------------------------------------------------------------------

    private void mostrarDetalle() {
        AlumnoActivo a = lista.getSelectedValue();
        archivoElegido = null;
        detalle.removeAll();
        if (a == null) {
            Tarjeta vacio = new Tarjeta();
            vacio.setBorder(BorderFactory.createEmptyBorder(40, 24, 40, 24));
            JLabel texto = Estilo.etiqueta(todos.isEmpty()
                    ? "No hay constancias pendientes. Las aprobadas y rechazadas están en Historial de constancias."
                    : "Selecciona un alumno de la lista para ver su detalle.", Estilo.fuente(Font.PLAIN, 15), Estilo.TEXTO_SECUNDARIO);
            texto.setHorizontalAlignment(JLabel.CENTER);
            vacio.add(texto, BorderLayout.CENTER);
            detalle.add(vacio, BorderLayout.NORTH);
        } else {
            List<RegistroHoras> ultimos = new ArrayList<>();
            int turnos = 0;
            try {
                ultimos = servicio.ultimosTurnosAprobados(a.getIdInscripcion(), ULTIMOS_TURNOS);
                turnos = servicio.turnosAprobados(a.getIdInscripcion());
            } catch (SQLException e) {
                Dialogos.mostrarError(this, e);
            }
            JPanel contenido = Estilo.panel(new GridBagLayout());
            agregar(contenido, crearEncabezado(a), 0, 0);
            agregar(contenido, crearIndicadores(a, turnos), 1, 16);
            agregar(contenido, crearFilaInferior(a, ultimos), 2, 16);
            detalle.add(contenido, BorderLayout.NORTH);
        }
        detalle.revalidate();
        detalle.repaint();
    }

    private JComponent crearEncabezado(AlumnoActivo a) {
        Tarjeta t = new Tarjeta(new BorderLayout(16, 0));
        t.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));
        Avatar avatar = new Avatar(a.getNombre(), 56);
        avatar.setColores(Estilo.VALIDADA_FONDO, Estilo.VALIDADA_TEXTO);
        t.add(avatar, BorderLayout.WEST);

        JPanel textos = Estilo.panel(new GridLayout(2, 1, 0, 2));
        textos.add(Estilo.titulo(a.getNombre()));
        textos.add(Estilo.etiqueta(vacio(a.getMatricula()) + "  ·  " + vacio(a.getCarrera()) + "  ·  " + vacio(a.getCorreo()),
                Estilo.fuente(Font.PLAIN, 14), Estilo.TEXTO_SECUNDARIO));
        t.add(textos, BorderLayout.CENTER);

        JPanel chips = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 6, 16));
        chips.add(new Chip(Programa.texto(a.getTipoPrograma()), Estilo.coloresPrograma(a.getTipoPrograma())));
        chips.add(new Chip(a.getInstitucion(), Estilo.NEUTRO_FONDO, Estilo.TEXTO));
        t.add(chips, BorderLayout.EAST);
        return t;
    }

    private JComponent crearIndicadores(AlumnoActivo a, int turnos) {
        JPanel fila = Estilo.panel(new GridLayout(1, 3, 14, 0));
        boolean completo = a.horasCompletas();
        int porcentaje = Formato.porcentaje(a.getHorasAprobadas(), a.getHorasRequeridas());

        BarraProgreso barra = new BarraProgreso(7);
        barra.setValor(porcentaje);
        fila.add(indicador("Horas aprobadas",
                Formato.horas(a.getHorasAprobadas()) + " / " + a.getHorasRequeridas() + " h", barra, completo));

        boolean hayPendientes = a.getHorasPendientes() != null && a.getHorasPendientes().signum() > 0;
        fila.add(indicador("En revisión", Formato.horas(a.getHorasPendientes()) + " h",
                Estilo.secundario(hayPendientes ? "Esperan tu revisión en Registros" : "Sin turnos pendientes"), false));

        fila.add(indicador("Turnos aprobados", String.valueOf(turnos),
                Estilo.secundario(a.getDocenteAsignado() == null ? "Sin docente asignado" : "Docente: " + a.getDocenteAsignado()), false));
        return fila;
    }

    private static Tarjeta indicador(String titulo, String valor, JComponent pie, boolean verde) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 6),
                verde ? Estilo.EXITO_SUAVE : Estilo.SUPERFICIE, verde ? Estilo.EXITO_BORDE : Estilo.BORDE, 12);
        Color tinta = verde ? Estilo.APROBADO_TEXTO : Estilo.TEXTO;
        t.add(Estilo.etiqueta(titulo, Estilo.fuente(Font.BOLD, 13), verde ? Estilo.APROBADO_TEXTO : Estilo.TEXTO_SECUNDARIO),
                BorderLayout.NORTH);
        t.add(Estilo.etiqueta(valor, Estilo.fuente(Font.BOLD, 26), tinta), BorderLayout.CENTER);
        t.add(pie, BorderLayout.SOUTH);
        return t;
    }

    private JComponent crearFilaInferior(AlumnoActivo a, List<RegistroHoras> ultimos) {
        JPanel fila = Estilo.panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1.0;
        c.insets = new Insets(0, 0, 0, 8);
        fila.add(crearUltimosTurnos(a, ultimos), c);
        c.gridx = 1;
        c.weightx = 1.3;
        c.insets = new Insets(0, 8, 0, 0);
        fila.add(crearConstancia(a), c);
        return fila;
    }

    private JComponent crearUltimosTurnos(AlumnoActivo a, List<RegistroHoras> ultimos) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 10));
        t.add(Estilo.subtitulo("Últimos turnos aprobados"), BorderLayout.NORTH);

        JPanel filas = Estilo.panel(null);
        filas.setLayout(new BoxLayout(filas, BoxLayout.Y_AXIS));
        if (ultimos.isEmpty()) {
            filas.add(Estilo.secundario("Todavía no tiene turnos aprobados."));
        }
        for (int i = 0; i < ultimos.size(); i++) {
            RegistroHoras r = ultimos.get(i);
            JPanel f = Estilo.panel(new BorderLayout());
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, i < ultimos.size() - 1 ? 1 : 0, 0, new Color(0xEEF0F5)),
                    BorderFactory.createEmptyBorder(9, 0, 9, 0)));
            f.add(Estilo.etiqueta(Formato.fecha(r.getFecha()) + "  ·  " + Formato.horario(r.getHoraInicio(), r.getHoraFin()),
                    Estilo.fuente(Font.PLAIN, 13), Estilo.TEXTO), BorderLayout.WEST);
            f.add(Estilo.etiqueta(Formato.horas(r.getHorasCalculadas()) + " h", Estilo.fuente(Font.BOLD, 13), Estilo.TEXTO),
                    BorderLayout.EAST);
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            filas.add(f);
        }
        t.add(filas, BorderLayout.CENTER);

        Boton verTodos = Boton.enlace("Ver todos sus registros");
        verTodos.addActionListener(e -> RegistrosAlumnoDialog.mostrar(this, servicio, a));
        JPanel pie = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pie.add(verTodos);
        t.add(pie, BorderLayout.SOUTH);
        return t;
    }

    private JComponent crearConstancia(AlumnoActivo a) {
        Tarjeta t = new Tarjeta(new BorderLayout(0, 12));
        boolean completo = a.horasCompletas();
        String programa = "practicas".equals(a.getTipoPrograma()) ? "prácticas" : "servicio social";

        JPanel cabecera = Estilo.panel(new BorderLayout(8, 0));
        cabecera.add(Estilo.subtitulo("Constancia de " + programa), BorderLayout.WEST);
        cabecera.add(Chip.deEstado(a.textoConstancia()), BorderLayout.EAST);
        JPanel arriba = Estilo.panel(new BorderLayout(0, 4));
        arriba.add(cabecera, BorderLayout.NORTH);
        // El texto se acomoda en varias líneas si la tarjeta es angosta
        TextoAjustado linea = new TextoAjustado(lineaDeEstado(a), Estilo.fuente(Font.PLAIN, 13),
                Estilo.TEXTO_SECUNDARIO, false);
        linea.setAjusteAutomatico(true);
        arriba.add(linea, BorderLayout.SOUTH);
        t.add(arriba, BorderLayout.NORTH);

        ZonaArchivo zona = new ZonaArchivo("Arrastra aquí el PDF de la constancia",
                "o haz clic para elegirlo · solo PDF, máximo 10 MB", "Archivos PDF", "pdf");
        zona.setTituloSelector("Elegir la constancia de " + a.getNombre(), "Solo archivos PDF, máximo 10 MB.");
        zona.setEnabled(completo);
        if (!completo) {
            zona.setTextos("Todavía no se puede subir", "Le faltan " + Formato.horas(a.horasFaltantes()) + " h aprobadas");
        }
        zona.alElegirArchivo(f -> archivoElegido = f);
        t.add(zona, BorderLayout.CENTER);

        Boton subir = Estilo.botonExito("Subir PDF y aprobar");
        subir.setEnabled(completo);
        if (!completo) {
            subir.setToolTipText("Se habilita cuando el alumno completa sus horas aprobadas.");
        }
        subir.addActionListener(e -> subirPdfYAprobar(a, zona));
        JPanel botones = Estilo.panel(new BorderLayout());
        botones.add(subir, BorderLayout.CENTER);

        JPanel abajo = Estilo.panel(new BorderLayout(0, 8));
        abajo.add(botones, BorderLayout.NORTH);
        if (completo) {
            JLabel nota = Estilo.secundario(primerNombre(a) + " podrá descargarla desde su menú en cuanto la apruebes.");
            nota.setHorizontalAlignment(JLabel.CENTER);
            abajo.add(nota, BorderLayout.CENTER);
        }
        if (a.solicitudEnEspera()) {
            Boton rechazar = Estilo.botonPeligro("Rechazar solicitud");
            rechazar.addActionListener(e -> rechazarSolicitud(a));
            JPanel fila = Estilo.panel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            fila.add(rechazar);
            abajo.add(fila, BorderLayout.SOUTH);
        }
        t.add(abajo, BorderLayout.SOUTH);
        return t;
    }

    private static String lineaDeEstado(AlumnoActivo a) {
        if (a.getEstadoConstancia() != null) {
            switch (a.getEstadoConstancia()) {
                case SOLICITADA:
                case VALIDADA:
                    return "La pidió el " + Formato.fechaHora(a.getFechaSolicitud()) + ".";
                default:
                    break;
            }
        }
        return a.horasCompletas()
                ? "Completó sus horas; aún no la pide. Puedes entregársela ya."
                : "Se habilita cuando complete sus " + a.getHorasRequeridas() + " horas aprobadas.";
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    private void subirPdfYAprobar(AlumnoActivo a, ZonaArchivo zona) {
        if (archivoElegido == null) {
            zona.elegirConDialogo();
            if (archivoElegido == null) {
                return;
            }
        }
        String programa = "practicas".equals(a.getTipoPrograma()) ? "prácticas" : "servicio social";
        String archivo = archivoElegido.getName() + "  ·  " + Formato.tamanoArchivo(archivoElegido.length());
        if (!Dialogos.confirmarAprobar(this, "¿Subir y aprobar la constancia?",
                "Será la constancia de " + programa + " de " + a.getNombre()
                + " y podrá descargarla desde su menú.", archivo, "Subir y aprobar")) {
            return;
        }
        try {
            servicio.subirPdfYAprobar(a.getIdInscripcion(), archivoElegido);
            Dialogos.exito(this, "Constancia aprobada", a.getNombre() + " ya puede descargarla desde su menú. "
                    + "Desde ahora la encuentras en Historial de constancias.");
        } catch (ReglaNegocioException | SQLException | IOException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    /** Abre el PDF de una constancia con el programa de la computadora (también lo usa el historial). */
    static void abrirPdf(Component padre, String ruta) {
        File pdf = PanelDocenteService.archivoDe(ruta);
        if (pdf == null || !pdf.isFile()) {
            Dialogos.aviso(padre, "No se encontró el PDF",
                    "El archivo de la constancia no está en esta computadora. Puedes volver a subirlo con Reemplazar PDF.",
                    pdf == null ? "(sin ruta guardada)" : pdf.getAbsolutePath());
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(pdf);
            } else {
                Dialogos.info(padre, "Ubicación del PDF", "Esta computadora no permite abrirlo desde aquí. Está en:",
                        pdf.getAbsolutePath());
            }
        } catch (IOException e) {
            Dialogos.mostrarError(padre, e);
        }
    }

    private void rechazarSolicitud(AlumnoActivo a) {
        if (a.getIdSolicitud() == null) {
            return;
        }
        String comentario = Dialogos.pedirMotivoRechazo(this, "Rechazar solicitud de constancia",
                "¿Por qué se rechaza la solicitud de " + a.getNombre() + "? El alumno leerá este comentario.",
                "Rechazar solicitud", "Ej. Faltan horas por validar en la institución");
        if (comentario == null) {
            return;
        }
        try {
            servicio.rechazarSolicitud(a.getIdSolicitud(), comentario);
            Dialogos.exito(this, "Solicitud rechazada", a.getNombre() + " verá tu motivo en su sección de constancia. "
                    + "Desde ahora la encuentras en Historial de constancias.");
        } catch (ReglaNegocioException | SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        recargar();
    }

    // ------------------------------------------------------------------
    // Ayudantes
    // ------------------------------------------------------------------

    private static void agregar(JPanel panel, JComponent c, int fila, int espacioArriba) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = fila;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.insets = new Insets(espacioArriba, 0, 0, 0);
        panel.add(c, g);
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }

    private static String primerNombre(AlumnoActivo a) {
        return a.getNombre().trim().split("\\s+")[0];
    }

    /** Texto corto y color del estado para la lista de la izquierda. */
    private static String estadoCorto(AlumnoActivo a) {
        if (a.horasCompletas()) {
            return "Lista";
        }
        return a.solicitudEnEspera() ? "Solicitada" : "En curso";
    }

    private static Color colorEstadoCorto(String estado) {
        return switch (estado) {
            case "Lista" -> Estilo.PRIMARIO;
            case "Solicitada" -> new Color(0xB45309);
            default -> Estilo.TEXTO_SECUNDARIO;
        };
    }

    /** Cómo se dibuja cada alumno en la lista de la izquierda. */
    private static class FilaAlumno extends JPanel implements ListCellRenderer<AlumnoActivo> {
        private final Avatar avatar = new Avatar("", 34);
        private final JLabel nombre = Estilo.etiqueta("", Estilo.fuente(Font.BOLD, 14), Estilo.TEXTO);
        private final JLabel estado = Estilo.etiqueta("", Estilo.fuente(Font.BOLD, 12), Estilo.TEXTO_SECUNDARIO);
        private final BarraProgreso barra = new BarraProgreso(5);
        private final JLabel detalle = Estilo.etiqueta("", Estilo.fuente(Font.PLAIN, 12), Estilo.TEXTO_SECUNDARIO);
        private boolean seleccionada;

        FilaAlumno() {
            super(new BorderLayout(10, 0));
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            JPanel arriba = Estilo.panel(new BorderLayout(6, 0));
            arriba.add(nombre, BorderLayout.CENTER);
            arriba.add(estado, BorderLayout.EAST);
            JPanel abajo = Estilo.panel(new BorderLayout(8, 0));
            abajo.add(barra, BorderLayout.CENTER);
            abajo.add(detalle, BorderLayout.EAST);
            JPanel centro = Estilo.panel(new GridLayout(2, 1, 0, 4));
            centro.add(arriba);
            centro.add(abajo);
            JPanel contenedorAvatar = Estilo.panel(new BorderLayout());
            contenedorAvatar.add(avatar, BorderLayout.CENTER);
            add(contenedorAvatar, BorderLayout.WEST);
            add(centro, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AlumnoActivo> lista, AlumnoActivo a,
                                                      int indice, boolean sel, boolean foco) {
            seleccionada = sel;
            boolean completo = a.horasCompletas();
            avatar.setNombre(a.getNombre());
            avatar.setColores(completo ? Estilo.APROBADO_FONDO : new Color(0xEEF0F7),
                    completo ? Estilo.APROBADO_TEXTO : Estilo.NEUTRO_TEXTO);
            nombre.setText(a.getNombre());
            String e = estadoCorto(a);
            estado.setText(e);
            estado.setForeground(colorEstadoCorto(e));
            barra.setValor(Formato.porcentaje(a.getHorasAprobadas(), a.getHorasRequeridas()));
            detalle.setText(("practicas".equals(a.getTipoPrograma()) ? "Prácticas" : "Servicio") + " · "
                    + Formato.horas(a.getHorasAprobadas()) + "/" + a.getHorasRequeridas() + " h");
            getAccessibleContext().setAccessibleName(a.getNombre() + ", " + e + ", "
                    + Formato.horas(a.getHorasAprobadas()) + " de " + a.getHorasRequeridas() + " horas");
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(seleccionada ? Estilo.PRIMARIO_SUAVE : Estilo.SUPERFICIE);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(new Color(0xEEF0F5));
            g.fillRect(0, getHeight() - 1, getWidth(), 1);
            if (seleccionada) {
                g.setColor(Estilo.PRIMARIO);
                g.fillRect(0, 0, 3, getHeight());
            }
        }
    }
}
