package vista.comun;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

/**
 * VENTANA EMERGENTE DEL DISEÑO BASE. Todas las ventanas que se abren encima de otra
 * (avisos, preguntas, detalles) usan esta clase para verse igual que el programa:
 * tarjeta blanca con esquinas redondeadas y sombra, encabezado propio (sin la barra de
 * Windows), botones del proyecto y un velo oscuro sobre la ventana de atrás.
 *
 *   VentanaDialogo v = new VentanaDialogo(this, "Añadir horas", "Llena los datos de tu turno");
 *   v.setContenido(miFormulario);
 *   v.agregarBotonCancelar("Cancelar");
 *   Boton guardar = v.agregarBoton(Estilo.botonPrincipal("Guardar"));
 *   guardar.addActionListener(e -> { ...; v.cerrar(); });
 *   v.setBotonPorDefecto(guardar);
 *   v.mostrar();                       // espera a que se cierre
 *
 * Para avisos y preguntas rápidas no hace falta usarla directo: usa la clase Dialogos.
 * Teclado: Esc cierra, Enter usa el botón por defecto. Se mueve arrastrando el encabezado.
 */
public class VentanaDialogo extends JDialog {

    /**
     * Icono de color que va a la izquierda del título:
     * EXITO palomita verde · INFO i índigo · AVISO ! amarillo · ERROR ! rojo ·
     * PREGUNTA ? índigo · RECHAZO ✕ rojo · DOCUMENTO hoja verde (aprobar un archivo).
     */
    public enum Tipo { EXITO, INFO, AVISO, ERROR, PREGUNTA, RECHAZO, DOCUMENTO }

    /** Ancho estándar de los avisos y preguntas. */
    public static final int ANCHO_MENSAJE = 480;

    private final Marco marco;
    private Runnable accionCancelar = this::cerrar;
    private JComponent focoInicial;
    private JRootPane raizVelada;
    private Component glassAnterior;
    private boolean glassAnteriorVisible;

    public VentanaDialogo(Component padre, String titulo) {
        this(padre, titulo, null, null);
    }

    public VentanaDialogo(Component padre, String titulo, String subtitulo) {
        this(padre, titulo, subtitulo, null);
    }

