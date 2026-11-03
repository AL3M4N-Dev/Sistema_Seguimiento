package vista.docente;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.EstadoSolicitud;
import modelo.Programa;
import modelo.SolicitudConstancia;
import servicio.PanelDocenteService;
import servicio.ReglaNegocioException;
import vista.comun.AreaTexto;
import vista.comun.Avatar;
import vista.comun.Boton;
import vista.comun.CampoBusqueda;
import vista.comun.Chip;
import vista.comun.ChipFiltro;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.SelectorArchivo;
import vista.comun.Tarjeta;
import vista.comun.VentanaDialogo;

/**
 * Sección "Historial de constancias": las constancias que ya se resolvieron
 * (aprobadas o rechazadas), con filtros, búsqueda y detalle. Las que siguen
 * en espera están en "Alumnos y constancias".
 */
public class HistorialConstanciasPanel extends JPanel {

    private static final int COL_ESTADO = 7;
    private static final int COL_MOTIVO = 8;

    private final PanelDocenteService servicio;
    private final ChipFiltro verTodas = new ChipFiltro("Todas");
    private final ChipFiltro verAprobadas = new ChipFiltro("Aprobadas");
    private final ChipFiltro verRechazadas = new ChipFiltro("Rechazadas");
    private final CampoBusqueda buscar = new CampoBusqueda("Buscar alumno o matrícula");
    private final ListaTableModel<SolicitudConstancia> modelo = new ListaTableModel<>(
            new String[]{"Alumno", "Matrícula", "Programa", "Institución", "Solicitada", "Revisada", "Revisó",
                "Estado", "Motivo del rechazo"},
            (s, col) -> switch (col) {
                case 0 -> s.getAlumnoNombre();
                case 1 -> vacio(s.getMatricula());
                case 2 -> Programa.texto(s.getTipoPrograma());
                case 3 -> s.getInstitucionNombre();
                case 4 -> fecha(s.getFechaSolicitud());
                case 5 -> fecha(s.getFechaValidacion());
                case 6 -> vacio(s.getRevisorNombre());
                case 7 -> s.textoHistorial();
                default -> s.getEstado() == EstadoSolicitud.RECHAZADA && s.getComentario() != null ? s.getComentario() : "";
            });
    private final JTable tabla = new JTable(modelo) {
        @Override
        public String getToolTipText(MouseEvent e) {
            int fila = rowAtPoint(e.getPoint());
            int columna = columnAtPoint(e.getPoint());
            Object valor = fila < 0 || columna < 0 ? null : getValueAt(fila, columna);
            boolean largo = columna == 0 || columna == 3 || columna == 6 || columna == COL_MOTIVO;
            return largo && valor != null && !valor.toString().isEmpty() && !"—".equals(valor) ? valor.toString() : null;
        }
    };
    private final JLabel resumen = Estilo.secundario(" ");
    private List<SolicitudConstancia> todas = new ArrayList<>();

