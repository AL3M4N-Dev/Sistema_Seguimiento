package vista.docente;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.EstadoRegistro;
import modelo.Programa;
import modelo.RegistroHoras;
import servicio.PanelDocenteService;
import vista.comun.Boton;
import vista.comun.ChipFiltro;
import vista.comun.Dialogos;
import vista.comun.EstadoRenderer;
import vista.comun.Estilo;
import vista.comun.Formato;
import vista.comun.ListaTableModel;
import vista.comun.Tarjeta;

/** Sección "Registros": bandeja de turnos para aprobar o rechazar. */
public class RegistrosPanel extends JPanel {

    private static final int COL_ESTADO = 7;

    private final PanelDocenteService servicio;
    private final ChipFiltro verPendientes = new ChipFiltro("Pendientes");
    private final ChipFiltro verAprobados = new ChipFiltro("Aprobados");
    private final ChipFiltro verRechazados = new ChipFiltro("Rechazados");
    private final ListaTableModel<RegistroHoras> modelo = new ListaTableModel<>(
            new String[]{"Alumno", "Matrícula", "Programa", "Institución", "Fecha", "Horario", "Horas", "Estado"},
            (r, col) -> switch (col) {
                case 0 -> r.getAlumnoNombre();
                case 1 -> r.getMatricula();
                case 2 -> Programa.texto(r.getTipoPrograma());
                case 3 -> r.getInstitucionNombre();
                case 4 -> Formato.fecha(r.getFecha());
                case 5 -> Formato.horario(r.getHoraInicio(), r.getHoraFin());
                case 6 -> Formato.horas(r.getHorasCalculadas()) + " h";
                default -> r.getEstado();
            });
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = Estilo.secundario(" ");

    public RegistrosPanel(PanelDocenteService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout(0, 16));
        setBackground(Estilo.FONDO);
        setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        String alcance = servicio.getUsuario().esResponsable()
                ? "Turnos de todos los alumnos. Doble clic en uno para revisarlo."
                : "Turnos de tus alumnos asignados (y de los que aún no tienen docente). Doble clic en uno para revisarlo.";
        JPanel textos = Estilo.panel(new BorderLayout(0, 4));
        textos.add(Estilo.titulo("Registros de horas"), BorderLayout.NORTH);
        textos.add(Estilo.secundario(alcance), BorderLayout.SOUTH);

        ChipFiltro.grupo(verPendientes, verAprobados, verRechazados);
        JPanel filtros = Estilo.panel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        filtros.add(verPendientes);
        filtros.add(verAprobados);
        filtros.add(verRechazados);

        Boton actualizar = Estilo.botonSecundario("Actualizar");
        Boton revisar = Estilo.botonPrincipal("Revisar registro");
        JPanel acciones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        acciones.add(actualizar);
        acciones.add(revisar);

        JPanel barra = Estilo.panel(new BorderLayout());
        barra.add(filtros, BorderLayout.WEST);
        barra.add(acciones, BorderLayout.EAST);

        JPanel arriba = Estilo.panel(new BorderLayout(0, 16));
        arriba.add(textos, BorderLayout.NORTH);
        arriba.add(barra, BorderLayout.SOUTH);

        Estilo.estilizarTabla(tabla);
        tabla.setAutoCreateRowSorter(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(COL_ESTADO).setCellRenderer(new EstadoRenderer());
        int[] anchos = {170, 90, 110, 200, 90, 110, 60, 110};
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
        Tarjeta marco = new Tarjeta(new BorderLayout(), Estilo.SUPERFICIE, Estilo.BORDE, 14);
        marco.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        marco.add(Estilo.scroll(tabla), BorderLayout.CENTER);

        add(arriba, BorderLayout.NORTH);
        add(marco, BorderLayout.CENTER);
        add(resumen, BorderLayout.SOUTH);

        verPendientes.addActionListener(e -> recargar());
        verAprobados.addActionListener(e -> recargar());
        verRechazados.addActionListener(e -> recargar());
        actualizar.addActionListener(e -> recargar());
        revisar.addActionListener(e -> abrirDetalle());
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirDetalle();
                }
            }
        });
    }

    private EstadoRegistro estadoElegido() {
        return verAprobados.isSelected() ? EstadoRegistro.APROBADO
                : verRechazados.isSelected() ? EstadoRegistro.RECHAZADO : EstadoRegistro.PENDIENTE;
    }

    public final void recargar() {
        EstadoRegistro estado = estadoElegido();
        try {
            List<RegistroHoras> lista = servicio.listarRegistros(estado);
            modelo.setFilas(lista);
            resumen.setText(lista.size() + " registro(s) " + estado.toString().toLowerCase()
                    + (estado == EstadoRegistro.PENDIENTE ? "(s) por revisar" : "(s)") + ".");
        } catch (SQLException e) {
            Dialogos.mostrarError(this, e);
        }
    }

    private void abrirDetalle() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Ningún registro seleccionado", "Haz clic en un registro de la tabla para revisarlo.");
            return;
        }
        RegistroHoras r = modelo.getFila(tabla.convertRowIndexToModel(fila));
        if (DetalleRegistroDialog.abrir(this, servicio, r.getIdRegistro())) {
            recargar();
        }
    }
}
