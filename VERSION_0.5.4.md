# Semáforo 0.5.4 · Botón «Me interesa»

Publicada como beta el 15 de septiembre de 2026 sobre 0.5.3.

Cuatro capturas proporcionadas por el usuario mostraban una tarjeta completa con el botón «Me interesa», pero el lector solo admitía «Aceptar» y «Viaje disponible». El OCR reconocía correctamente «Me interesa» y los demás campos; el parser descartaba la tarjeta por falta de una acción final compatible. Una quinta captura con «Aceptar» sí funcionaba y se usó como control.

El lector acepta ahora «Me interesa» únicamente cuando ocupa una línea completa. Frases parecidas dentro de una dirección o texto más largo no cierran una tarjeta. Siguen siendo obligatorios una sola categoría compatible, un importe legible con moneda y centavos, recogida, viaje y botón final. Se conservan los controles de duplicados, orden y coherencia.

## Resultado de las cinco capturas

Las imágenes 1 a 4 pasaron de «Tarjeta incompleta: falta el botón de la oferta» a «Datos detectados». La imagen 5 continuó detectándose. Las diez pruebas de imagen —cinco completas y cinco con el recorte de lectura en vivo— aprobaron y comprobaron importe, tiempos, kilómetros, pasajero, tipo y Exclusivo.

En la imagen Priority, ML Kit leyó la tarifa visible `$11.08/km` como `$1l08/km`. La app deja ese dato opcional como desconocido: no convierte la letra en un número ni usa el bono separado de $10.34 como tarifa u oferta. El viaje sí se evalúa con el importe principal de $42.12, recogida de 4 min/1.3 km y trayecto de 9 min/2.6 km.

La regresión completa aprobó 97 pruebas Android en una sola ejecución. Aprobaron además 169 pruebas unitarias. Lint debug y release terminaron con 0 errores y 21 advertencias. La actualización desde 0.5.3 conservó todos los campos del perfil y las reglas de zona.

[APK firmado 0.5.4](https://github.com/galjoctavio-ux/semaforo/releases/download/v0.5.4/Detector-ofertas-0.5.4.apk) · [SHA-256](https://github.com/galjoctavio-ux/semaforo/releases/download/v0.5.4/Detector-ofertas-0.5.4.sha256.txt) · [Registro de verificación](verification/release-0.5.4.json).

La versión Android es versionCode 16 / versionName 0.5.4 y conserva el esquema de diagnóstico 8. Las capturas y sus textos OCR completos quedan fuera de Git. La publicación no acredita todavía una prueba de esta versión en el teléfono físico del conductor.
