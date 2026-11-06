package vista.alumno;

import dao.CatalogoDAO;
import dao.InscripcionDAO;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import modelo.InstitucionCatalogo;
import util.Sesion;
import vista.comun.Boton;
import vista.comun.Dialogos;
import vista.comun.Estilo;
import vista.comun.ListaTableModel;
import vista.comun.Tarjeta;

public class CatalogoFrame extends JFrame {

    private static final String[] COLUMNAS
            = {"Institución", "Dirección", "Horas requeridas", "Cupos", "Estado"};

    private final CatalogoDAO catalogoDAO = new CatalogoDAO();
    private final InscripcionDAO inscripcionDAO = new InscripcionDAO();
    private final ListaTableModel<InstitucionCatalogo> modelo
            = new ListaTableModel<>(COLUMNAS, (inst, col) -> valor(inst, col));
    private final JTable tabla = new JTable(modelo);
    private final Boton btnInscribirme = Estilo.botonPrincipal("Inscribirme");

    public CatalogoFrame() {
        setTitle("Catálogo de instituciones - Servicio social");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel raiz = new JPanel(new BorderLayout(0, 16));
        raiz.setBackground(Estilo.FONDO);
        raiz.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel encabezado = Estilo.panel(new BorderLayout(0, 4));
        encabezado.add(Estilo.titulo("Elige tu institución"), BorderLayout.NORTH);
        encabezado.add(Estilo.secundario(
                "Selecciona una institución de la tabla y pulsa \"Inscribirme\". "
                + "Solo puedes inscribirte una vez."), BorderLayout.SOUTH);
        raiz.add(encabezado, BorderLayout.NORTH);

        Estilo.estilizarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Tarjeta tarjeta = new Tarjeta();
        tarjeta.add(Estilo.scroll(tabla), BorderLayout.CENTER);
        raiz.add(tarjeta, BorderLayout.CENTER);

        JPanel botones = Estilo.panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        botones.add(btnInscribirme);
        raiz.add(botones, BorderLayout.SOUTH);

        btnInscribirme.setEnabled(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarBoton();
            }
        });
        btnInscribirme.addActionListener(e -> inscribirse());

        setContentPane(raiz);
        setSize(960, 600);
        setLocationRelativeTo(null);
        cargar();
    }

    // Texto que va en cada columna de la tabla.
    private static Object valor(InstitucionCatalogo inst, int col) {
        switch (col) {
            case 0:
                return inst.getNombre();
            case 1:
                return inst.getDireccion() == null ? "" : inst.getDireccion();
            case 2:
                return inst.getHorasRequeridas() + " h";
            case 3:
                if (inst.getCuposDisponibles() == null || inst.getCuposTotales() == null) {
                    return "-";
                }
                return inst.getCuposDisponibles() + " de " + inst.getCuposTotales();
            default:
                return inst.estadoTexto();
        }
    }

    // Llena la tabla con las instituciones de la base.
    private void cargar() {
        try {
            modelo.setFilas(catalogoDAO.listarServicioSocial());
        } catch (SQLException ex) {
            Dialogos.mostrarError(this, ex);
        }
    }

    // El botón solo se activa si la institución elegida tiene cupo.
    private void actualizarBoton() {
        int fila = tabla.getSelectedRow();
        btnInscribirme.setEnabled(fila >= 0 && modelo.getFila(fila).puedeInscribirse());
    }

    private void inscribirse() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            Dialogos.info(this, "Elige una institución",
                    "Haz clic en una institución de la tabla.");
            return;
        }
        InstitucionCatalogo inst = modelo.getFila(fila);
        boolean acepta = Dialogos.confirmar(this, "¿Inscribirte en esta institución?",
                "Te inscribirás en " + inst.getNombre() + ". Solo puedes inscribirte una vez.",
                "Sí, inscribirme");
        if (!acepta) {
            return;
        }
        try {
            inscripcionDAO.inscribir(Sesion.getUsuario().getIdUsuario(), inst.getIdInstitucion());
            Dialogos.exito(this, "Inscripción realizada",
                    "Ya estás inscrito en " + inst.getNombre() + ".");
            // TODO (integrante 3): abrir aquí el menú principal del alumno
            dispose();
        } catch (IllegalStateException ex) {
            Dialogos.aviso(this, "No se pudo inscribir", ex.getMessage());
            cargar();
        } catch (SQLException ex) {
            Dialogos.mostrarError(this, ex);
        }
    }
}