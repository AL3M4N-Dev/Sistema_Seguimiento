# Parte 5: Panel del docente y constancias

Usa la estructura del proyecto del equipo: `util.Conexion`, `modelo.Usuario`, `util.Sesion` y las tablas de `servicio_social.sql`. No agrega librerías y no modifica archivos de los demás integrantes.

## Qué hace

El docente y el responsable de área ven 3 pestañas:

| Pestaña | Qué hace |
|---|---|
| **Registros de horas** | Bandeja para aprobar o rechazar los turnos (con reporte y fotos). El rechazo pide comentario. |
| **Alumnos y constancias** | Lista de alumnos activos de **servicio social y prácticas** con su barra de horas aprobadas, filtros y buscador. Al seleccionar a uno se ve su ficha. Cuando completa sus horas se habilita **Subir PDF y aprobar**: el PDF queda guardado y el alumno ya puede descargar su constancia. |
| **Instituciones por aprobar** | Las instituciones de prácticas que dieron de alta los alumnos. |

- El **docente** ve a sus alumnos asignados (`inscripciones.id_docente`) y a los que aún no tienen docente.
- El **responsable de área** ve a todos.

Para el **alumno**, la sección `vista.alumno.ConstanciaAlumnoPanel` muestra su avance y le permite pedir su constancia (se desbloquea al 100%) y descargar el PDF. Está lista para que el Integrante 3 la ponga en el menú principal.

## Archivos

```
src/modelo/     AlumnoActivo, ProgresoAlumno, RegistroHoras, Evidencia, Institucion,
                SolicitudConstancia, EstadoRegistro, EstadoSolicitud, Programa
src/dao/        AlumnoActivoDAO, ProgresoAlumnoDAO, RegistroHorasDAO, EvidenciaDAO,
                InstitucionDAO, SolicitudConstanciaDAO
src/servicio/   PanelDocenteService, ConstanciaAlumnoService (todas las reglas),
                ReglaNegocioException
src/vista/comun/    colores, tablas, barra de avance y mensajes que puede usar todo el equipo
src/vista/docente/  PanelDocenteFrame, RegistrosPanel, DetalleRegistroDialog,
                    AlumnosPanel, InstitucionesPanel, PruebaPanelDocente (solo para probar)
src/vista/alumno/   ConstanciaAlumnoPanel, PruebaConstanciaAlumno (solo para probar)
bd/             datos_prueba_parte5.sql, borrar_datos_prueba_parte5.sql
```

## Cómo probarlo

1. Importa en phpMyAdmin la base del equipo (`servicio_social.sql`) y luego `bd/datos_prueba_parte5.sql`.
   Si ya tenías los datos de prueba, primero importa `bd/borrar_datos_prueba_parte5.sql`.
2. En NetBeans: `src/vista/docente/PruebaPanelDocente.java` → clic derecho → **Run File** (Shift + F6).
3. Elige **Docente Prueba** y ve a la pestaña **Alumnos y constancias**.

| Prueba | Qué debe pasar |
|---|---|
| Seleccionar a Ana López (8 de 480 h) | El botón **Subir PDF y aprobar** está bloqueado; la ficha dice "Le faltan 472 h" |
| Valeria Núñez (servicio social, 480 h) → **Subir PDF y aprobar** con un PDF | Pasa a "Entregada" y se habilita **Abrir PDF** |
| Daniela Ortiz (**prácticas**, 480 h) → **Subir PDF y aprobar** | Igual que Valeria |
| Luis Herrera (pidió su constancia) → **Rechazar solicitud** sin comentario | Aviso: "Escribe un comentario para rechazar la solicitud." |
| Subir un archivo que no es PDF | Aviso: "El archivo no es un PDF válido." |
| Filtro **Programa: Prácticas** | Solo aparecen los alumnos de prácticas |
| Alumno: `src/vista/alumno/PruebaConstanciaAlumno.java` → **Run File** → Valeria | Le aparece **Descargar constancia en PDF** |

## Para conectarlo con el resto del equipo

- **Inicio de sesión:** si el usuario es docente o responsable, abre el panel así:
  ```java
  Sesion.iniciar(usuario);
  new PanelDocenteFrame().setVisible(true);
  ```
- **Menú principal del alumno:** la sección de constancia ya está hecha. Solo hay que agregarla al menú:
  ```java
  ConstanciaAlumnoPanel constancia = new ConstanciaAlumnoPanel();  // usa Sesion.getUsuario()
  panelDelMenu.add(constancia);
  constancia.recargar();
  ```
- **Dónde quedan los PDF:** en `archivos/constancias/constancia_<id_inscripcion>.pdf`, dentro de la carpeta del proyecto. Esa ruta se guarda en `solicitudes_constancia.ruta_pdf` con `estado = 'disponible'`, y la inscripción pasa a `estado = 'completada'`.
- **Evidencias (registro de horas):** el panel abre las fotos desde `evidencias.ruta_archivo`. Si la ruta es relativa, la busca dentro de la carpeta del proyecto. Como son rutas de archivo, las fotos y los PDF solo se ven en la computadora donde se guardaron.
- **Solicitud rechazada:** como `id_inscripcion` es único en `solicitudes_constancia`, "Volver a solicitar" actualiza la misma fila en lugar de insertar otra.
- **Institución rechazada:** su `estado` pasa a `'rechazada'`. Como `id_alumno` es único en `inscripciones`, el módulo de instituciones debe dejar que el alumno elija otra cambiando `id_institucion`.
