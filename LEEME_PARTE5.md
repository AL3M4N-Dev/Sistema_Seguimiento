# Parte 5: Panel del docente y del responsable de área

Usa la estructura del proyecto del equipo: `util.Conexion`, `modelo.Usuario`, `util.Sesion` y las tablas de `servicio_social.sql`.

| Rol | Pestañas que ve |
|---|---|
| Docente | **Registros de horas** (sus alumnos asignados y los que no tienen docente) e **Instituciones por aprobar** |
| Responsable de área | Las dos anteriores (registros de todos) y **Constancias** |

## Archivos nuevos

```
src/modelo/     EstadoRegistro, EstadoSolicitud, Programa, RegistroHoras, Evidencia,
                Institucion, SolicitudConstancia
src/dao/        RegistroHorasDAO, EvidenciaDAO, InstitucionDAO, SolicitudConstanciaDAO
src/servicio/   PanelDocenteService (todas las reglas), ReglaNegocioException
src/vista/comun/    colores, tablas y mensajes que puede usar todo el equipo
src/vista/docente/  PanelDocenteFrame y sus pestañas, PruebaPanelDocente (solo para probar)
bd/             datos_prueba_parte5.sql y borrar_datos_prueba_parte5.sql
```

No se modificó ningún archivo de los demás. Solo cambió `.gitignore`, que ahora ignora `/build/`, `/dist/` y `/archivos/`.

## Cómo probarlo

1. Importa en phpMyAdmin la base del equipo (`servicio_social.sql`) y luego `bd/datos_prueba_parte5.sql`.
2. En NetBeans: `src/vista/docente/PruebaPanelDocente.java` → clic derecho → **Run File** (Shift + F6).
3. Elige **Docente Prueba** o **Responsable Prueba**.

| Prueba | Qué debe pasar |
|---|---|
| Docente → doble clic en Ana López → **Aprobar** | Pasa a "Aprobado" |
| Carlos Ruiz → **Rechazar** sin comentario | Aviso: "Escribe un comentario para rechazar el registro." |
| Instituciones → "Agencia Digital Pixel (prueba)" → **Aprobar** | Desaparece de la lista; queda `disponible` |
| Responsable → Constancias → Jorge Castillo → **1. Validar horas** | No deja: lleva 476 de 480 horas |
| Luis Herrera → **1. Validar horas** y luego **2. Subir PDF** | Queda "Disponible"; el PDF se copia a `archivos/constancias/` |

Para quitar los datos de prueba, usa `bd/borrar_datos_prueba_parte5.sql`.

## Para conectarlo con el resto del equipo

- **Inicio de sesión:** si el usuario es docente o responsable, abre el panel así:
  ```java
  Sesion.iniciar(usuario);
  new PanelDocenteFrame().setVisible(true);
  ```
- **Evidencias (registro de horas):** el panel abre la imagen desde `evidencias.ruta_archivo`. Si la ruta es relativa, la busca dentro de la carpeta del proyecto. Como es una ruta de archivo, la imagen tiene que estar en la misma computadora donde se abre el panel.
- **Constancias (menú principal):** el PDF se guarda en `archivos/constancias/constancia_<id_solicitud>.pdf` y esa ruta queda en `solicitudes_constancia.ruta_pdf`. El alumno lo descarga cuando `estado = 'disponible'`.
- **Solicitud rechazada:** como `id_inscripcion` es único en `solicitudes_constancia`, para volver a pedir la constancia hay que actualizar la misma fila a `'solicitada'`, no insertar otra.
- **Institución rechazada:** su `estado` pasa a `'rechazada'`. Como `id_alumno` es único en `inscripciones`, el módulo de instituciones debe dejar que el alumno elija otra cambiando `id_institucion`. La tabla no tiene una columna para guardar el motivo del rechazo; si la quieren, hay que agregarla.
