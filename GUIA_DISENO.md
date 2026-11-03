# Guía del diseño base · Sistema de Seguimiento

El equipo eligió la **opción E (lista y detalle)** como diseño de todo el proyecto. Todo está en el paquete `vista.comun`, así que cualquier pantalla puede verse igual sin copiar colores ni código.

## 1. Activarlo (una sola vez)

Al iniciar la app, antes de abrir cualquier ventana (en el `main` o en el login):

```java
Estilo.aplicarTema();
```

> Si hiciste tu ventana con el diseñador de NetBeans (*JFrame Form*), su `main` trae un bloque que activa **Nimbus**. Cámbialo por `Estilo.aplicarTema();`, porque Nimbus ignora los colores del proyecto.

## 2. Colores

| Uso | Constante | Color |
|---|---|---|
| Acción principal, enlaces, pestaña activa | `Estilo.PRIMARIO` | Índigo #4338CA |
| Fondo de las pantallas | `Estilo.FONDO` | Gris muy claro #F5F6FA |
| Tarjetas | `Estilo.SUPERFICIE` | Blanco |
| Bordes | `Estilo.BORDE` | #E3E5EE |
| Texto / texto secundario | `Estilo.TEXTO` / `Estilo.TEXTO_SECUNDARIO` | #151826 / #5A5F73 |
| Aprobar, completo | `Estilo.EXITO` | Verde #15803D |
| Rechazar, eliminar | `Estilo.PELIGRO` | Rojo #B91C1C |

Estados (pastillas): **amarillo** = pendiente o solicitada · **índigo** = lista para subir · **verde** = aprobado o entregada · **rojo** = rechazado · **gris** = en curso. `Estilo.coloresEstado("Pendiente")` te da el par {fondo, texto}.

**Letra:** Segoe UI (la de Windows). Usa `Estilo.fuente(Font.BOLD, 14)`.

## 3. Piezas listas para usar

| Pieza | Para qué | Ejemplo |
|---|---|---|
| `EncabezadoApp` | Barra de arriba con marca, pestañas y usuario | `new EncabezadoApp("Ana López · Alumna")` |
| `Tarjeta` | Caja blanca redondeada para agrupar | `Tarjeta t = new Tarjeta();` |
| `Estilo.titulo / subtitulo / secundario` | Textos con el tamaño correcto | `Estilo.titulo("Mi menú")` |
| `Estilo.botonPrincipal` | Acción principal (índigo) | `Estilo.botonPrincipal("Guardar")` |
| `Estilo.botonExito` | Aprobar / confirmar (verde) | `Estilo.botonExito("Aprobar")` |
| `Estilo.botonSecundario` | Acción secundaria (blanco) | `Estilo.botonSecundario("Cancelar")` |
| `Estilo.botonPeligro` | Rechazar / eliminar (borde rojo) | `Estilo.botonPeligro("Rechazar")` |
| `Boton.enlace` | Botón que se ve como enlace | `Boton.enlace("Ver todos")` |
| `Chip` | Pastilla de estado o categoría | `Chip.deEstado("Pendiente")` |
| `ChipFiltro` | Filtros tipo pastilla | `ChipFiltro.grupo(todos, servicio, practicas)` |
| `CampoBusqueda` | Búsqueda con lupa y texto de ayuda | `new CampoBusqueda("Buscar alumno")` |
| `BarraProgreso` | Barra de avance delgada (verde al 100%) | `barra.setValor(75)` |
| `Avatar` | Círculo con iniciales | `new Avatar("Daniela Ortiz", 40)` |
| `ZonaArchivo` | Arrastrar o elegir un archivo (PDF, fotos…) | `new ZonaArchivo("Arrastra tus fotos", "o haz clic", "Imágenes", "jpg", "png")` |
| `Estilo.estilizarTabla(tabla)` | Tablas con filas altas y líneas suaves | junto con `EstadoRenderer` para la columna de estado |
| `Estilo.panel(layout)` | Sub-panel transparente para poner dentro de una `Tarjeta` | `Estilo.panel(new BorderLayout())` |

## 4. Reglas para que todo se vea igual

1. **Fondo de pantalla** `Estilo.FONDO`, con un margen de 22 px arriba y abajo y 28 px a los lados.
2. **Agrupa en tarjetas** y deja 16 px entre ellas.
3. **Un solo botón principal por pantalla.** Lo demás va en secundario.
4. **Dentro de una `Tarjeta` usa `Estilo.panel(...)`**, no `new JPanel(...)`; si no, se pinta un cuadro gris encima de la tarjeta.
5. **Los textos de la app van en español y con frases claras** ("Subir PDF y aprobar", no "Submit").

## 5. Ejemplo: una pantalla nueva con el diseño

```java
JPanel pantalla = new JPanel(new BorderLayout(0, 16));
pantalla.setBackground(Estilo.FONDO);
pantalla.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

Tarjeta tarjeta = new Tarjeta();
tarjeta.add(Estilo.subtitulo("Registrar horas"), BorderLayout.NORTH);
tarjeta.add(new ZonaArchivo("Arrastra aquí tus fotos", "o haz clic para elegirlas",
        "Imágenes", "jpg", "jpeg", "png"), BorderLayout.CENTER);
tarjeta.add(Estilo.botonPrincipal("Enviar a revisión"), BorderLayout.SOUTH);

pantalla.add(Estilo.titulo("Añadir horas"), BorderLayout.NORTH);
pantalla.add(tarjeta, BorderLayout.CENTER);
```

Para ver un ejemplo completo, revisa `vista/docente/AlumnosPanel.java`: es la pestaña del boceto E ya funcionando.
