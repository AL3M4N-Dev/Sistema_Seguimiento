# Parte 5: Panel del docente y constancias

Usa la estructura del proyecto del equipo: `util.Conexion`, `modelo.Usuario`, `util.Sesion` y las tablas de `servicio_social.sql`. No agrega librerías y no modifica archivos de los demás integrantes.

**Diseño:** usa el diseño base del proyecto (opción E, lista y detalle). Cómo usarlo en otras pantallas: ver **`GUIA_DISENO.md`**.

## Qué hace

El docente y el responsable de área entran a una ventana con 3 secciones en la barra de arriba:

| Sección | Qué hace |
|---|---|
| **Registros** | Bandeja para aprobar o rechazar los turnos (con reporte y fotos). El rechazo pide comentario. El número junto al nombre indica cuántos turnos están pendientes. |
| **Alumnos y constancias** | A la izquierda, la lista de alumnos de **servicio social y prácticas** con su avance, un buscador y filtros. A la derecha, el detalle del alumno: sus horas, sus últimos turnos y su constancia. Cuando completa sus horas, se puede **arrastrar el PDF** (o hacer clic para elegirlo) y **Subir PDF y aprobar**. |
| **Instituciones** | Las instituciones de prácticas que dieron de alta los alumnos, para aprobarlas o rechazarlas. |

- El **docente** ve a sus alumnos asignados (`inscripciones.id_docente`) y a los que aún no tienen docente.
- El **responsable de área** ve a todos.

Para el **alumno**, la sección `vista.alumno.ConstanciaAlumnoPanel` muestra su avance y le permite pedir su constancia (se desbloquea al 100%) y descargar el PDF. Ya tiene el diseño base y está lista para que el Integrante 3 la ponga en el menú principal.

## Archivos

```
src/modelo/     AlumnoActivo, ProgresoAlumno, RegistroHoras, Evidencia, Institucion,
                SolicitudConstancia, EstadoRegistro, EstadoSolicitud, Programa
src/dao/        AlumnoActivoDAO, ProgresoAlumnoDAO, RegistroHorasDAO, EvidenciaDAO,
                InstitucionDAO, SolicitudConstanciaDAO
src/servicio/   PanelDocenteService, ConstanciaAlumnoService (todas las reglas),
                ReglaNegocioException
src/vista/comun/    DISEÑO BASE para todo el equipo: Estilo, Boton, Tarjeta, Chip, ChipFiltro,
                    Avatar, BarraProgreso, CampoBusqueda, ZonaArchivo, EncabezadoApp,
                    EstadoRenderer, ListaTableModel, Dialogos, Formato (y Avance/AvanceRenderer)
src/vista/docente/  PanelDocenteFrame, RegistrosPanel, DetalleRegistroDialog, AlumnosPanel,
                    RegistrosAlumnoDialog, InstitucionesPanel, PruebaPanelDocente (solo para probar)
src/vista/alumno/   ConstanciaAlumnoPanel, PruebaConstanciaAlumno (solo para probar)
bd/             datos_prueba_parte5.sql, borrar_datos_prueba_parte5.sql
```

## Cómo probarlo

1. Importa en phpMyAdmin la base del equipo (`servicio_social.sql`) y luego `bd/datos_prueba_parte5.sql`.
   Si ya tenías los datos de prueba, primero importa `bd/borrar_datos_prueba_parte5.sql`.
2. En NetBeans: `src/vista/docente/PruebaPanelDocente.java` → clic derecho → **Run File** (Shift + F6).
3. Elige **Docente Prueba** y ve a **Alumnos y constancias**.

| Prueba | Qué debe pasar |
|---|---|
| Abrir **Alumnos y constancias** | Arriba de la lista aparecen primero los que ya pueden recibir su constancia (Daniela, Luis) |
| Seleccionar a Ana López (8 de 480 h) | La zona del PDF y **Subir PDF y aprobar** están bloqueadas: "Le faltan 472 h aprobadas" |
| Daniela Ortiz (**prácticas**, 480 h) → arrastrar un PDF a la zona → **Subir PDF y aprobar** | Pasa a "Entregada" y aparecen **Abrir PDF** y **Reemplazar PDF** |
| Valeria Núñez (servicio social, 480 h) → **Subir PDF y aprobar** sin arrastrar nada | Se abre el selector de archivos para elegir el PDF |
| Luis Herrera (pidió su constancia) → **Rechazar solicitud** sin comentario | Aviso: "Escribe un comentario para rechazar la solicitud." |
| Arrastrar un archivo que no es PDF | La zona avisa: "Ese archivo no se puede usar · Solo se aceptan archivos pdf" y no lo acepta |
| **Ver todos sus registros** | Ventana con todos los turnos del alumno y su estado |
| Filtro **Prácticas** o escribir en **Buscar** | La lista se filtra al momento |
| Alumno: `src/vista/alumno/PruebaConstanciaAlumno.java` → **Run File** → Daniela | Le aparece **Descargar constancia en PDF** |

## Para conectarlo con el resto del equipo

- **Inicio de sesión:** si el usuario es docente o responsable, abre el panel así:
  ```java
  Estilo.aplicarTema();          // una vez, al iniciar la app
  Sesion.iniciar(usuario);
  PanelDocenteFrame panel = new PanelDocenteFrame();
  panel.setAccionCerrarSesion(() -> new LoginFrame().setVisible(true));   // opcional
  panel.setVisible(true);
  ```
- **Menú principal del alumno:** la sección de constancia ya está hecha. Solo hay que agregarla al menú:
  ```java
  ConstanciaAlumnoPanel constancia = new ConstanciaAlumnoPanel();  // usa Sesion.getUsuario()
  panelDelMenu.add(constancia);
  constancia.recargar();
  ```
- **Dónde quedan los PDF:** en `archivos/constancias/constancia_<id_inscripcion>.pdf`, dentro de la carpeta del proyecto. Esa ruta se guarda en `solicitudes_constancia.ruta_pdf` con `estado = 'disponible'`, y la inscripción pasa a `estado = 'completada'`.
- **Evidencias (registro de horas):** el panel abre las fotos desde `evidencias.ruta_archivo`. Si la ruta es relativa, la busca dentro de la carpeta del proyecto. Como son rutas de archivo, las fotos y los PDF solo se ven en la computadora donde se guardaron. `ZonaArchivo` también sirve para que el alumno suba sus fotos.
- **Solicitud rechazada:** como `id_inscripcion` es único en `solicitudes_constancia`, "Volver a solicitar" actualiza la misma fila en lugar de insertar otra.
- **Institución rechazada:** su `estado` pasa a `'rechazada'`. Como `id_alumno` es único en `inscripciones`, el módulo de instituciones debe dejar que el alumno elija otra cambiando `id_institucion`.