    /**
     * @param padre     componente de la ventana de atrás (puede ser null)
     * @param titulo    título en negritas
     * @param subtitulo texto gris debajo del título; se acomoda en varias líneas (puede ser null)
     * @param tipo      icono de color a la izquierda (puede ser null)
     */
    public VentanaDialogo(Component padre, String titulo, String subtitulo, Tipo tipo) {
        super(ventanaDe(padre), titulo, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        boolean conSombra = conSombra();
        if (conSombra) {
            try {
                setBackground(new Color(0, 0, 0, 0));
                getRootPane().setOpaque(false);
            } catch (UnsupportedOperationException e) {
                conSombra = false;      // la pantalla no permite ventanas transparentes: queda cuadrada
            }
        }
        marco = new Marco(titulo, subtitulo, tipo == null ? null : new IconoAviso(tipo), conSombra);
        marco.alCerrar(() -> accionCancelar.run());
        setContentPane(marco);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                accionCancelar.run();     // Alt + F4
            }

            @Override
            public void windowOpened(WindowEvent e) {
                JComponent foco = focoInicial != null ? focoInicial : getRootPane().getDefaultButton();
                if (foco != null) {
                    SwingUtilities.invokeLater(foco::requestFocusInWindow);
                }
            }
        });
        getRootPane().registerKeyboardAction(e -> accionCancelar.run(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getAccessibleContext().setAccessibleName(titulo);
        if (subtitulo != null) {
            getAccessibleContext().setAccessibleDescription(subtitulo);
        }
    }

    // ------------------------------------------------------------------
    // Configuración
    // ------------------------------------------------------------------

    /** Lo que va en medio de la ventana (formulario, tabla, imagen…). */
    public void setContenido(JComponent contenido) {
        marco.setContenido(contenido);
    }

    /** Margen alrededor del contenido. Por defecto queda alineado con el texto del encabezado. */
    public void setMargenContenido(int arriba, int izquierda, int abajo, int derecha) {
        marco.setMargenContenido(new Insets(arriba, izquierda, abajo, derecha));
    }

    /** Color de fondo de la parte del contenido (por ejemplo Estilo.FONDO para una imagen). */
    public void setFondoContenido(Color fondo) {
        marco.setFondoContenido(fondo);
    }

    /** Línea entre el encabezado y el contenido (para ventanas grandes con tablas o formularios). */
    public void setSeparador(boolean separador) {
        marco.setSeparador(separador);
    }

    /** Cambia el icono del encabezado por otro componente (por ejemplo, un Avatar). */
    public void setIcono(JComponent icono) {
        marco.setIcono(icono);
    }

    /** Algo pequeño a la izquierda de los botones (una pastilla de estado, un texto de ayuda…). */
    public void setPie(JComponent izquierda) {
        marco.setPie(izquierda);
    }

    /** Agrega un botón abajo a la derecha (en el orden en que se agregan). */
    public Boton agregarBoton(Boton boton) {
        marco.agregarBoton(boton);
        return boton;
    }

    /** Agrega un botón secundario que cierra la ventana (hace lo mismo que Esc y la X). */
    public Boton agregarBotonCancelar(String texto) {
        Boton b = Estilo.botonSecundario(texto);
        b.addActionListener(e -> accionCancelar.run());
        return agregarBoton(b);
    }

    /** Botón que se presiona con Enter. */
    public void setBotonPorDefecto(JButton boton) {
        getRootPane().setDefaultButton(boton);
    }

    /** Componente que recibe el foco al abrir (si no se indica, el botón por defecto). */
    public void setFocoInicial(JComponent componente) {
        this.focoInicial = componente;
    }

    /** Qué pasa con Esc, la X o Alt + F4. Por defecto solo cierra. */
    public void setAccionCancelar(Runnable accion) {
        this.accionCancelar = accion;
    }

    /** Ancho de la tarjeta; el alto se calcula solo. */
    public void setAncho(int ancho) {
        marco.setTamanoTarjeta(ancho, -1);
    }

    /** Tamaño fijo de la tarjeta (si no cabe en la pantalla, se reduce). */
    public void setTamano(int ancho, int alto) {
        marco.setTamanoTarjeta(ancho, alto);
    }

    public Marco getMarco() {
        return marco;
    }

    // ------------------------------------------------------------------
    // Mostrar y cerrar
    // ------------------------------------------------------------------

    /** Muestra la ventana centrada sobre la de atrás y espera a que se cierre. */
    public void mostrar() {
        pack();
        ajustarAPantalla();
        setLocationRelativeTo(getOwner());
        ponerVelo();
        try {
            setVisible(true);
        } finally {
            quitarVelo();
            devolverFoco();
        }
    }

    /** Al cerrar, la ventana de atrás vuelve a recibir el teclado (para que Esc y Enter sigan funcionando). */
    private void devolverFoco() {
        Window atras = getOwner();
        if (atras != null && atras.isShowing() && !atras.isFocused()) {
            atras.toFront();
            atras.requestFocus();
        }
    }

    public void cerrar() {
        dispose();
    }

    @Override
    public void dispose() {
        quitarVelo();
        super.dispose();
    }

    private void ajustarAPantalla() {
        Window base = getOwner() != null && getOwner().isShowing() ? getOwner() : this;
        GraphicsConfiguration gc = base.getGraphicsConfiguration();
        if (gc == null) {
            return;
        }
        Rectangle area = gc.getBounds();
        Insets bordes = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int maxAncho = area.width - bordes.left - bordes.right;
        int maxAlto = area.height - bordes.top - bordes.bottom;
        if (getWidth() > maxAncho || getHeight() > maxAlto) {
            setSize(Math.min(getWidth(), maxAncho), Math.min(getHeight(), maxAlto));
        }
    }

    /** Oscurece la ventana de atrás mientras esta está abierta. */
    private void ponerVelo() {
        Window atras = getOwner();
        if (!(atras instanceof RootPaneContainer) || !atras.isShowing() || raizVelada != null) {
            return;
        }
        JRootPane raiz = ((RootPaneContainer) atras).getRootPane();
        Shape forma = atras instanceof VentanaDialogo
                ? ((VentanaDialogo) atras).marco.formaTarjetaEn(raiz) : null;
        glassAnterior = raiz.getGlassPane();
        glassAnteriorVisible = glassAnterior.isVisible();
        Velo velo = new Velo(forma);
        raiz.setGlassPane(velo);
        velo.setVisible(true);
        raizVelada = raiz;
    }

    private void quitarVelo() {
        if (raizVelada == null) {
            return;
        }
        raizVelada.getGlassPane().setVisible(false);
        raizVelada.setGlassPane(glassAnterior);
        glassAnterior.setVisible(glassAnteriorVisible);
        raizVelada.repaint();
        raizVelada = null;
    }

    private static Window ventanaDe(Component c) {
        if (c == null) {
            return null;
        }
        return c instanceof Window ? (Window) c : SwingUtilities.getWindowAncestor(c);
    }

    /**
     * Esquinas redondeadas y sombra en Windows y Mac. En Linux queda cuadrada (con borde).
     * Se puede apagar con -Dvista.dialogos.sombra=no, o encender en Linux con =si.
     */
    private static boolean conSombra() {
        String forzar = System.getProperty("vista.dialogos.sombra", "");
        if (forzar.equalsIgnoreCase("no")) {
            return false;
        }
        // En Linux algunas pantallas sin efectos dibujan negro lo transparente: ahí queda cuadrada
        String sistema = System.getProperty("os.name", "").toLowerCase();
        boolean sistemaSeguro = sistema.contains("win") || sistema.contains("mac");
        try {
            GraphicsDevice pantalla = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            boolean soporta = pantalla.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
            return soporta && (sistemaSeguro || forzar.equalsIgnoreCase("si"));
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ==================================================================
    // Marco: todo lo que se ve (se puede dibujar sin abrir la ventana)
    // ==================================================================

    /** Sombra + tarjeta con encabezado, contenido y pie con botones. */
    public static class Marco extends JPanel {

        private static final int CAPAS_SOMBRA = 16;
        private static final int CAIDA_SOMBRA = 5;
        private static final Color FONDO_PIE = new Color(0xF7F8FB);
        private static final int MARGEN_LADOS = 24;
        private static final int SEPARACION_ICONO = 14;

        private final boolean sombra;
        private final int radio;
        private final Tarjeta tarjeta = new Tarjeta();
        private final Encabezado encabezado;
        private final JPanel cuerpo = Estilo.panel(new BorderLayout());
        private final JPanel pie = Estilo.panel(new BorderLayout(16, 0));
        private final JPanel botones = Estilo.panel(null);
        private JComponent contenido;
        private Insets margenContenido;     // null = automático
        private Color fondoContenido;
        private boolean separador;
        private int anchoTarjeta = ANCHO_MENSAJE;
        private int altoTarjeta = -1;

        public Marco(String titulo, String subtitulo, JComponent icono, boolean sombra) {
            super(new BorderLayout());
            this.sombra = sombra;
            this.radio = sombra ? 16 : 0;
            setOpaque(false);
            setBorder(sombra
                    ? BorderFactory.createEmptyBorder(CAPAS_SOMBRA - CAIDA_SOMBRA, CAPAS_SOMBRA,
                            CAPAS_SOMBRA + CAIDA_SOMBRA, CAPAS_SOMBRA)
                    : BorderFactory.createEmptyBorder());

            encabezado = new Encabezado(titulo, subtitulo, icono);
            botones.setLayout(new BoxLayout(botones, BoxLayout.X_AXIS));
            pie.setBorder(BorderFactory.createEmptyBorder(14, MARGEN_LADOS, 14, 20));
            pie.add(botones, BorderLayout.EAST);
            pie.setVisible(false);
            cuerpo.setVisible(false);

            tarjeta.add(encabezado, BorderLayout.NORTH);
            tarjeta.add(cuerpo, BorderLayout.CENTER);
            tarjeta.add(pie, BorderLayout.SOUTH);
            add(tarjeta, BorderLayout.CENTER);
            actualizar();
        }

        void alCerrar(Runnable accion) {
            encabezado.cerrar.addActionListener(e -> accion.run());
        }

        public void setContenido(JComponent c) {
            if (contenido != null) {
                cuerpo.remove(contenido);
            }
            contenido = c;
            if (c != null) {
                cuerpo.add(c, BorderLayout.CENTER);
            }
            actualizar();
        }

        public void setMargenContenido(Insets margen) {
            this.margenContenido = margen;
            actualizar();
        }

        public void setFondoContenido(Color fondo) {
            this.fondoContenido = fondo;
            repaint();
        }

        public void setSeparador(boolean separador) {
            this.separador = separador;
            actualizar();
        }

        public void setIcono(JComponent icono) {
            encabezado.setIcono(icono);
            actualizar();
        }

        public void setPie(JComponent izquierda) {
            JPanel centrado = Estilo.panel(new java.awt.GridBagLayout());   // para que no se estire a lo alto
            centrado.add(izquierda);
            pie.add(centrado, BorderLayout.WEST);
            pie.setVisible(true);
            revalidate();
        }

        public void agregarBoton(Boton boton) {
            if (botones.getComponentCount() > 0) {
                botones.add(Box.createHorizontalStrut(10));
            }
            botones.add(boton);
            pie.setVisible(true);
            revalidate();
        }

        /** Tamaño de la tarjeta blanca (sin contar la sombra). alto = -1 para calcularlo solo. */
        public void setTamanoTarjeta(int ancho, int alto) {
            this.anchoTarjeta = ancho;
            this.altoTarjeta = alto;
            actualizar();
        }

        /** Ancho del texto del encabezado (para alinear lo de abajo con él). */
        int columnaTexto() {
            return anchoTarjeta - MARGEN_LADOS - encabezado.anchoFijo();
        }

        /** Margen izquierdo donde empieza el texto del encabezado. */
        int inicioTexto() {
            return MARGEN_LADOS + encabezado.anchoIcono();
        }

        private void actualizar() {
            boolean hayContenido = contenido != null;
            cuerpo.setVisible(hayContenido);
            encabezado.setBorder(BorderFactory.createEmptyBorder(18, MARGEN_LADOS,
                    separador ? 16 : hayContenido ? 4 : 22, 12));
            encabezado.ajustarTexto(columnaTexto());
            Insets m = margenActual();
            cuerpo.setBorder(BorderFactory.createEmptyBorder(m.top, m.left, m.bottom, m.right));
            revalidate();
            repaint();
        }

        private Insets margenActual() {
            return margenContenido != null ? margenContenido
                    : new Insets(separador ? 18 : 12, inicioTexto(), 22, MARGEN_LADOS);
        }

        /** Ancho disponible para el contenido (la tarjeta menos los márgenes). */
        public int anchoContenido() {
            Insets m = margenActual();
            return anchoTarjeta - m.left - m.right;
        }

        /** Forma de la tarjeta en las coordenadas de 'destino' (para el velo). */
        Shape formaTarjetaEn(Component destino) {
            Rectangle r = SwingUtilities.convertRectangle(this, tarjeta.getBounds(), destino);
            return new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, radio, radio);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!sombra) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle r = tarjeta.getBounds();
            g2.setColor(new Color(21, 24, 38, 5));
            for (int i = CAPAS_SOMBRA; i >= 1; i--) {
                float crecer = i;
                g2.fill(new RoundRectangle2D.Float(r.x - crecer, r.y - crecer + CAIDA_SOMBRA,
                        r.width + 2 * crecer, r.height + 2 * crecer, radio + 2 * crecer, radio + 2 * crecer));
            }
            g2.dispose();
        }

        /** La tarjeta blanca: pinta también la franja gris del pie y la línea del separador. */
        private class Tarjeta extends JPanel {
            Tarjeta() {
                super(new BorderLayout());
                setOpaque(false);
            }

            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(anchoTarjeta > 0 ? anchoTarjeta : d.width, altoTarjeta > 0 ? altoTarjeta : d.height);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Shape forma = radio > 0 ? new RoundRectangle2D.Float(0, 0, w, h, radio, radio) : new Rectangle(0, 0, w, h);
                g2.setColor(Estilo.SUPERFICIE);
                g2.fill(forma);
                Shape antes = g2.getClip();
                g2.clip(forma);
                if (fondoContenido != null && cuerpo.isVisible()) {
                    g2.setColor(fondoContenido);
                    g2.fill(cuerpo.getBounds());
                }
                if (separador) {
                    g2.setColor(Estilo.BORDE);
                    g2.fillRect(0, encabezado.getY() + encabezado.getHeight() - 1, w, 1);
                }
                if (pie.isVisible()) {
                    int y = pie.getY();
                    g2.setColor(FONDO_PIE);
                    g2.fillRect(0, y, w, h - y);
                    g2.setColor(Estilo.BORDE);
                    g2.fillRect(0, y, w, 1);
                }
                g2.setClip(antes);
                g2.setColor(sombra ? new Color(0xD5D8E3) : new Color(0xB9BECD));
                if (radio > 0) {
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, radio, radio));
                } else {
                    g2.drawRect(0, 0, w - 1, h - 1);
                }
                g2.dispose();
            }
        }
    }

    // ==================================================================
    // Piezas internas
    // ==================================================================

    /** Icono (opcional) + título + subtítulo + botón X. Se arrastra para mover la ventana. */
    private static class Encabezado extends JPanel {
        private static final int SEPARACION = 14;

        final BotonCerrar cerrar = new BotonCerrar();
        private final JPanel contenedorIcono = Estilo.panel(new BorderLayout());
        private final TextoAjustado titulo;
        private final TextoAjustado subtitulo;
        private JComponent icono;

        Encabezado(String textoTitulo, String textoSubtitulo, JComponent icono) {
            super(new BorderLayout(SEPARACION, 0));
            setOpaque(false);

            JPanel textos = Estilo.panel(null);
            textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
            titulo = new TextoAjustado(textoTitulo, Estilo.fuente(Font.BOLD, 17), Estilo.TEXTO, false);
            textos.add(Box.createVerticalStrut(icono instanceof IconoAviso ? 1 : 0));
            textos.add(titulo);
            if (textoSubtitulo != null && !textoSubtitulo.isEmpty()) {
                subtitulo = new TextoAjustado(textoSubtitulo, Estilo.fuente(Font.PLAIN, 14), Estilo.TEXTO_SECUNDARIO, false);
                textos.add(Box.createVerticalStrut(4));
                textos.add(subtitulo);
            } else {
                subtitulo = null;
            }

            JPanel contenedorCerrar = Estilo.panel(new BorderLayout());
            contenedorCerrar.add(cerrar, BorderLayout.NORTH);

            add(textos, BorderLayout.CENTER);
            add(contenedorCerrar, BorderLayout.EAST);
            setIcono(icono);

            MouseAdapter arrastre = new Arrastre();
            for (Component c : new Component[]{this, textos, titulo, subtitulo, contenedorIcono}) {
                if (c != null) {
                    c.addMouseListener(arrastre);
                    c.addMouseMotionListener(arrastre);
                }
            }
        }

        void setIcono(JComponent nuevo) {
            if (icono != null) {
                contenedorIcono.remove(icono);
            }
            icono = nuevo;
            if (nuevo != null) {
                contenedorIcono.add(nuevo, BorderLayout.NORTH);
                if (contenedorIcono.getParent() == null) {
                    add(contenedorIcono, BorderLayout.WEST);
                }
            } else if (contenedorIcono.getParent() != null) {
                remove(contenedorIcono);
            }
        }

        int anchoIcono() {
            return icono == null ? 0 : icono.getPreferredSize().width + SEPARACION;
        }

        /** Todo lo que no es texto: icono, botón X, márgenes. */
        int anchoFijo() {
            return anchoIcono() + SEPARACION + cerrar.getPreferredSize().width + 12;
        }

        void ajustarTexto(int ancho) {
            titulo.setAnchoAjuste(Math.max(120, ancho));
            if (subtitulo != null) {
                subtitulo.setAnchoAjuste(Math.max(120, ancho));
            }
        }

        private class Arrastre extends MouseAdapter {
            private Point inicio;
            private Point origen;

            @Override
            public void mousePressed(MouseEvent e) {
                Window w = SwingUtilities.getWindowAncestor(Encabezado.this);
                if (w != null && SwingUtilities.isLeftMouseButton(e)) {
                    inicio = e.getLocationOnScreen();
                    origen = w.getLocation();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Window w = SwingUtilities.getWindowAncestor(Encabezado.this);
                if (w != null && inicio != null) {
                    Point p = e.getLocationOnScreen();
                    w.setLocation(origen.x + p.x - inicio.x, origen.y + p.y - inicio.y);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                inicio = null;
            }
        }
    }

    /** La X de arriba a la derecha. */
    private static class BotonCerrar extends JButton {
        BotonCerrar() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Cerrar (Esc)");
            getAccessibleContext().setAccessibleName("Cerrar");
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(32, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean encima = getModel().isRollover();
            if (encima || getModel().isPressed()) {
                g2.setColor(getModel().isPressed() ? new Color(0xE4E6EE) : Estilo.NEUTRO_FONDO);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
            g2.setColor(encima ? Estilo.TEXTO : Estilo.TEXTO_SECUNDARIO);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int c = getWidth() / 2;
            int m = getHeight() / 2;
            g2.drawLine(c - 5, m - 5, c + 5, m + 5);
            g2.drawLine(c + 5, m - 5, c - 5, m + 5);
            g2.dispose();
        }
    }

    /** Círculo de color con un símbolo dibujado (palomita, !, ?, i, ✕ o una hoja). */
    static class IconoAviso extends JComponent {
        private static final int TAMANO = 40;
        private final Tipo tipo;

        IconoAviso(Tipo tipo) {
            this.tipo = tipo;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(TAMANO, TAMANO);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color fondo;
            Color tinta;
            switch (tipo) {
                case EXITO, DOCUMENTO -> { fondo = Estilo.APROBADO_FONDO; tinta = Estilo.EXITO; }
                case AVISO -> { fondo = Estilo.PENDIENTE_FONDO; tinta = new Color(0xB45309); }
                case ERROR, RECHAZO -> { fondo = Estilo.RECHAZADO_FONDO; tinta = Estilo.PELIGRO; }
                default -> { fondo = Estilo.VALIDADA_FONDO; tinta = Estilo.PRIMARIO; }
            }
            g2.setColor(fondo);
            g2.fillOval(0, 0, TAMANO, TAMANO);
            g2.setColor(tinta);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float c = TAMANO / 2f;
            switch (tipo) {
                case EXITO -> {
                    Path2D p = new Path2D.Float();
                    p.moveTo(c - 7.5f, c + 0.5f);
                    p.lineTo(c - 2.5f, c + 5.5f);
                    p.lineTo(c + 8f, c - 5.5f);
                    g2.draw(p);
                }
                case AVISO, ERROR -> {
                    g2.draw(new Line2D.Float(c, c - 8, c, c + 2));
                    g2.fill(new Ellipse2D.Float(c - 1.7f, c + 5.5f, 3.4f, 3.4f));
                }
                case INFO -> {
                    g2.fill(new Ellipse2D.Float(c - 1.7f, c - 9.5f, 3.4f, 3.4f));
                    g2.draw(new Line2D.Float(c, c - 2, c, c + 8));
                }
                case RECHAZO -> {
                    g2.draw(new Line2D.Float(c - 6, c - 6, c + 6, c + 6));
                    g2.draw(new Line2D.Float(c + 6, c - 6, c - 6, c + 6));
                }
                case PREGUNTA -> {
                    g2.setFont(Estilo.fuente(Font.BOLD, 21));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("?", c - fm.stringWidth("?") / 2f, c + fm.getAscent() / 2f - 2.5f);
                }
                case DOCUMENTO -> {
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    float x = c - 7;
                    float y = c - 9;
                    Path2D hoja = new Path2D.Float();
                    hoja.moveTo(x + 9, y);
                    hoja.lineTo(x + 2.5f, y);
                    hoja.quadTo(x, y, x, y + 2.5f);
                    hoja.lineTo(x, y + 15.5f);
                    hoja.quadTo(x, y + 18, x + 2.5f, y + 18);
                    hoja.lineTo(x + 11.5f, y + 18);
                    hoja.quadTo(x + 14, y + 18, x + 14, y + 15.5f);
                    hoja.lineTo(x + 14, y + 5);
                    hoja.closePath();
                    g2.draw(hoja);
                    g2.draw(new Line2D.Float(x + 9, y, x + 9, y + 5));
                    g2.draw(new Line2D.Float(x + 9, y + 5, x + 14, y + 5));
                    g2.draw(new Line2D.Float(x + 4, y + 10.5f, x + 10, y + 10.5f));
                    g2.draw(new Line2D.Float(x + 4, y + 14, x + 8, y + 14));
                }
                default -> { }
            }
            g2.dispose();
        }
    }

    /** Capa oscura semitransparente que se pone sobre la ventana de atrás. */
    private static class Velo extends JComponent {
        private final Shape forma;

        Velo(Shape forma) {
            this.forma = forma;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(21, 24, 38, 92));
            if (forma == null) {
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.fill(forma);
            }
            g2.dispose();
        }
    }
}
