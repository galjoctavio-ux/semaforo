# Semáforo 0.4.1 · Lectura de tarjetas oscuras en pantalla completa

La 0.4.0 podía leer `$1l4.33` en una oferta de $114.33. La letra intermedia hacía que el importe se descartara sin activar una segunda lectura. La captura en vivo de $118.03 también reprodujo «Falta el importe» con la APK anterior en el emulador.

## Correcciones

- Un importe con caracteres confundidos solicita una lectura ampliada de ese campo de la misma imagen. Solo se admite el importe si la nueva lectura contiene dígitos y centavos legibles. No se sustituyen letras por números, no se reconstruye el precio desde $/km y no se suma dos veces el suplemento Priority.
- Las líneas se delimitan con la posición de la categoría, campos numéricos y botón de la tarjeta actual. Esto excluye fragmentos del mapa que podían mezclarse con las direcciones y romper la confirmación. La posición se vuelve a determinar en cada imagen; no se combinan datos de dos fotogramas.
- La ventana flotante no vuelve a generar texto y fondo idénticos. Así evita provocar redibujos de su propia ventana en cada lectura de pantalla completa.
- Los avisos repetidos de Android que conservan el mismo estado de visibilidad no reinician la confirmación.

## Límites de la lectura

Continúan siendo necesarias dos observaciones completas y coincidentes. Una oferta distinta o una imagen incompleta retira la evaluación previa; ocultar, detener o redimensionar la captura invalida el trabajo anterior. Los resultados que tardan más de 1.4 segundos desde adquisición se descartan; sin publicación reciente, la evaluación caduca a los 1.4 segundos, revisados cada 0.3 segundos. No se prolonga una puntuación para disimular una lectura fallida.

Las cuatro nuevas capturas se verifican completas y en la región usada por la captura en vivo: $114.33, $118.03, $92.92 y $61.06, incluidos minutos, kilómetros y datos visibles del pasajero. Las secuencias comprueban tarjetas consecutivas, cambio de posición y resolución, pérdida de la oferta y confirmación repetida. Se mantienen las regresiones anteriores de modo oscuro, Exclusivo, UberXL, Comfort, Priority y Reservar UberX.

Las cifras definitivas de pruebas y la huella de la APK están en [verification/release-0.4.1.json](verification/release-0.4.1.json). Las capturas y registros privados no se distribuyen. Las pruebas de captura continua se ejecutan sobre superficies de prueba en un emulador Android 16; no equivalen a verificar la aplicación Uber en el teléfono del conductor. Sigue siendo necesaria esa validación tras instalar la actualización.

## Instalación

Instala [Detector-ofertas-0.4.1.apk](entrega/Detector-ofertas-0.4.1.apk) sobre la versión anterior, sin desinstalar. Mantiene el paquete y certificado de firma. Las funciones y supuestos de [0.4.0](VERSION_0.4.0.md) siguen vigentes.

Referencias de Android usadas en la revisión: [imágenes nuevas en ImageReader](https://developer.android.com/reference/android/media/ImageReader#acquireLatestImage()) y [visibilidad y finalización de MediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjection.Callback).
