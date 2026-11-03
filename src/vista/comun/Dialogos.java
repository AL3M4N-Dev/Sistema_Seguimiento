package vista.comun;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import servicio.ReglaNegocioException;
import vista.comun.VentanaDialogo.Tipo;

/**
 * Avisos y preguntas del programa, todos con el diseño base (ver VentanaDialogo).
 *
 *   Dialogos.exito(this, "Constancia aprobada", "Daniela ya puede descargarla.");
 *   Dialogos.info(this, "Ningún registro seleccionado", "Haz clic en un registro de la tabla.");
 *   Dialogos.aviso(this, "Revisa esto", "La hora de fin debe ser después de la de inicio.");
 *   Dialogos.mostrarError(this, excepcion);
 *
 *   if (Dialogos.confirmar(this, "¿Enviar tu solicitud?", "Se enviará al responsable.", "Enviar solicitud")) …
 *   if (Dialogos.confirmarRechazo(this, "¿Rechazar la institución?", "…", "Rechazar")) …
 *   String motivo = Dialogos.pedirMotivoRechazo(this, "Rechazar solicitud", "¿Por qué…?", "Rechazar solicitud");
 */
public final class Dialogos {

    private static final int ANCHO_COMENTARIO = 540;
    private static final int MAX_COMENTARIO = 500;

    private Dialogos() {
    }

    // ------------------------------------------------------------------
    // Avisos con un solo botón
    // ------------------------------------------------------------------

    /** Algo salió bien (palomita verde). */
    public static void exito(Component padre, String titulo, String mensaje) {
        avisar(padre, Tipo.EXITO, titulo, mensaje, null);
    }

    /** Algo salió bien; 'detalle' va en una caja gris que se puede copiar (por ejemplo, una ruta). */
    public static void exito(Component padre, String titulo, String mensaje, String detalle) {
        avisar(padre, Tipo.EXITO, titulo, mensaje, detalle);
    }

    /** Información o indicación (i índigo). */
    public static void info(Component padre, String titulo, String mensaje) {
        avisar(padre, Tipo.INFO, titulo, mensaje, null);
    }

    public static void info(Component padre, String titulo, String mensaje, String detalle) {
        avisar(padre, Tipo.INFO, titulo, mensaje, detalle);
    }

    /** Falta algo o no se cumple una regla (! amarillo). */
    public static void aviso(Component padre, String titulo, String mensaje) {
        avisar(padre, Tipo.AVISO, titulo, mensaje, null);
    }

    public static void aviso(Component padre, String titulo, String mensaje, String detalle) {
        avisar(padre, Tipo.AVISO, titulo, mensaje, detalle);
    }

    /** Falló algo (! rojo). 'detalle' es el texto técnico del error (puede ser null). */
    public static void error(Component padre, String titulo, String mensaje, String detalle) {
        avisar(padre, Tipo.ERROR, titulo, mensaje, detalle);
    }

    /** Una regla no cumplida se muestra como aviso; cualquier otro error, como error. */
    public static void mostrarError(Component padre, Exception e) {
        if (e instanceof ReglaNegocioException) {
            aviso(padre, "Revisa esto", e.getMessage());
            return;
        }
        e.printStackTrace();
        if (e instanceof SQLException) {
            error(padre, "Problema con la base de datos",
                    "No se pudo completar la acción. Revisa que MySQL esté encendido en XAMPP y vuelve a intentarlo.",
                    e.getMessage());
        } else if (e instanceof IOException) {
            error(padre, "Problema con el archivo",
                    "No se pudo leer o guardar el archivo. Revisa que exista y que no esté abierto en otro programa.",
                    e.getMessage());
        } else {
            error(padre, "Algo salió mal", "No se pudo completar la acción.", e.getMessage());
        }
    }

    /** Versión corta: aviso informativo con título "Aviso". */
    public static void info(Component padre, String mensaje) {
        info(padre, "Aviso", mensaje);
    }

    // ------------------------------------------------------------------
    // Preguntas Sí / Cancelar
    // ------------------------------------------------------------------

    /** Pregunta normal (botón índigo). Devuelve true si el usuario acepta. */
    public static boolean confirmar(Component padre, String titulo, String mensaje, String textoSi) {
        return preguntar(padre, Tipo.PREGUNTA, titulo, mensaje, null, Estilo.botonPrincipal(textoSi), false);
    }

    /** Pregunta antes de aprobar algo (botón verde). 'detalle' puede ser null. */
    public static boolean confirmarAprobar(Component padre, String titulo, String mensaje, String detalle, String textoSi) {
        Tipo icono = detalle != null && !detalle.isBlank() ? Tipo.DOCUMENTO : Tipo.EXITO;
        return preguntar(padre, icono, titulo, mensaje, detalle, Estilo.botonExito(textoSi), false);
    }

