package vista.comun;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.LayoutManager;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * DISEÑO BASE DEL PROYECTO (opción E): colores, letra y fábrica de componentes.
 *
 * Uso en cualquier pantalla del equipo:
 *   1. Al iniciar la app (una sola vez, antes de abrir ventanas):  Estilo.aplicarTema();
 *   2. Fondo de las pantallas: Estilo.FONDO; tarjetas blancas: new Tarjeta();
 *   3. Botones: Estilo.botonPrincipal("Guardar"), Estilo.botonExito("Aprobar"),
 *      Estilo.botonSecundario("Cancelar"), Estilo.botonPeligro("Rechazar").
 *   4. Textos: Estilo.titulo(...), Estilo.subtitulo(...), Estilo.secundario(...).
 * Ver GUIA_DISENO.md.
 */
public final class Estilo {

    // ---------------- Paleta base ----------------
    public static final Color PRIMARIO = new Color(0x4338CA);
    public static final Color PRIMARIO_OSCURO = new Color(0x3730A3);
    public static final Color PRIMARIO_SUAVE = new Color(0xEEF2FF);
    public static final Color PRIMARIO_BORDE = new Color(0xA5B4FC);
    public static final Color FONDO = new Color(0xF5F6FA);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color BORDE = new Color(0xE3E5EE);
    public static final Color BORDE_FUERTE = new Color(0xDADDE8);
    public static final Color TEXTO = new Color(0x151826);
    public static final Color TEXTO_SECUNDARIO = new Color(0x5A5F73);
    public static final Color TEXTO_DESHABILITADO = new Color(0x9AA0B4);
    public static final Color DESHABILITADO_FONDO = new Color(0xECEEF3);
    public static final Color EXITO = new Color(0x15803D);
    public static final Color EXITO_SUAVE = new Color(0xECFDF3);
    public static final Color EXITO_BORDE = new Color(0xBBF0D0);
    public static final Color PELIGRO = new Color(0xB91C1C);

    // Nombres anteriores (se siguen usando en el código)
    public static final Color AZUL = PRIMARIO;
    public static final Color VERDE = EXITO;
    public static final Color ROJO = PELIGRO;

    // ---------------- Colores de estado {fondo, texto} ----------------
    public static final Color PENDIENTE_FONDO = new Color(0xFEF3C7);
    public static final Color PENDIENTE_TEXTO = new Color(0x92400E);
    public static final Color APROBADO_FONDO = new Color(0xDCFCE7);
    public static final Color APROBADO_TEXTO = new Color(0x166534);
    public static final Color RECHAZADO_FONDO = new Color(0xFEE2E2);
    public static final Color RECHAZADO_TEXTO = new Color(0x991B1B);
    public static final Color VALIDADA_FONDO = new Color(0xE0E7FF);
    public static final Color VALIDADA_TEXTO = new Color(0x3730A3);
    public static final Color NEUTRO_FONDO = new Color(0xF1F2F7);
    public static final Color NEUTRO_TEXTO = new Color(0x3D4257);

    private static final String[] FAMILIAS_PREFERIDAS = {"Segoe UI", "Inter", "Helvetica Neue", "Arial"};
    private static String familia;

    private Estilo() {
    }

    // ---------------- Tema ----------------