    public HistorialConstanciasPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 16));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        String alcance = servicio.getUsuario().esResponsable()
                ? "Constancias aprobadas y rechazadas de todos los alumnos. Doble clic en una para ver el detalle."
                : "Constancias aprobadas y rechazadas de tus alumnos (y de los que aún no tienen docente). "
                  + "Doble clic en una para ver el detalle.";
        JPanel textos = Estilo.panel(new BorderLayout(0, 4));
        textos.add(Estilo.titulo("Historial de constancias"), BorderLayout.NORTH);
        textos.add(Estilo.secundario(alcance), BorderLayout.SOUTH);

        ChipFiltro.grupo(verTodas, verAprobadas, verRechazadas);
        JPanel filtros = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filtros.add(verTodas);
        filtros.add(verAprobadas);
        filtros.add(verRechazadas);

        buscar.setPreferredSize(new Dimension(280, buscar.getPreferredSize().height));
        Boton actualizar = Estilo.botonSecundario("Actualizar");
        Boton verDetalle = Estilo.botonPrincipal("Ver detalle");
        JPanel acciones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(buscar);
        acciones.add(actualizar);
        acciones.add(verDetalle);

        JPanel barra = Estilo.panel(new BorderLayout());
        barra.add(filtros, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel arriba = Estilo.panel(new BorderLayout(0, 16));
        arriba.add(textos, BorderLayout.NORTH);
        arriba.add(barra, BorderLayout.SOUTH);

        Estilo.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        // Las fechas, el programa y el estado nunca se recortan; lo demás se ajusta al ancho
        int[] anchos = {150, 110, 130, 160, 115, 115, 140, 110, 190};
        int[] minimos = {90, 105, 125, 90, 115, 115, 80, 108, 80};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
            tabla.getColumnModel().getColumn(i).setMinWidth(minimos[i]);
        }
        tabla.getAccessibleContext().setAccessibleName("Historial de constancias");
        Tarjeta marco = new Tarjeta(new BorderLayout(), Estilo.SUPERFICIE, Estilo.BORDE, 14);
        marco.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        marco.add(Estilo.scroll(tabla), BorderLayout.CENTER);

        add(arriba, BorderLayout.NORTH);
        add(marco, BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        for (ChipFiltro chip : new ChipFiltro[]{verTodas, verAprobadas, verRechazadas}) {
            chip.addActionListener(e -> filtrar());
        }
        buscar.alCambiar(texto -> filtrar());
        actualizar.addActionListener(e -> recargar());
        verDetalle.addActionListener(e -> abrirDetalle());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tabla.rowAtPoint(e.getPoint()) >= 0) {
                    abrirDetalle();
                }
            }
        });
    }

    /** Vuelve a leer el historial desde la base. */
    public final void recargar() {
        try {
            todas = new ArrayList<>(servicio.historialConstancias());
            todas.removeIf(s -> !"Aprobada".equals(s.textoHistorial()) && !"Rechazada".equals(s.textoHistorial()));
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
        filtrar();
    }

    private void filtrar() {
        long aprobadas = contar("Aprobada");
        long rechazadas = contar("Rechazada");
        verTodas.setText("Todas (" + todas.size() + ")");
        verAprobadas.setText("Aprobadas (" + aprobadas + ")");
        verRechazadas.setText("Rechazadas (" + rechazadas + ")");

        String estado = verAprobadas.isSelected() ? "Aprobada" : verRechazadas.isSelected() ? "Rechazada" : null;
        String texto = buscar.getText().trim().toLowerCase();
        List<SolicitudConstancia> visibles = new ArrayList<>();
        for (SolicitudConstancia s : todas) {
            boolean okEstado = estado == null || estado.equals(s.textoHistorial());
            boolean okTexto = texto.isEmpty() || s.getAlumnoNombre().toLowerCase().contains(texto)
                    || (s.getMatricula() != null && s.getMatricula().toLowerCase().contains(texto));
            if (okEstado && okTexto) {
                visibles.add(s);
            }
        }
        modelo.setFilas(visibles);
        if (todas.isEmpty()) {
            resumen.setText("Todavía no hay constancias resueltas: aparecerán aquí cuando apruebes o rechaces una "
                    + "en Alumnos y constancias.");
        } else if (visibles.isEmpty()) {
            resumen.setText("No hay constancias con ese filtro.");
        } else {
            resumen.setText("Mostrando " + visibles.size() + " de " + todas.size() + "  ·  " + aprobadas + " aprobada(s)  ·  "
                    + rechazadas + " rechazada(s)");
        }
        revalidate();
        repaint();
    }

    private long contar(String estado) {
        return todas.stream().filter(s -> estado.equals(s.textoHistorial())).count();
    }

    // ------------------------------------------------------------------
    // Detalle
    // ------------------------------------------------------------------

    private void abrirDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Ninguna constancia seleccionada", "Haz clic en una fila de la tabla para ver su detalle.");
            return;
        }
        SolicitudConstancia s = modelo.getFila(fila);
        String estado = s.textoHistorial();

        VentanaDialogo v = new VentanaDialogo(this, s.getAlumnoNombre(), vacio(s.getMatricula()) + "  ·  "
                + Programa.texto(s.getTipoPrograma()) + "  ·  " + s.getInstitucionNombre());
        v.setTitle("Constancia de " + s.getAlumnoNombre());
        v.setIcono(new Avatar(s.getAlumnoNombre(), 44));
        v.setSeparador(true);
        v.setAncho(640);
        v.setMargenContenido(20, 24, 22, 24);

        JPanel cuerpo = Estilo.panel(new GridBagLayout());
        int fila2 = 0;
        Color[] colores = Estilo.coloresEstado(estado);
        JPanel datos = Estilo.panel(new GridLayout(1, 3, 12, 0));
        datos.add(dato("Estado", estado, colores[0], colores[1]));
        datos.add(dato("Solicitada", fechaHora(s.getFechaSolicitud()), Estilo.FONDO, Estilo.TEXTO));
        String revisada = s.getEstado() == EstadoSolicitud.DISPONIBLE ? "Aprobada el" : "Rechazada el";
        datos.add(dato(revisada, fechaHora(s.getFechaValidacion()), Estilo.FONDO, Estilo.TEXTO));
        agregar(cuerpo, datos, fila2++, 0);

        String revisor = s.getRevisorNombre() == null ? "Sin revisar todavía" : "Revisó: " + s.getRevisorNombre();
        agregar(cuerpo, Estilo.etiqueta("Horas aprobadas: " + Formato.horas(s.getHorasAprobadas()) + " de "
                + s.getHorasRequeridas() + " h  ·  " + revisor, Estilo.fuente(Font.PLAIN, 13), Estilo.TEXTO_SECUNDARIO), fila2++, 14);

        if (s.getEstado() == EstadoSolicitud.RECHAZADA) {
            agregar(cuerpo, Estilo.subtitulo("Motivo del rechazo"), fila2++, 20);
            AreaTexto motivo = AreaTexto.soloLectura(s.getComentario(), 3);
            motivo.setNombreAccesible("Motivo del rechazo");
            agregar(cuerpo, motivo, fila2++, 8);
            agregar(cuerpo, Estilo.secundario("El alumno ve este motivo. Si la vuelve a solicitar, regresa a Alumnos y constancias."),
                    fila2++, 8);
        } else {
            agregar(cuerpo, Estilo.secundario("El alumno ya puede descargarla desde su menú."), fila2++, 14);
        }
        v.setContenido(cuerpo);

        v.setPie(Chip.deEstado(estado));
        Boton cerrar = v.agregarBotonCancelar("Cerrar");
        if (s.getEstado() == EstadoSolicitud.DISPONIBLE) {
            // Como la aprobada ya no está en "Alumnos y constancias", aquí se puede cambiar el PDF
            Boton reemplazar = v.agregarBoton(Estilo.botonSecundario("Reemplazar PDF"));
            reemplazar.addActionListener(e -> {
                if (reemplazarPdf(v, s)) {
                    v.cerrar();
                    recargar();
                }
            });
            Boton abrir = v.agregarBoton(Estilo.botonPrincipal("Abrir PDF"));
            abrir.addActionListener(e -> AlumnosPanel.abrirPdf(v, s.getRutaPdf()));
            v.setBotonPorDefecto(abrir);
        } else {
            v.setBotonPorDefecto(cerrar);
        }
        v.mostrar();
    }

    /** Elige otro PDF y lo pone en lugar de la constancia aprobada. true si se reemplazó. */
    private boolean reemplazarPdf(Component padre, SolicitudConstancia s) {
        File pdf = SelectorArchivo.abrir(padre, "Reemplazar la constancia de " + s.getAlumnoNombre(),
                "Solo archivos PDF, máximo 10 MB.", "Archivos PDF", "pdf");
        if (pdf == null) {
            return false;
        }
        if (!Dialogos.confirmarAprobar(padre, "¿Reemplazar la constancia?",
                s.getAlumnoNombre() + " ya tiene su constancia. Se cambiará por este archivo:",
                pdf.getName() + "  ·  " + Formato.tamanoArchivo(pdf.length()), "Reemplazar PDF")) {
            return false;
        }
        try {
            servicio.subirPdfYAprobar(s.getIdInscripcion(), pdf);
            Dialogos.exito(padre, "Constancia reemplazada", s.getAlumnoNombre() + " ya puede descargar el nuevo PDF.");
            return true;
        } catch (ReglaNegocioException | SQLException | IOException e) {
            Dialogos.mostrarError(padre, e);
            return false;
        }
    }

    private static JComponent dato(String etiqueta, String valor, Color fondo, Color tinta) {
        Tarjeta p = new Tarjeta(new BorderLayout(0, 2), fondo, null, 10);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        p.add(Estilo.etiqueta(etiqueta, Estilo.fuente(Font.PLAIN, 13),
                tinta.equals(Estilo.TEXTO) ? Estilo.TEXTO_SECUNDARIO : tinta), BorderLayout.NORTH);
        p.add(Estilo.etiqueta(valor, Estilo.fuente(Font.BOLD, 15), tinta), BorderLayout.CENTER);
        return p;
    }

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

    private static String fecha(LocalDateTime f) {
        return f == null ? "—" : Formato.fecha(f.toLocalDate());
    }

    private static String fechaHora(LocalDateTime f) {
        return f == null ? "—" : Formato.fechaHora(f);
    }

    private static String vacio(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
