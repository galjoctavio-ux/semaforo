# Semáforo 0.3.1 · corrección de lectura

Instala `entrega/Detector-ofertas-0.3.1.apk` sobre 0.3.0 sin desinstalar. Conserva paquete, certificado, costos, filtros y reglas personales. Inicia una nueva sesión de pantalla completa después de actualizar.

## Fallos reproducidos con las cinco capturas nuevas

- El OCR interpretó «11 min» como «1l min», causando «Faltan minutos o km del viaje». Se admite l/i como 1 solamente dentro del token de minutos de una línea completa de recogida o viaje. No se transforma texto de direcciones ni dígitos del importe.
- El OCR interpretó $77.06 como $7706. El lector anterior permitió una oferta de $7,706 y estimaciones de miles de pesos/hora. Este fallo afecta la confiabilidad del cálculo de 0.3.0, aunque el rojo por pasajero nuevo estuviera correcto.
- En el fotograma recibido por MediaProjection, 1.3 km se leyó como 13 km. El filtro de coherencia impidió usar esa distancia, pero dejó sin evaluar una oferta válida. Una captura PNG de la misma pantalla no reproducía el error: se conservó el fotograma real como caso de regresión en la APK de pruebas.
- Cifras de nuestra propia ventana podían contaminar la búsqueda de importes en pantalla. Ahora se analiza la tarjeta desde su etiqueta de categoría hasta su botón vigente, excluyendo texto antes/después. Varias tarjetas o importes sin identificación siguen siendo ambiguos.

La oferta necesita centavos explícitos. Si el primer OCR los omite, se realiza **un reintento local sobre los píxeles de la línea del importe ampliada**. Si tiene centavos pero las cifras no cuadran, el único reintento reúne las franjas ampliadas del importe, tarifa, recogida y viaje. Las sustituciones deben conservar el tipo de campo y producir una oferta completa y válida. Se mantienen las direcciones y datos del pasajero de la misma imagen. No se guardan imágenes ni se envían datos. Si sigue sin poder validarse, se muestra gris: no se asume $77.06 a partir de 7706 ni 1.3 a partir de 13, y no se usa la tarifa por km para inventar el pago.

Cuando hay tarifa estimada por km, se compara con el importe y la distancia total. La tolerancia es amplia por redondeos/estimaciones (25% del importe esperado más 0.2 km por la tarifa). Es un filtro heurístico de incoherencias; no prueba que el importe sea correcto y nunca lo sustituye. Formatos con importe entero sin centavos legibles quedan sin evaluación hasta comprobarlos con ejemplos.

El reintento forma parte del tiempo de OCR y sigue sujeto al límite de antigüedad de 1.4 s de la captura. Se mantienen dos lecturas coincidentes, caducidad del resultado y Detener. El ciclo respeta al menos 420 ms entre inicios; si el OCR tarda más, pide el siguiente fotograma al terminar, sin sumar otra espera de sondeo. No se aumentó el límite para aceptar imágenes más antiguas. Los tiempos del emulador no acreditan una respuesta menor de diez segundos en el vivo.

## Exclusivo y Priority