    /**
     * Aplica el diseño base a toda la app. Llamarlo UNA vez al iniciar, antes de crear
     * ventanas (en el main o en la pantalla de inicio de sesión).
     */
    public static void aplicarTema() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // Se queda la apariencia por defecto de Java
        }
        FontUIResource normal = new FontUIResource(fuente(Font.PLAIN, 14));
        for (Object clave : Collections.list(UIManager.getLookAndFeelDefaults().keys())) {
            if (clave.toString().endsWith(".font")) {
                UIManager.put(clave, normal);
            }
        }
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", SUPERFICIE);
        UIManager.put("OptionPane.messageForeground", TEXTO);
        UIManager.put("Table.selectionBackground", PRIMARIO_SUAVE);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("List.selectionBackground", PRIMARIO_SUAVE);
        UIManager.put("List.selectionForeground", TEXTO);
        UIManager.put("ScrollBar.width", 12);
    }

    /** Letra del proyecto: Segoe UI en Windows; si no existe, una parecida. */
    public static Font fuente(int estilo, float tamano) {
        if (familia == null) {
            String[] instaladas = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            familia = Arrays.stream(FAMILIAS_PREFERIDAS)
                    .filter(f -> Arrays.asList(instaladas).contains(f))
                    .findFirst().orElse(Font.SANS_SERIF);
        }
        return new Font(familia, estilo, Math.round(tamano));
    }

    // ---------------- Colores por significado ----------------

    /**
     * Colores {fondo, texto} para una etiqueta de estado, o null si no es un estado.
     * Amarillo = pendiente/solicitada, índigo = lista para subir/validada,
     * verde = aprobado/entregada, rojo = rechazado, gris = en curso.
     */
    public static Color[] coloresEstado(String etiqueta) {
        String e = etiqueta == null ? "" : etiqueta.toLowerCase();
        if (e.equals("pendiente") || e.equals("solicitada")) {
            return new Color[]{PENDIENTE_FONDO, PENDIENTE_TEXTO};
        }
        if (e.equals("validada") || e.startsWith("lista")) {
            return new Color[]{VALIDADA_FONDO, VALIDADA_TEXTO};
        }
        if (e.equals("aprobado") || e.equals("disponible") || e.equals("entregada") || e.equals("sí")) {
            return new Color[]{APROBADO_FONDO, APROBADO_TEXTO};
        }
        if (e.equals("rechazado") || e.equals("rechazada") || e.startsWith("no,")) {
            return new Color[]{RECHAZADO_FONDO, RECHAZADO_TEXTO};
        }
        if (e.equals("en curso")) {
            return new Color[]{NEUTRO_FONDO, NEUTRO_TEXTO};
        }
        return null;
    }

    /** Colores {fondo, texto} de la etiqueta del programa ('servicio_social' | 'practicas'). */
    public static Color[] coloresPrograma(String tipo) {
        return "practicas".equals(tipo)
                ? new Color[]{new Color(0xEDE9FE), new Color(0x5B21B6)}
                : new Color[]{new Color(0xE0F2FE), new Color(0x075985)};
    }

    // ---------------- Textos ----------------

    public static JLabel titulo(String texto) {
        return etiqueta(texto, fuente(Font.BOLD, 22), TEXTO);
    }

    public static JLabel subtitulo(String texto) {
        return etiqueta(texto, fuente(Font.BOLD, 15), TEXTO);
    }

    public static JLabel secundario(String texto) {
        return etiqueta(texto, fuente(Font.PLAIN, 13), TEXTO_SECUNDARIO);
    }

    public static JLabel etiqueta(String texto, Font fuente, Color color) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente);
        l.setForeground(color);
        return l;
    }

    // ---------------- Botones ----------------

    /** Acción principal de la pantalla (índigo). */
    public static Boton botonPrincipal(String texto) {
        return Boton.relleno(texto, PRIMARIO);
    }

    /** Botón relleno del color indicado (por ejemplo Estilo.VERDE para aprobar). */
    public static Boton botonPrincipal(String texto, Color color) {
        return Boton.relleno(texto, color);
    }

    /** Aprobar / confirmar algo positivo (verde). */
    public static Boton botonExito(String texto) {
        return Boton.relleno(texto, EXITO);
    }

    /** Acción secundaria (blanco con borde gris). */
    public static Boton botonSecundario(String texto) {
        return Boton.contorno(texto, TEXTO);
    }

    /** Rechazar / eliminar (blanco con borde rojo). */
    public static Boton botonPeligro(String texto) {
        return Boton.contorno(texto, PELIGRO);
    }

    /** Botón con borde del color indicado. */
    public static Boton botonContorno(String texto, Color color) {
        return Boton.contorno(texto, color);
    }

    // ---------------- Contenedores ----------------

    /** Panel transparente para acomodar cosas dentro de una Tarjeta. */
    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /** JScrollPane sin borde y con fondo blanco, para tablas y listas dentro de una Tarjeta. */
    public static JScrollPane scroll(Component contenido) {
        JScrollPane s = new JScrollPane(contenido);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.getViewport().setBackground(SUPERFICIE);
        s.getVerticalScrollBar().setUnitIncrement(16);
        return s;
    }

    /** Da a una tabla el estilo del proyecto: filas altas, líneas suaves, encabezado limpio. */
    public static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(42);
        tabla.setFont(fuente(Font.PLAIN, 14));
        tabla.setForeground(TEXTO);
        tabla.setBackground(SUPERFICIE);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEEF0F5));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(PRIMARIO_SUAVE);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFillsViewportHeight(true);
        // Swing reinicia el borde de la celda cada vez que la dibuja: el margen se pone después
        DefaultTableCellRenderer celdas = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, f, c);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
                return this;
            }
        };
        tabla.setDefaultRenderer(Object.class, celdas);
        DefaultTableCellRenderer encabezado = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foco, int f, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(), false, false, f, c);
                l.setFont(fuente(Font.BOLD, 12));
                l.setForeground(TEXTO_SECUNDARIO);
                l.setBackground(new Color(0xF7F8FB));
                l.setOpaque(true);
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                        BorderFactory.createEmptyBorder(0, 14, 0, 14)));
                return l;
            }
        };
        tabla.getTableHeader().setDefaultRenderer(encabezado);
        tabla.getTableHeader().setPreferredSize(new Dimension(10, 38));
        tabla.getTableHeader().setReorderingAllowed(false);
    }

    /** Margen interior estándar. */
    public static void margen(JComponent c, int arriba, int izquierda, int abajo, int derecha) {
        c.setBorder(BorderFactory.createEmptyBorder(arriba, izquierda, abajo, derecha));
    }

    /** Hace que un botón normal de Swing se vea como enlace. */
    public static JButton enlace(String texto) {
        return Boton.enlace(texto);
    }
}