    /** Pregunta antes de rechazar o eliminar (botón rojo). El foco empieza en Cancelar. */
    public static boolean confirmarRechazo(Component padre, String titulo, String mensaje, String textoSi) {
        return preguntar(padre, Tipo.RECHAZO, titulo, mensaje, null, Estilo.botonPrincipal(textoSi, Estilo.PELIGRO), true);
    }

    /** Versión corta: "¿Continuar?" con el mensaje indicado. */
    public static boolean confirmar(Component padre, String mensaje) {
        return confirmar(padre, "¿Continuar?", mensaje, "Sí, continuar");
    }

    // ------------------------------------------------------------------
    // Pedir un texto
    // ------------------------------------------------------------------

    /**
     * Pide el motivo de un rechazo. El comentario es obligatorio: si está vacío, el campo se
     * marca en rojo y la ventana no se cierra. Devuelve el texto, o null si el usuario cancela.
     */
    public static String pedirMotivoRechazo(Component padre, String titulo, String mensaje, String textoBoton) {
        return pedirMotivoRechazo(padre, titulo, mensaje, textoBoton, "Escribe aquí el motivo");
    }

    /** Igual, con un ejemplo gris dentro del campo (por ejemplo "Ej. Falta la firma del responsable"). */
    public static String pedirMotivoRechazo(Component padre, String titulo, String mensaje, String textoBoton,
                                            String ejemplo) {
        return pedirTexto(padre, Tipo.RECHAZO, titulo, mensaje,
                Estilo.botonPrincipal(textoBoton, Estilo.PELIGRO), true, ejemplo);
    }

    /** Pide un comentario opcional. Devuelve el texto (puede estar vacío), o null si el usuario cancela. */
    public static String pedirComentario(Component padre, String titulo, String mensaje) {
        return pedirTexto(padre, Tipo.INFO, titulo, mensaje, Estilo.botonPrincipal("Guardar"), false, null);
    }

    // ------------------------------------------------------------------
    // Elegir de una lista
    // ------------------------------------------------------------------

    /**
     * Muestra una lista para elegir una opción (con iniciales, nombre y un detalle gris).
     * Doble clic o Enter eligen. Devuelve la opción elegida, o null si el usuario cancela.
     */
    public static <T> T elegir(Component padre, String titulo, String mensaje, List<T> opciones,
                               Function<T, String> nombre, Function<T, String> detalle, String textoBoton) {
        VentanaDialogo v = new VentanaDialogo(padre, titulo, mensaje, null);
        v.setAncho(520);
        v.setMargenContenido(14, 24, 22, 24);

        JList<T> lista = new JList<>(new Vector<>(opciones));
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new FilaOpcion<>(nombre, detalle));
        lista.setVisibleRowCount(Math.max(1, Math.min(opciones.size(), 5)));
        lista.setSelectedIndex(0);
        lista.getAccessibleContext().setAccessibleName(titulo);

        Tarjeta caja = new Tarjeta(new BorderLayout(), Estilo.SUPERFICIE, Estilo.BORDE_FUERTE, 10);
        caja.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        caja.add(Estilo.scroll(lista), BorderLayout.CENTER);
        v.setContenido(caja);

