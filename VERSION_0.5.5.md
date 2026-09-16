# Semáforo 0.5.5 · Ofertas Black

Publicada como beta el 15 de septiembre de 2026 sobre 0.5.4.

Una captura real mostraba una oferta `Black` con distintivo `Exclusivo`, importe, tarifa por kilómetro, pasajero, recogida, trayecto y botón Aceptar completos. El OCR leyó esos datos, pero el lector permanecía en «Esperando…» porque Black no era una categoría admitida.

El lector acepta ahora `Black`, `Black Exclusivo` y `Uber Black`. Black se registra como tipo propio y aparece también en el simulador. No recibe puntos, costos, mínimos ni garantías especiales por su nombre: la evaluación sigue usando el importe visible, ambos tramos, el perfil del conductor, las zonas y el pasajero. Variantes no comprobadas como `Black SUV`, `Comfort Black` y `Black VIP` continúan sin admitirse.

La captura pasó de dos fallos —imagen completa y recorte usado durante la captura en vivo— a dos lecturas completas. Ambas extrajeron $148.13, $13.59/km, recogida de 11 min/3.3 km, trayecto de 20 min/7.6 km, pasajero 4.71 con contador 191, Black y Exclusivo.

La regresión aprobó 172 pruebas unitarias y 99 pruebas Android en una sola ejecución. Lint debug y release terminaron con 0 errores y 21 advertencias cada uno. La actualización firmada desde 0.5.4 conservó el perfil y las reglas de zonas del emulador.

El [APK firmado](https://github.com/galjoctavio-ux/semaforo/releases/download/v0.5.5/Detector-ofertas-0.5.5.apk) y su [archivo SHA-256](https://github.com/galjoctavio-ux/semaforo/releases/download/v0.5.5/Detector-ofertas-0.5.5.sha256.txt) están publicados en GitHub Releases. Su SHA-256 es `e97db05f2dd0b93450d18c212cbc301441ac907deff3cb07d0ea6f9204cb6543`. La versión Android es versionCode 17 / versionName 0.5.5 y conserva el esquema de diagnóstico 8.

La captura original y el texto OCR completo permanecen fuera de Git. Esta prueba reproduce la imagen en un emulador Android 16 propio; queda pendiente comprobar 0.5.5 durante una oferta Black en el teléfono físico.
