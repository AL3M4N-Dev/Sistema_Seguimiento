-- =====================================================================
-- Borra los datos de prueba de la parte 5 (datos_prueba_parte5.sql).
-- Solo toca alumnos con correo ".p5@escuela.edu" e instituciones con "(prueba)".
-- Al borrar a los alumnos se borran solas sus inscripciones, registros,
-- evidencias y solicitudes (las llaves foráneas tienen ON DELETE CASCADE).
-- Los PDF que subiste quedan en la carpeta archivos/constancias: bórralos a mano.
-- =====================================================================

START TRANSACTION;

DELETE FROM usuarios WHERE correo LIKE '%.p5@escuela.edu';
DELETE FROM instituciones WHERE nombre LIKE '%(prueba)';

COMMIT;
