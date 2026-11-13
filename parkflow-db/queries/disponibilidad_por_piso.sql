-- Contadores por piso: SIEMPRE calculados, nunca guardados (RN-P3, RN-P10).
SELECT p.codigo                                                              AS piso,
       count(*)                                                              AS total_espacios,
       count(*) FILTER (WHERE e.habilitado AND p.habilitado AND s.id IS NULL)     AS disponibles,
       count(*) FILTER (WHERE e.habilitado AND p.habilitado AND s.id IS NOT NULL) AS ocupados,
       count(*) FILTER (WHERE NOT (e.habilitado AND p.habilitado))                AS fuera_de_servicio,
       round(100.0 * count(*) FILTER (WHERE e.habilitado AND p.habilitado AND s.id IS NOT NULL)
             / count(*), 1)                                                  AS pct_ocupacion
  FROM pisos p
  JOIN espacios e ON e.piso_id = p.id
  LEFT JOIN sesiones_parqueo s ON s.espacio_id = e.id AND s.estado = 'ACTIVA'
 GROUP BY p.id, p.codigo, p.orden
 ORDER BY p.orden;
