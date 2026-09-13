# Semáforo 0.3.2 · UberXL y Exclusivo

Instala `entrega/Detector-ofertas-0.3.2.apk` sobre la anterior sin desinstalar. Usa el mismo paquete y certificado. Costos, objetivos, filtros, historial y reglas personales se conservan. Inicia una nueva sesión de Pantalla completa después de actualizar.

## Cambio

0.3.1 buscaba las etiquetas UberX y Priority. UberXL quedaba fuera antes de leer importe y trayecto, mostrando «Esperando una oferta UberX o Priority». Ahora reconoce UberXL, Uber XL y Uber X L, con Exclusivo y botón Aceptar o Viaje disponible. Se conserva como categoría UBER_XL en diagnóstico e historial y aparece en el simulador y la ventana.

Los cuatro ejemplos nuevos comprueban UberX flotante sobre Facebook por $123.27, UberXL por $284.93, UberXL Exclusivo por $284.93 y UberXL Exclusivo por $236.82. Las tarjetas XL muestran recogida de 19 min / 8.5 km, viaje de 41 min / 17.1 km y pasajero 4.87 (91 visibles). Son dos importes distintos, sin afirmar por qué Uber los cambió ni que correspondan al mismo identificador oficial.

Cambiar de $284.93 a $236.82 inicia una nueva confirmación; no se conserva el cálculo del precio anterior. Tras dos lecturas coincidentes se evalúa y registra el nuevo importe. Exclusivo puede cambiar en la transición radar/solicitud individual sin cambiar la identidad numérica; la identidad sigue siendo heurística. Se mantienen los controles de centavos, incoherencias, antigüedad de captura y retirada de resultados de 0.3.1.

El recorte de lectura en vivo de la foto de $236.82 produjo «17.1 m» en vez de «17.1 km». Reunir franjas en una imagen artificial no recuperaba una lectura válida. Ahora hay una etapa de relectura sobre franjas aisladas con contexto natural ampliado: una para centavos ausentes, o hasta tres (tarifa, recogida, viaje) si las cifras no cuadran. Se conserva el importe con centavos ya legible y las direcciones/pasajero de la misma imagen. Cada sustitución debe conservar su tipo y el resultado completo debe pasar los controles. No se transforma m en km por suposición. Si sigue siendo ambiguo o tarda más de 1.4 s desde adquisición en vivo, no se publica un score.

## Criterio económico

El desglose del historial muestra «Umbral configurado de usuario nuevo: contador visible < 5» en lugar de «Nuevo por contador < 5». Describe la regla guardada con esa evaluación, no clasifica al pasajero mostrado como nuevo. Un contador de 393 no cumple ese umbral. No cambia el score ni las reglas configuradas.

No hay bonificación de score por UberXL, Exclusivo ni Priority. Se calcula con el importe visible, recogida, trayecto, costos configurados, pasajero y zonas. Un pago mayor ya mejora el margen. No se infiere que XL implique más propinas, seguridad o un pasajero mejor.

Las categorías usan el mismo perfil de vehículo configurado. No se conoce por la tarjeta cuántos pasajeros viajarán ni su equipaje; no se inventa un incremento de consumo ni se valida capacidad o elegibilidad del carro.

Con los defaults, la recogida de 19 minutos / 8.5 km supera los límites de 15 minutos / 7 km y genera rojo aun con buen margen. Los límites se pueden cambiar en Objetivos y score. No se relajan por observar una tarifa alta. En este escenario inicial, antes de impuestos y sin regreso adicional, el disponible estimado es $226.84 para la oferta de $284.93 y $178.73 para la de $236.82. Tus valores reales pueden dar otros resultados.

## Verificación

Resultados, firma, fotos ausentes de la entrega y pruebas de actualización en `verification/release-0.3.2.json`. Las fotos se usan únicamente en la APK separada de pruebas. Las pruebas locales no acreditan respuesta dentro de diez segundos en el vivo. Falta comprobar una tarjeta real Priority mexicana.

Pasaron 87 pruebas unitarias y 41 Android. En el emulador Android 16, la captura de pantalla completa detectó ambas tarjetas XL con las actividades minimizadas y registró los dos importes correctamente. La APK final firmada se instaló sobre 0.3.1 sin desinstalar entre ambas y conservó gasolina configurada en 28 y las 90 reglas. Se comprobó el texto del umbral en el desglose final; la APK instalada coincide por SHA-256 con la entrega.

Prueba estacionado las capturas desde el importador y luego una solicitud XL real en Pantalla completa, incluida su transición a Exclusivo/Aceptar. Confirma que aparezca TIPO: UBERXL y que importe, tiempo y distancia coincidan antes de usar el semáforo para decidir.
