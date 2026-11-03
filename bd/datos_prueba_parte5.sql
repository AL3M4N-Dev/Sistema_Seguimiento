-- =====================================================================
-- Datos de prueba para la parte 5 (panel del docente y constancias)
--
-- Se ejecuta ENCIMA de la base del equipo (servicio_social.sql).
-- No toca los datos que ya existen: agrega alumnos de prueba con correo
-- terminado en ".p5@escuela.edu" e instituciones con "(prueba)" en el nombre.
-- Para quitarlos: bd/borrar_datos_prueba_parte5.sql
--
-- Usa al docente docente@escuela.edu y al responsable responsable@escuela.edu
-- que ya vienen en la base del equipo.
-- =====================================================================

START TRANSACTION;

-- Alumnos de prueba (no pueden iniciar sesión: su contraseña no es un hash real)
INSERT INTO usuarios (nombre, matricula, carrera, correo, password_hash, rol, tipo_programa) VALUES
 ('Ana López',      'P5-001', 'Ing. en Sistemas', 'ana.p5@escuela.edu',    'SIN_ACCESO_PRUEBA', 'alumno', 'servicio_social'),
 ('Carlos Ruiz',    'P5-002', 'Contaduría',       'carlos.p5@escuela.edu', 'SIN_ACCESO_PRUEBA', 'alumno', 'practicas'),
 ('María Torres',   'P5-003', 'Nutrición',        'maria.p5@escuela.edu',  'SIN_ACCESO_PRUEBA', 'alumno', 'servicio_social'),
 ('Luis Herrera',   'P5-004', 'Enfermería',       'luis.p5@escuela.edu',   'SIN_ACCESO_PRUEBA', 'alumno', 'servicio_social'),
 ('Jorge Castillo', 'P5-005', 'Psicología',       'jorge.p5@escuela.edu',  'SIN_ACCESO_PRUEBA', 'alumno', 'servicio_social'),
 ('Sofía Ramírez',  'P5-006', 'Diseño Gráfico',   'sofia.p5@escuela.edu',  'SIN_ACCESO_PRUEBA', 'alumno', 'practicas'),
 ('Pedro Salas',    'P5-007', 'Ing. Electrónica', 'pedro.p5@escuela.edu',  'SIN_ACCESO_PRUEBA', 'alumno', 'practicas'),
 ('Valeria Núñez',  'P5-008', 'Trabajo Social',   'valeria.p5@escuela.edu','SIN_ACCESO_PRUEBA', 'alumno', 'servicio_social'),
 ('Daniela Ortiz',  'P5-009', 'Contaduría',       'daniela.p5@escuela.edu','SIN_ACCESO_PRUEBA', 'alumno', 'practicas');

-- Instituciones de prácticas que dieron de alta los alumnos
INSERT INTO instituciones (nombre, direccion, contacto, tipo, horas_requeridas, estado, creada_por, aprobada_por)
SELECT 'Despacho Contable Méndez (prueba)', 'Av. Juárez 120, Centro', 'Lic. Rosa Méndez · 000 000 0000',
       'practicas', 480, 'disponible', a.id_usuario, d.id_usuario
FROM usuarios a LEFT JOIN usuarios d ON d.correo = 'docente@escuela.edu'
WHERE a.correo = 'carlos.p5@escuela.edu';

INSERT INTO instituciones (nombre, direccion, contacto, tipo, horas_requeridas, estado, creada_por)
SELECT 'Agencia Digital Pixel (prueba)', 'Calle Río Bravo 45', 'Ing. Marco Díaz · 000 000 0000',
       'practicas', 480, 'en_revision', id_usuario
FROM usuarios WHERE correo = 'sofia.p5@escuela.edu';

INSERT INTO instituciones (nombre, direccion, contacto, tipo, horas_requeridas, estado, creada_por)
SELECT 'Taller Electrónico Norte (prueba)', 'Blvd. Norte 310', 'Sr. Julio Paredes · 000 000 0000',
       'practicas', 480, 'en_revision', id_usuario
FROM usuarios WHERE correo = 'pedro.p5@escuela.edu';

-- Inscripciones (cada alumno, a una institución, con el docente de prueba asignado)
INSERT INTO inscripciones (id_alumno, id_institucion, id_docente)
SELECT a.id_usuario, ins.id_institucion, d.id_usuario
FROM usuarios a
JOIN instituciones ins ON ins.nombre = CASE a.correo
        WHEN 'ana.p5@escuela.edu'    THEN 'Biblioteca Municipal'
        WHEN 'carlos.p5@escuela.edu' THEN 'Despacho Contable Méndez (prueba)'
        WHEN 'maria.p5@escuela.edu'  THEN 'Centro de Salud Comunitario'
        WHEN 'luis.p5@escuela.edu'   THEN 'Biblioteca Municipal'
        WHEN 'jorge.p5@escuela.edu'  THEN 'Centro de Salud Comunitario'
        WHEN 'sofia.p5@escuela.edu'  THEN 'Agencia Digital Pixel (prueba)'
        WHEN 'pedro.p5@escuela.edu'  THEN 'Taller Electrónico Norte (prueba)'
        WHEN 'valeria.p5@escuela.edu' THEN 'Centro de Salud Comunitario'
        WHEN 'daniela.p5@escuela.edu' THEN 'Despacho Contable Méndez (prueba)'
     END
