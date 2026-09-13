# Semáforo 0.3.3 · Lectura durante el efecto azul

Instala `entrega/Detector-ofertas-0.3.3.apk` sobre la anterior sin desinstalar. Se mantiene el paquete y certificado privados. Después de actualizar inicia una nueva sesión de Pantalla completa.

## Evidencia y cambio

Las cinco fotos nuevas contienen UberX Exclusivo por $80.17 en dos fases azules, su tarjeta radar con las mismas cifras, UberX por $129.34 y una tarjeta real Uber Priority por $65.49 con una línea adicional de $10.34. Las fotos originales y sus recortes de lectura pasaron en 0.3.2. Por sí solas no reproducían el fallo reportado.

La prueba de pantalla completa en el emulador sí produjo lecturas grises. Una imagen de esa prueba, reducida a la resolución de captura, perdió el punto en 1.4 km y leyó 14 km; también apareció un carácter pegado al botón Aceptar. Se conserva como regresión únicamente en la APK separada de pruebas.

La primera lectura y sus recortes conservan el color original, como en 0.3.2. Se ensayó quitar tinte azul antes de toda lectura, pero no recuperaba todos los casos. Se mantiene la ruta que ya pasó las capturas anteriores y las cinco nuevas.

Si no se consigue una oferta completa y hay un problema de centavos, coherencia numérica o categoría/botón en una tarjeta con cifras completas, se permite un único segundo reconocimiento completo con una copia temporal monocromática de canal rojo y contraste 1.5 / desplazamiento −64. Esa variante recuperó la imagen problemática, pero aplicada a todas las fotos causó cuatro regresiones; por eso se usa solo como alternativa a una lectura fallida. Los recortes numéricos existentes siguen disponibles en cada variante cuando hacen falta.

Cada intento usa exclusivamente píxeles de la misma imagen y debe validar la tarjeta completa. No se cambia resolución, confirmación de dos lecturas, identidad de direcciones, score ni caducidad de 1.4 segundos. El segundo intento puede consumir el tiempo disponible: en ese caso queda gris y se toma una imagen reciente. No hay recursión ni mezcla de cifras entre variantes. Las copias se liberan al terminar sus tareas y no se almacenan ni envían. No se reemplazan puntos ni unidades por suposición.

El reloj de caducidad de la lectura visible empieza después de publicar el cálculo; antes empezaba antes del cálculo. Se mantiene el mismo límite y la comprobación de antigüedad del fotograma antes de aceptarlo.

Una lectura ambigua sigue gris; no se retiene un color anterior para ocultar el fallo. Estas capturas no acreditan todas las fases posibles de la animación ni el tiempo de respuesta completo en el vivo.

## Priority

El cálculo toma el importe principal de $65.49 y no suma otra vez la línea de $10.34. La [guía oficial de UberX Priority](https://help.uber.com/driving-and-delivering/article/uberx-priority?nodeId=5c3c8fb3-d9e9-44c2-9985-a42a2a3ae50a) describe el recargo como incluido en la estimación principal; esa página incluye contexto británico y no es una liquidación de este viaje mexicano. La tarjeta permite comprobar detección y cifras, no un cobro final. No se añaden puntos por la categoría.

## Prueba siguiente

Estacionado, importa ambas capturas azules y luego observa una solicitud Exclusivo en Pantalla completa. Confirma $80.17, recogida 5 min / 1.4 km, viaje 16 min / 6.8 km y pasajero 4.84 / 64 visibles en los ejemplos. Si vuelve a gris, conserva el mensaje exacto y guarda el diagnóstico de esa sesión. No desactives las validaciones económicas para mantener el color.

La verificación final está en `verification/release-0.3.3.json`. El catálogo y los defaults se conservan; tus ajustes determinan el color, no el color observado en las capturas de otro perfil.

Pasaron 89 pruebas unitarias y 48 Android, sin errores de lint. La APK final firmada se instaló sobre 0.3.2 sin desinstalar entre esas dos versiones y conservó el costo de gasolina configurado en 28 y las 90 reglas. La importación de la foto azul de $80.17 también pasó en esa APK. Su SHA-256 instalado coincide con la entrega. Las fotos de usuario y clases de prueba no están incluidas en la APK entregada.

En la prueba de pantalla completa con ambas actividades minimizadas, las dos fotos azules se registraron con $80.17, 1.4 km de recogida, 6.8 km de viaje y 64 visibles. Priority se registró por $65.49, 0.9 km / 4.8 km y 102 visibles. Se usó un emulador de 896×1920 para aproximar el tamaño de caracteres de las capturas del teléfono. La configuración original de 1080×1920, que ampliaba más la tarjeta de prueba, produjo intentos tardíos descartados; no se acredita recuperación en tiempo real de todas las escalas. Las duraciones OCR registradas de 882–1243 ms en esas fotos azules no son tiempos completos de detección ni mediciones del vivo.
