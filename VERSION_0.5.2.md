# Semáforo 0.5.2 · Variantes de texto de las ofertas

Entrega local del 13 de septiembre de 2026, sobre los cambios de perfiles y Plan del mes de 0.5.1. La descarga pública continúa separada de esta entrega local.

Una tarjeta clara de UberX no se evaluaba aunque el OCR reconocía sus textos: usaba «Exclusiva», «153,05 MXN», «Est. 6,05 MXN/km», «A 3 min (0.7 km) de distancia» y «Viaje de 33 min (24.6 km)». El lector esperaba otras formas de esas etiquetas. El fallo se reprodujo con el APK 0.5.1 tanto sobre la imagen completa como sobre el recorte de captura.

El lector acepta ahora la moneda antes o después del importe, la tarifa por kilómetro con MXN al final, Exclusiva/Exclusivo, el sufijo exacto «de distancia» y «Viaje de» junto con «Viaje:». Los formatos anteriores siguen admitidos. La moneda y los centavos deben ser legibles; se conservan los controles de importes duplicados, coherencia con la tarifa por kilómetro y tarjeta completa. Los bonos separados no se suman al importe principal. No se convierten letras dudosas en dinero ni se rellenan campos con un viaje anterior.

La captura debe producir una oferta de $153.05, recogida de 3 minutos y 0.7 km, viaje de 33 minutos y 24.6 km, tarifa visible de $6.05/km, calificación 4.87 y contador visible 15. Los totales de los tramos son 36 minutos y 25.3 km antes de las esperas o escenarios del perfil. La etiqueta Exclusiva no añade puntos ni acredita seguridad.

No cambia el esquema de datos, los costos, los mínimos ni las reglas de puntuación de 0.5.1. La versión Android es 14 / 0.5.2, con diagnóstico de esquema 8.

## Entrega y verificación

[APK firmado 0.5.2](entrega/Detector-ofertas-0.5.2.apk) · [SHA-256](entrega/Detector-ofertas-0.5.2.sha256.txt) · [Registro de verificación](verification/release-0.5.2.json).

Pasaron 168 pruebas unitarias y una ejecución completa de 87 pruebas Android, incluidas esta captura y las anteriores. Lint debug y release terminaron con 0 errores y 21 advertencias cada uno. Las dos pruebas de la nueva imagen fallaron sobre 0.5.1 y pasaron sobre 0.5.2 con el OCR de ML Kit.

Una sesión real de MediaProjection, autorizada en la interfaz de Android del emulador propio, leyó esta imagen en una app de pruebas externa. Durante 16 segundos hubo 20 lecturas completas y 157 comprobaciones del resultado, sin desapariciones del resultado confirmado. Se verificó visualmente la tarjeta con el semáforo encima. El perfil usado era del emulador, por lo que el color y las cantidades netas de esa prueba no describen los costos del conductor.

La instalación sobre el APK 0.5.1 conservó todos los campos previos del perfil y las reglas de zona. El APK final mantiene la firma de las versiones anteriores. No se publicó esta entrega ni se validó todavía en un teléfono físico con una oferta real; la siguiente comprobación es instalarla sobre la versión anterior y repetir esta captura y una oferta en vivo.

La captura original y los textos OCR completos quedan excluidos de Git. Las pruebas unitarias usan direcciones de ejemplo; las pruebas Android de la captura requieren el recurso privado local.
