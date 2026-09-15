# Semáforo 0.5.3 · Banner mínimo

Entrega local del 13 de septiembre de 2026 sobre 0.5.2. El banner pasa de varias líneas a una franja de 216 × 56 dp: fondo con el color de la evaluación, una cifra por hora y × para detener.

Ejemplo de formato: **Est. $146/h ×**. El importe se redondea a pesos enteros solo al mostrarlo. Usa la base elegida en el perfil: aporte antes de fijos o saldo tras fijos y pagos. El color conserva todas las reglas de la evaluación, incluidos los límites que pueden producir rojo aunque el importe por hora sea alto. Los cálculos mantienen su precisión original.

No se muestran en la franja el tipo de Uber, la versión, el score, los motivos, los pendientes ni varios importes. El desglose continúa en «Ver cálculo y motivos» dentro de la app. Sin oferta confirmada, la franja es gris y muestra «Esperando…», «Leyendo…» o «Sin datos», según el estado.

Se arrastra desde el texto y se detiene desde ×. El control de detener tiene una zona táctil de 48 × 48 dp y etiqueta accesible. Para lectores de pantalla se conserva el nombre del color, la base del importe y el motivo principal. Se mantiene la protección contra redibujos repetidos del banner durante la captura de pantalla completa.

## Verificación y entrega

[APK firmado 0.5.3](entrega/Detector-ofertas-0.5.3.apk) · [SHA-256](entrega/Detector-ofertas-0.5.3.sha256.txt) · [Evidencia](verification/release-0.5.3.json).

Pasaron 168 pruebas unitarias existentes y las 15 pruebas Android de los flujos afectados de app y Plan del mes. Se ajustó la prueba que esperaba el resumen antiguo para comprobar la cifra mostrada con ambas bases de cálculo. Lint debug y release terminaron con 0 errores y 21 advertencias cada uno.

En un emulador propio Android 16, una sesión real de MediaProjection leyó la captura privada de $153.05 en una app externa: 23 lecturas completas y 171 comprobaciones durante 20 segundos, sin perder el resultado confirmado. Se revisó visualmente la franja gris y roja, el arrastre y el cierre con ×. Al tocar × desapareció el banner y Android confirmó que la proyección había terminado. El importe neto de las imágenes de prueba pertenece al perfil del emulador.

Se conservan el parser y los cálculos de 0.5.2, la firma de actualización y el esquema de diagnóstico 8. Android pasa a versionCode 15 / 0.5.3. Esta entrega es local; queda por comprobar el tamaño y uso de la franja en el teléfono del conductor.