Las cinco fotos recibidas muestran UberX; dos incluyen «Exclusivo». **No se recibió una captura de Priority.** Uber describe Exclusive como la etiqueta de solicitudes individuales en su [ayuda de Trip Radar para Australia](https://help.uber.com/en-AU/driving-and-delivering/article/trip-radar-?nodeId=010666f0-5925-4bc6-957f-3244039b1e6a). Sirve para explicar la distinción, sin trasladar automáticamente reglas de aceptación de ese mercado a México.

Se admiten las etiquetas explícitas UberX Priority, Uber Priority y Priority, además de UberX con Exclusivo y botón Aceptar. Las variantes Priority se prueban con texto sintético; falta comprobar una tarjeta real de esta interfaz mexicana. Una etiqueta separada Exclusivo puede preceder al nombre por el orden vertical del OCR. Ambas son metadatos, no puntos adicionales.

La [ayuda de UberX Priority que menciona Reino Unido](https://help.uber.com/driving-and-delivering/article/uberx-priority?nodeId=5c3c8fb3-d9e9-44c2-9985-a42a2a3ae50a) describe un recargo **ya incluido en la estimación de la tarjeta**. No confirma importe ni tratamiento específico en Guadalajara. Por tanto, se evalúa el importe total visible y no se añade un recargo presumido. A igualdad de pago, tiempo, distancia, pasajero y zona, Priority y UberX obtienen el mismo score. Un mayor pago mejora las métricas económicas; la etiqueta por sí sola no acredita mejor pasajero, seguridad o menor desgaste.

La ventana muestra tipo de oferta; el simulador permite cambiar tipo sin puntos adicionales. Historial y diagnóstico añaden categoría y versión del lector. Cambiar Exclusivo durante la transición radar/solicitud individual no cambia la identidad numérica de la oferta. Priority sí requiere su propia confirmación. La identidad sigue siendo heurística, sin identificador oficial de Uber.

## Colonias e historial

[Revisión de las cinco zonas aportadas](REVISION_COLONIAS_0.3.1.md): Portillo López, San José del Castillo, Toluquilla y Oblatos están incluidos. Pila Seca no está. No se equipara El Castillo con San José del Castillo ni se extienden reglas a corredores/alrededores sin delimitar. El catálogo conserva 90 lugares, fuentes, niveles y preferencias existentes.

Los registros previos no se corrigen automáticamente sin sus píxeles originales. Ver desglose identifica registros anteriores como importe no revalidado con 0.3.1. No deben usarse esas cifras anteriores como ingresos comprobados; el historial representa ofertas observadas. Se puede borrar voluntariamente desde Historial si se desea empezar una prueba limpia.

## Verificación y próxima prueba

Resultados finales, firma y pruebas en `verification/release-0.3.1.json`. Las fotos son assets de una APK de pruebas separada y están ausentes del APK entregado. Pruebas con Android 16 aislado; falta prueba de esta versión en el vivo.

- 81 pruebas unitarias y 32 pruebas Android aprobadas. Lint: cero errores, 17 advertencias; compilación release aprobada.
- MediaProjection real en el emulador: UberX $59.90, recogida 1.3 km y viaje 5.6 km; UberX Exclusivo $77.06, recogida 1.0 km y viaje 8.2 km. El resultado se confirmó sobre el inicio con ambas actividades minimizadas. Se observó retirada del resultado al desaparecer la tarjeta y fin de la proyección con Detener.
- Con valores iniciales, $77.06 / 5.00 (3) muestra rojo 44/100, disponible estimado $56.07 y $146.28/h. Son cálculos sobre una oferta de prueba, no ingresos cobrados.
- Actualización firmada 0.3.0 → 0.3.1 sin desinstalar entre ambas: conservó gasolina personalizada de $28/L y catálogo de 90 reglas. El APK release importó la foto real de $77.06 mediante el selector de archivos y mostró el importe correcto y rojo por pasajero nuevo. El hash del APK instalado coincide con la entrega.

La primera sesión del emulador tardó en empezar a producir lecturas y hubo estados grises/de confirmación durante la captura. Estos ensayos no acreditan latencia ni disponibilidad continua en el vivo. La prueba más importante pendiente es medir si se obtiene un resultado correcto dentro de los diez segundos reales de la solicitud. No se considera comprobado un formato Priority mexicano sin una tarjeta real.

Prueba estacionado tus cinco capturas desde el importador, especialmente $77.06 con 5.00 (3), que debe mantener rojo por tu regla y mostrar cifras del importe real. Luego prueba Uber visible, Exclusivo/Aceptar y oferta flotante sobre el inicio, con modo Pantalla completa. Para Priority necesitamos una tarjeta real antes de afirmar compatibilidad completa de su formato.
