# Semáforo 0.3.4 · Fondos oscuros, Comfort y horas

Instala la APK firmada de `entrega/Detector-ofertas-0.3.4.apk` sobre la anterior, sin desinstalar. Después inicia una nueva sesión de Pantalla completa.

## Causa comprobada

Las muestras completas de Comfort y Priority no se reconocían. El OCR sí extraía texto blanco sobre negro, pero Comfort no era una categoría admitida; `1 h 3 min` no era una duración admitida y los iconos de los puntos de recogida/destino aparecían como `9`, `o` o `°` delante de una línea. La tarifa por km con prefijo MXN tampoco se extraía para comprobar coherencia, aunque el importe principal con ese prefijo ya estaba admitido. La captura clara MXN tenía este último problema.

Se amplían los formatos explícitos, con reparaciones de `i/l/|` solo dentro de tiempos de una línea completa y una lista acotada de caracteres delante de sus etiquetas. No se transforma toda imagen a negativo: el reconocimiento original ya leía el fondo oscuro. Continúa disponible el único reintento de contraste de 0.3.3 cuando procede. Cada intento debe validar una oferta completa desde los píxeles de la misma imagen.

## Cifras que se comprueban

| Muestra | Importe principal | Recogida | Viaje | Pasajero | Resultado de lectura |
| --- | --- | --- | --- | --- | --- |
| Oscura UberX recortada | MXN 145.22 visible | 5 min / 2.5 km | 18 min / 18.5 km | 4.82 / 250 visibles | Sin recomendación: no aparece el botón |
| Comfort oscura | $235.82 | 4 min / 1.0 km | 63 min / 16.3 km | 4.88 / 19 visibles | Oferta detectada, aviso de destinos |
| UberX clara MXN | MXN 138.53 | 1 min / 0.1 km | 35 min / 26.1 km | 4.79 / 16 visibles | Oferta detectada |
| Comfort Exclusivo oscura | $235.82 | 2 min / 0.6 km | 63 min / 16.3 km | 4.88 / 19 visibles | Oferta detectada, aviso de destinos |
| Priority Exclusivo oscura | $66.03 | 11 min / 6.7 km | 5 min / 1.3 km | 4.91 / 167 visibles | Oferta detectada |

La línea `+$10.34 por inicio de viaje prioritario` no se añade otra vez al importe principal. Comfort, Priority y Exclusivo no reciben puntos por su etiqueta; sus cifras y tus ajustes determinan la conveniencia. Los filtros de pasajero y zona conservan sus umbrales.

## Aviso de destinos y límites

Comfort muestra `1 destino` entre recogida y viaje. No se ha confirmado el significado exacto de esa etiqueta en esta interfaz; no se interpreta como prueba de que solo hay un destino final. Se conserva su número como `destination_notice_count`, sin mezclarlo con la dirección. El cálculo utiliza los minutos/km visibles y las esperas configuradas, pero añade el motivo de revisar paradas, direcciones y esperas. Esos casos pueden ser ámbar o rojo, nunca verde. Cambiar ese aviso obliga a confirmar nuevamente la oferta. Más de un aviso, un número distinto de uno o paradas explícitas siguen sin recomendación.

La [ayuda de Uber sobre múltiples paradas](https://help.uber.com/en/riders/article/request-a-ride-with-multiple-stops?nodeId=26f09874-91e9-4fe1-9537-ec680a47ecbe) confirma que existen paradas adicionales y que pueden cambiar precio y esperas; no documenta el significado exacto de esta etiqueta mexicana ni valida una liquidación de estas ofertas.

El distintivo `Viaje largo (45+ min)` no reemplaza los 63 minutos ni se guarda dentro de la dirección. Se mantienen centavos legibles, comprobación de tarifa, exclusión de importes de otras pantallas, confirmación en dos lecturas, antigüedad máxima de fotograma de 1.4 segundos y catálogo/defaults. No se retiene un color anterior para disimular lecturas inválidas.

Las imágenes originales de terceros permanecen locales, fuera del repositorio y de la APK principal. Algunas proceden de iOS: sirven para probar lectura de imágenes; esta APK continúa siendo solo Android 14 o posterior. Las muestras estáticas y el emulador no acreditan todos los teléfonos, escalas ni fases de animación.

Pasaron 99 pruebas unitarias y 54 pruebas Android sobre la APK release, sin errores de lint en debug ni release. Las cuatro fotos completas pasaron enteras y en el recorte de lectura; la recortada se rechazó. La instalación firmada conservó el precio configurado en 28 y las 90 reglas. El APK instalado y la copia de descarga tienen la misma huella que la entrega; no incluyen fotos ni clases de prueba, y mantienen el certificado anterior.

En pantalla completa, con el inicio del emulador como actividad visible y las actividades del lector y de la muestra minimizadas, Comfort Exclusivo se registró por $235.82, recogida 2 min / 0.6 km, viaje 63 min / 16.3 km y aviso de destino. Priority Exclusivo se registró por $66.03, recogida 11 min / 6.7 km y viaje 5 min / 1.3 km. Las duraciones OCR registradas de 1150 y 888 ms no son latencia completa ni mediciones de teléfonos de choferes. El botón Detener terminó la proyección.

Las pruebas y comprobaciones de la entrega se registran en `verification/release-0.3.4.json`.
