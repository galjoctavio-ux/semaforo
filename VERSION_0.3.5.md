# Semáforo 0.3.5 · Reservar UberX

Instala `entrega/Detector-ofertas-0.3.5.apk` sobre la versión anterior, sin desinstalar. Después inicia una nueva sesión de Pantalla completa.

## Causa comprobada y corrección

La nueva tarjeta oscura falló tanto completa como en el recorte utilizado por el lector. El OCR sí extraía sus cifras, pero el parser no admitía el encabezado `Reservar UberX` ni la recogida `A 24 min y (14.4 km)`. Se añaden esos formatos explícitos; la conjunción opcional se limita a la línea completa de recogida. No se relajan los requisitos de centavos legibles, unidades, orden, botón visible ni rechazo de datos ambiguos.

| Campo | Valor comprobado |
| --- | --- |
| Categoría | UberX · Reserva |
| Importe principal | $265.62 |
| Recogida | 24 min / 14.4 km |
| Trayecto | 29 min / 21.4 km |
| Total visible | 53 min / 35.8 km |
| Pasajero | 4.91 / 74 visibles |
| Tarifa visible por km | Ausente; no se inventa |

El aviso `Reserva` con su icono se separa de la dirección del destino. Un nombre de calle como `La Reserva` no convierte una tarjeta normal en una reserva ni se elimina. Se conserva `reservation` en el historial numérico; cambiar de tarjeta inmediata a reserva obliga a confirmar nuevamente la lectura.

## Cómo se evalúa una reserva

La tarjeta no muestra la hora programada ni permite comprobar toda la espera previa. La [ayuda oficial de Uber Reserve para conductores](https://help.uber.com/es/driving-and-delivering/article/what-is-uber-reserve?nodeId=185d703d-d0ac-40cc-a622-e54ddf921f4a) describe condiciones de conexión antes del viaje; no prueba que ese tiempo sea espera improductiva ni documenta todos los tiempos de esta oferta concreta.

La app calcula con minutos/km visibles y tus esperas configuradas, como una estimación incompleta para reservas. No inventa un horario, minutos adicionales de espera o una tarifa de reserva. Añade `Reserva: horario y espera previa por verificar; sin verde` y muestra la advertencia en la ventana, detalle e historial. Estas tarjetas se limitan a ámbar o rojo; la reserva jamás mejora un rojo de costos, pasajero, zonas, autonomía o recogida. No hay puntos por categoría.

Con los límites iniciales de recogida de 15 minutos / 7 km, esta oferta es **roja por recogida fuera de límites**, aunque el cálculo económico sea favorable. Cambiar tus parámetros puede cambiar el resultado, pero la reserva sigue sin recomendar verde mientras no se implemente la comprobación del horario y espera. Otras categorías de reserva aún no se admiten.

## Verificación de la entrega

Pasaron **108 pruebas unitarias y 57 pruebas Android** sobre la APK release, incluyendo regresiones de fondos oscuros, Comfort, horas, MXN, Priority, UberXL, Exclusivo/efecto azul, importes y filtros. Sin errores de lint en debug ni release; permanecen 17 advertencias en cada variante. La nueva imagen pasó completa y en el recorte de lectura. Los casos incompletos/ambiguos siguen sin recomendación.

En un emulador Android 16 propio de 896×1920, con el inicio visible y las actividades del lector y la muestra minimizadas, MediaProjection leyó esta tarjeta flotante y registró sus cifras, la bandera de reserva y rojo por recogida. Detener terminó la proyección. La duración OCR registrada de 1130 ms no es latencia completa ni una medición de un teléfono de chofer.

La actualización firmada conservó el precio configurado en 28 y las 90 reglas de zonas. El catálogo se mantiene idéntico a 0.3.4. La APK instalada y la copia de descarga coinciden con la entrega firmada y mantienen el certificado anterior. No incluyen fotos privadas ni clases de pruebas; no se añaden permisos de Internet, cámara, micrófono o ubicación. Se mantienen dos lecturas coincidentes y antigüedad máxima de fotograma de 1.4 segundos.

La imagen original permanece local, fuera de GitHub y de la APK principal. Procede de iOS, pero esta app continúa siendo **solo Android 14 o posterior**. Falta probar esta reserva en teléfonos Android reales; no se acreditan todas las escalas, diseños o animaciones. El score no garantiza ingresos ni seguridad y no acepta/rechaza viajes.

Huella y comprobaciones: `entrega/Detector-ofertas-0.3.5.sha256.txt` y `verification/release-0.3.5.json`.