        Object[] elegido = {null};
        v.agregarBotonCancelar("Cancelar");
        Boton aceptar = v.agregarBoton(Estilo.botonPrincipal(textoBoton));
        aceptar.addActionListener(e -> {
            elegido[0] = lista.getSelectedValue();
            if (elegido[0] != null) {
                v.cerrar();
            }
        });
        lista.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && lista.locationToIndex(e.getPoint()) >= 0) {
                    aceptar.doClick();
                }
            }
        });
        v.setBotonPorDefecto(aceptar);
        v.setFocoInicial(lista);
        v.mostrar();
        @SuppressWarnings("unchecked")
        T resultado = (T) elegido[0];
        return resultado;
    }

    // ------------------------------------------------------------------
    // Construcción (también la usan las pruebas visuales)
    // ------------------------------------------------------------------

    private static void avisar(Component padre, Tipo tipo, String titulo, String mensaje, String detalle) {
        VentanaDialogo v = new VentanaDialogo(padre, titulo, mensaje, tipo);
        if (detalle != null && !detalle.isBlank()) {
            v.setContenido(cajaDetalle(detalle, v.getMarco().anchoContenido()));
        }
        Boton aceptar = v.agregarBoton(Estilo.botonPrincipal("Aceptar"));
        aceptar.addActionListener(e -> v.cerrar());
        v.setBotonPorDefecto(aceptar);
        v.mostrar();
    }

    private static boolean preguntar(Component padre, Tipo tipo, String titulo, String mensaje, String detalle,
                                     Boton botonSi, boolean focoEnCancelar) {
        VentanaDialogo v = new VentanaDialogo(padre, titulo, mensaje, tipo);
        if (detalle != null && !detalle.isBlank()) {
            v.setContenido(cajaDetalle(detalle, v.getMarco().anchoContenido()));
        }
        boolean[] acepto = {false};
        Boton cancelar = v.agregarBotonCancelar("Cancelar");
        v.agregarBoton(botonSi);
        botonSi.addActionListener(e -> {
            acepto[0] = true;
            v.cerrar();
        });
        v.setBotonPorDefecto(focoEnCancelar ? cancelar : botonSi);
        v.mostrar();
        return acepto[0];
    }

    private static String pedirTexto(Component padre, Tipo tipo, String titulo, String mensaje, Boton botonSi,
                                     boolean obligatorio, String ejemplo) {
        VentanaDialogo v = new VentanaDialogo(padre, titulo, mensaje, tipo);
        v.setAncho(ANCHO_COMENTARIO);

        AreaTexto campo = new AreaTexto(4, MAX_COMENTARIO);
        campo.setPlaceholder(ejemplo);
        campo.setNombreAccesible(titulo);
        campo.setAyuda(obligatorio ? "Obligatorio · Ctrl + Enter para enviar" : "Ctrl + Enter para guardar");
        v.setContenido(campo);

        String[] resultado = {null};
        v.agregarBotonCancelar("Cancelar");
        v.agregarBoton(botonSi);
        botonSi.addActionListener(e -> {
            String texto = campo.getTexto().trim();
            if (obligatorio && texto.isEmpty()) {
                campo.setError("Escribe el motivo; el alumno lo leerá.");
                campo.requestFocusInWindow();
                return;
            }
            resultado[0] = texto;
            v.cerrar();
        });
        campo.getAreaTexto().getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "enviar");
        campo.getAreaTexto().getActionMap().put("enviar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                botonSi.doClick();
            }
        });
        v.setFocoInicial(campo.getAreaTexto());
        v.mostrar();
        return resultado[0];
    }

    /** Caja gris con texto que se puede seleccionar y copiar (rutas, detalle técnico). */
    static JComponent cajaDetalle(String texto, int ancho) {
        Tarjeta caja = new Tarjeta(new BorderLayout(), Estilo.FONDO, Estilo.BORDE, 10);
        caja.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        TextoAjustado t = new TextoAjustado(texto, Estilo.fuente(Font.PLAIN, 13), Estilo.TEXTO, true);
        t.setAnchoAjuste(Math.max(120, ancho - 24));
        caja.add(t, BorderLayout.CENTER);
        return caja;
    }

    /** Fila de la lista de Dialogos.elegir: iniciales, nombre y detalle. */
    private static class FilaOpcion<T> extends JPanel implements ListCellRenderer<T> {
        private final Function<T, String> nombre;
        private final Function<T, String> detalle;
        private final Avatar avatar = new Avatar("", 36);
        private final JLabel textoNombre = Estilo.etiqueta("", Estilo.fuente(Font.BOLD, 14), Estilo.TEXTO);
        private final JLabel textoDetalle = Estilo.etiqueta("", Estilo.fuente(Font.PLAIN, 12), Estilo.TEXTO_SECUNDARIO);
        private boolean seleccionada;

        FilaOpcion(Function<T, String> nombre, Function<T, String> detalle) {
            super(new BorderLayout(12, 0));
            this.nombre = nombre;
            this.detalle = detalle;
            setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
            JPanel textos = Estilo.panel(new BorderLayout(0, 2));
            textos.add(textoNombre, BorderLayout.NORTH);
            textos.add(textoDetalle, BorderLayout.SOUTH);
            JPanel contenedorAvatar = Estilo.panel(new BorderLayout());
            contenedorAvatar.add(avatar, BorderLayout.CENTER);
            add(contenedorAvatar, BorderLayout.WEST);
            add(textos, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends T> lista, T valor, int indice,
                                                      boolean sel, boolean foco) {
            seleccionada = sel;
            String n = valor == null ? "" : nombre.apply(valor);
            String d = valor == null || detalle == null ? "" : detalle.apply(valor);
            avatar.setNombre(n);
            avatar.setColores(sel ? Estilo.VALIDADA_FONDO : new Color(0xEEF0F7), sel ? Estilo.PRIMARIO_OSCURO : Estilo.NEUTRO_TEXTO);
            textoNombre.setText(n);
            textoDetalle.setText(d == null ? "" : d);
            getAccessibleContext().setAccessibleName(n + (d == null || d.isEmpty() ? "" : ", " + d));
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
