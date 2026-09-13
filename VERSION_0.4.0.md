# Semáforo 0.4.0 · Decisiones explicables y resultados voluntarios

La evaluación distingue rentabilidad económica, restricciones personales y datos pendientes. El importe grande de Uber no representa lo disponible después de costos, ni una oferta observada acredita un viaje realizado.

## Cambios

- Perfil por revisar: confirma por separado vehículo/energía, precio y rendimiento, mantenimiento/desgaste/fijos y objetivos. La instalación nueva empieza con un ejemplo genérico a gasolina, editable; las actualizaciones conservan tu modelo, energía, autonomía, precios y reglas existentes. El reconocimiento general de una versión anterior no sustituye estas revisiones. En PHEV/eléctrico se avisa y limita a ámbar cuando la autonomía restante no se actualizó en 24 horas. Puedes actualizarla expresamente aunque su valor no haya cambiado.
- Recogida desproporcionada: muestra porcentajes de kilómetros y minutos visibles antes de subir al pasajero. Umbral inicial editable de 60%. Es una alerta; no duplica la penalización económica existente.
- Ganancia frágil: calcula el importe mínimo económico redondeado hacia arriba a centavos y los minutos extra tolerables antes de incumplir el mínimo por hora o km, incluyendo costos fijos adicionales. La tolerancia supone carro detenido sin energía ni distancia adicionales: aire acondicionado, combustible al ralentí y desplazamientos pueden reducirla. Un mínimo económico favorable nunca elimina otra restricción.
- Regreso: escenario opcional con 10 km / 20 min como valores de ejemplo, inactivo por defecto. Sustituye la reposición base; no suma dos veces el regreso. Si incumple mínimos o autonomía, impide verde y conserva cualquier rojo. Sin un escenario/reposición se avisa que el regreso no se consideró. No estima demanda, probabilidades de retorno ni condiciones de tráfico.
- Precio actualizado: dos lecturas coincidentes confirman una nueva tarifa; se recalcula y avisa cuánto subió o bajó. Agrupa versiones consecutivas con mismos tiempos, distancias, categoría, contador y ambos extremos legibles durante una misma sesión, con separación máxima de tres minutos. No es un identificador de Uber; cambios de ruta/tiempo o sesiones separadas generan otro grupo. Con extremos ausentes o imágenes importadas de distinto importe no infiere que sean la misma oferta.
- Zonas: indica incertidumbre por separado para recogida y destino; distingue municipio sin confirmar de revisión vencida. Mantiene periodo y fuente histórica/comunitaria separados de tu motivo y fecha de revisión. Revisar hoy una regla no vuelve reciente el antecedente. El catálogo de 90 reglas no cambió y respeta tus modificaciones y eliminaciones.
- Semáforo: muestra color final, score económico, restricción principal y pendientes. El filtro configurable de usuario nuevo sigue imponiendo rojo. El contador visible y estrellas son reglas personales; no son un pronóstico de peligrosidad ni un historial certificado de viajes. El detalle conserva todos los motivos, aun cuando la ventana resume uno.
- Resultados y precisión: selecciona la versión y perfil con los que decidiste; registra voluntariamente rechazo, aceptación pendiente, cancelación o finalización. Solo una finalización admite importe recibido, tiempo y km totales; espera/regreso son partes de esos totales. Cambiar la versión con resultado sustituye el registro del grupo, sin sumar otro viaje completado. Sin registrar un resultado, una oferta nunca aporta ingresos realizados.
- Costos reales informados: los cuatro costos opcionales quedan desconocidos cuando están vacíos. Para calcular saldo informado deben estar completos, incluido 0 escrito expresamente si corresponde. Se permiten pérdidas reales. No precarga cifras reales con estimaciones ni verifica pagos con Uber. Los promedios comparan contra la versión seleccionada; los resultados con costos pendientes se excluyen del saldo y hora informados. No mide tiempos ociosos de toda la jornada, ni ajusta automáticamente pesos del score.
- Simulador: añade Comfort y la condición de reserva. No se guarda como viaje real.

## Datos y actualización

Todo el análisis y registro permanece local. Se conservan hasta 200 versiones, procurando descartar primero las que no tienen un resultado registrado. Si todas tienen resultados, se elimina la más antigua al alcanzar el límite. Las direcciones y texto de las tarjetas solo se usan en memoria; no se publican capturas privadas ni se suben resultados. El diagnóstico exportado voluntariamente incluye los nuevos resultados y escenarios, con esquema 6. Android no respalda automáticamente estos datos.

Instala sobre la versión anterior sin desinstalar, y revisa las nuevas confirmaciones de perfil. La firma continúa siendo la misma. La landing y el repositorio público describen el alcance y distribuyen la APK firmada.

## Verificación

Consulta cifras y evidencia verificadas de esta entrega en [verification/release-0.4.0.json](verification/release-0.4.0.json). Incluye fórmulas en sus límites, versiones de oferta, resultados con costos ausentes, selección exacta del registro, escritura en disco, migración y formularios Android. Las regresiones de capturas privadas se ejecutan localmente sin incluirlas en la APK pública.

Las pruebas locales no sustituyen probar ofertas reales en varios teléfonos. Siguen pendientes la ruta intermedia, múltiples paradas, horario de reserva, tráfico en vivo y un modelo de demanda de regreso. Para aprender condiciones históricas harían falta resultados voluntarios suficientes y comparables; estas capturas por sí solas no acreditan esos patrones.