LEFT JOIN usuarios d ON d.correo = 'docente@escuela.edu'
WHERE a.correo LIKE '%.p5@escuela.edu';

-- Registros PENDIENTES (aparecen en la bandeja del docente)
INSERT INTO registros_horas (id_inscripcion, fecha, hora_inicio, hora_fin, horas_calculadas, reporte, fecha_envio)
SELECT i.id_inscripcion, v.fecha, v.inicio, v.fin, v.horas, v.reporte, v.envio
FROM inscripciones i
JOIN usuarios a ON a.id_usuario = i.id_alumno
JOIN (
      SELECT 'ana.p5@escuela.edu' AS correo, '2026-10-28' AS fecha, '09:00:00' AS inicio, '13:00:00' AS fin, 4.00 AS horas,
             'Clasifiqué los libros nuevos, atendí el módulo de préstamos y ordené la sección infantil.' AS reporte,
             '2026-10-28 13:30:00' AS envio
      UNION ALL
      SELECT 'carlos.p5@escuela.edu', '2026-10-27', '15:00:00', '19:00:00', 4.00,
             'Capturé facturas del mes y concilié dos cuentas bancarias con el contador.', '2026-10-27 19:15:00'
      UNION ALL
      SELECT 'maria.p5@escuela.edu', '2026-10-27', '08:00:00', '11:00:00', 3.00,
             'Apoyé en la toma de signos vitales y en el registro de pacientes.', '2026-10-27 11:20:00'
     ) v ON v.correo = a.correo;

-- Historial ya revisado (aprobado y rechazado)
INSERT INTO registros_horas (id_inscripcion, fecha, hora_inicio, hora_fin, horas_calculadas, reporte, estado,
                             comentario_docente, id_docente_revisor, fecha_envio, fecha_revision)
SELECT i.id_inscripcion, v.fecha, v.inicio, v.fin, v.horas, v.reporte, v.estado, v.comentario,
       d.id_usuario, v.envio, v.revision
FROM inscripciones i
JOIN usuarios a ON a.id_usuario = i.id_alumno
LEFT JOIN usuarios d ON d.correo = 'docente@escuela.edu'
JOIN (
      SELECT 'ana.p5@escuela.edu' AS correo, '2026-10-21' AS fecha, '09:00:00' AS inicio, '13:00:00' AS fin, 4.00 AS horas,
             'Atendí el módulo de préstamos.' AS reporte, 'aprobado' AS estado, NULL AS comentario,
             '2026-10-21 13:10:00' AS envio, '2026-10-22 10:00:00' AS revision
      UNION ALL
      SELECT 'ana.p5@escuela.edu', '2026-10-14', '09:00:00', '13:00:00', 4.00,
             'Inventario de la sección de consulta.', 'aprobado', NULL, '2026-10-14 13:05:00', '2026-10-15 10:00:00'
      UNION ALL
      SELECT 'carlos.p5@escuela.edu', '2026-10-20', '15:00:00', '18:00:00', 3.00,
             'Apoyo.', 'rechazado', 'Describe qué actividades hiciste en el turno.', '2026-10-20 18:10:00', '2026-10-21 09:30:00'
     ) v ON v.correo = a.correo;

-- Luis: 120 turnos de 4 h aprobados = 480 h (horas completas, ya pidió constancia)
-- Valeria: 120 turnos de 4 h aprobados = 480 h (horas completas, todavía NO la pide)
-- Daniela (PRÁCTICAS): 120 turnos de 4 h aprobados = 480 h (horas completas, no la pide)
-- Jorge: 119 turnos de 4 h aprobados = 476 h (le faltan 4 h)
INSERT INTO registros_horas (id_inscripcion, fecha, hora_inicio, hora_fin, horas_calculadas, reporte, estado,
                             id_docente_revisor, fecha_envio, fecha_revision)
SELECT i.id_inscripcion, DATE_SUB('2026-10-20', INTERVAL n.n DAY), '10:00:00', '14:00:00', 4.00,
       'Turno de servicio (dato de prueba).', 'aprobado', d.id_usuario,
       DATE_SUB('2026-10-20', INTERVAL n.n DAY), '2026-10-21 12:00:00'
FROM inscripciones i
JOIN usuarios a ON a.id_usuario = i.id_alumno
LEFT JOIN usuarios d ON d.correo = 'docente@escuela.edu'
CROSS JOIN (SELECT x.d + y.d * 10 + z.d * 100 AS n
            FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) x
            CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                  UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) y
            CROSS JOIN (SELECT 0 d UNION ALL SELECT 1) z) n
WHERE (a.correo = 'luis.p5@escuela.edu' AND n.n < 120)
   OR (a.correo = 'valeria.p5@escuela.edu' AND n.n < 120)
   OR (a.correo = 'daniela.p5@escuela.edu' AND n.n < 120)
   OR (a.correo = 'jorge.p5@escuela.edu' AND n.n < 119);

-- Solicitudes de constancia de Luis y Jorge
INSERT INTO solicitudes_constancia (id_inscripcion, fecha_solicitud)
SELECT i.id_inscripcion, CASE a.correo WHEN 'luis.p5@escuela.edu' THEN '2026-11-01 09:15:00'
                                       ELSE '2026-11-02 11:40:00' END
FROM inscripciones i
JOIN usuarios a ON a.id_usuario = i.id_alumno
WHERE a.correo IN ('luis.p5@escuela.edu', 'jorge.p5@escuela.edu');

COMMIT;
