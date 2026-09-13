# Semáforo de viajes 0.2.0

Documento histórico de 0.2.0. **0.2.1 agrega una base de 64 colonias con datos oficiales de 2021**, cargada por solicitud del usuario aun siendo antigua. Su fecha, selección, edición y comportamiento se describen en [VERSION_0.2.1](VERSION_0.2.1.md). Los costos y fórmulas de este documento continúan aplicando.

12 de septiembre de 2026. APK privada para Android 14 o posterior. Primera versión que evalúa ofertas con costos, objetivos y reglas locales de colonia. Mantiene el paquete y la firma de 0.1.0 para instalar como actualización.

## Instalar y configurar

1. Instala `entrega/Detector-ofertas-0.2.0.apk` sobre la versión anterior. No hace falta desinstalarla. La actualización conservó el permiso de superposición en el emulador Android 16; falta comprobar esta actualización concreta en el vivo.
2. Desplaza la pantalla principal hasta **Configuración y revisión**. Abre **Vehículo y costos** y ajusta el perfil. Después abre **Objetivos y score**. Guarda cada pantalla.
3. Marca **He revisado mis costos y objetivos** cuando hayas revisado ambos apartados. Mientras siga desmarcado, los cálculos se muestran, pero no habrá verde.
4. En **Colonias y zonas**, añade reglas personales. El filtro está activado de inicio y no contiene colonias precargadas. Una recogida o destino sin regla aplicable impide el verde. Si desactivas expresamente el filtro, el resultado indicará que las zonas no se evaluaron.
5. Comprueba ejemplos en **Simulador** o con **Probar una captura guardada**, estando estacionado. Inicia la lectura como en 0.1.0, compartiendo exclusivamente Uber Driver. **Detener** finaliza la captura.
6. Para modificar el perfil durante una sesión, detén antes la lectura. Revisa **Historial → Ver desglose** después; no hace falta interpretar todo el cálculo durante los segundos de una oferta.

La revisión económica es una declaración del usuario, no una validación automática de sus cifras. La autonomía restante se actualiza manualmente: la aplicación no conoce el estado del carro ni descuenta batería por cada oferta observada.

## Valores de inicio

Son supuestos editables para comenzar, no precios actuales, especificaciones verificadas de Chevrolet ni umbrales de rentabilidad de Guadalajara. Los 100 km y la electricidad a cero reflejan el escenario informado por el usuario, sujetos a revisión.

| Parámetro | Inicial |
| --- | --- |
| Vehículo / energía | Captiva PHEV · perfil editable / PHEV |
| Año / odómetro | 0 / 0: sin registrar, informativos |
| Gasolina / rendimiento con gasolina | $25 MXN/L / 12 km/L |
| Electricidad / consumo eléctrico | $0 MXN/kWh / 20 kWh por 100 km |
| Autonomía cargado / restante | 100 / 100 km |
| Servicio / intervalo | $3,000 / 10,000 km |
| Juego de llantas / vida estimada | $10,000 / 50,000 km |
| Otro desgaste o depreciación por uso | $1/km |
| Gastos fijos asignados / horas al mes | $3,000 / 160 h |
| Espera del pasajero / extra conservador | 3 / 5 min |
| Reposicionamiento | 0 km / 0 min |
| Extras no reembolsados / descuento adicional | $0 / 0% |
| Mínimo / meta disponible por hora | $120 / $180 |
| Mínimo / meta disponible por km | $5 / $8 |
| Límite de recogida | 15 min y 7 km |
| Pesos hora / km / recogida | 55 / 30 / 15 |
| Score verde / ámbar | 75 / 45 |
| Penalización por precaución | 20 puntos |
| Filtro de zonas / costos revisados | Activado / no |
| Reglas de colonia precargadas | Ninguna |

Cada nueva regla propone inicialmente **Evitar**, ambos extremos, todo el día, motivo **Preferencia personal**, fecha del día y vigencia de 90 días. Se puede modificar todo antes de guardar. Estos valores no asignan peligrosidad a ningún lugar.

Se admiten perfiles PHEV, gasolina o híbrido no enchufable y solo eléctrico. Año y odómetro no alteran automáticamente el mantenimiento: el costo y sus intervalos los configura el usuario. No se incluye un catálogo de vehículos.

## Cálculo reproducible

```text
D = km de recogida + trayecto + reposicionamiento configurado
T = min de recogida + trayecto + espera del pasajero + reposicionamiento
Ingreso comparable = importe visible × (1 − descuento adicional / 100)
Mantenimiento y desgaste = D × (servicio / intervalo + llantas / vida + otro desgaste por km)
Fijos asignados = gastos mensuales / horas mensuales × T / 60
Disponible estimado = ingreso comparable − energía − mantenimiento y desgaste − fijos − extras
Disponible por hora = disponible estimado × 60 / T
Disponible por km = disponible estimado / D
```

En PHEV se asignan primero kilómetros eléctricos hasta la autonomía restante configurada y después gasolina. Es una aproximación; no modela aceleraciones ni la mezcla real del tren motriz. El perfil eléctrico no inventa combustible de respaldo: distancia superior a la autonomía restante causa rojo. El escenario conservador agrega los minutos extra y sus gastos fijos asignados.

El descuento adicional empieza en cero para evitar descontar nuevamente una comisión que ya esté incluida en el importe mostrado. Revisa el significado del importe contra el desglose de Uber. Propinas, incentivos condicionados, impuestos y cargos no informados no se inventan. No dupliques depreciación o financiamiento en gastos fijos y desgaste. No añadas tráfico genérico si ya está incorporado en los minutos mostrados.

## Score y límites

Los pesos se normalizan por su suma. Cada tasa recibe 50 puntos al alcanzar su mínimo, 80 al alcanzar su meta y 100 al llegar a 1.25 veces su meta, con interpolación entre esos puntos. Por debajo del mínimo, los puntos bajan proporcionalmente hasta cero. La recogida recibe `max(0, 100 − 80 × max(minutos/límite, km/límite))`.

El promedio ponderado es el **score económico**. Rendimientos, mantenimiento y desgaste ya influyen en los costos: no se suman como penalizaciones duplicadas. Después se aplican las restricciones, obteniendo el score final y el color:

- **Rojo:** debajo de los mínimos económicos, saldo no positivo, recogida fuera de límites, autonomía insuficiente en modo eléctrico, regla vigente de evitar o score demasiado bajo. Un límite duro reduce el score final por debajo del umbral ámbar.
- **Ámbar:** cumple mínimos, pero no alcanza todas las condiciones del verde; también es el máximo permitido con costos no revisados, zonas pendientes o un escenario conservador que cae bajo el mínimo. Una regla de precaución resta puntos y permite como máximo ámbar; puede resultar rojo si cae por debajo del umbral.
- **Verde:** alcanza el score verde, la meta por hora y los mínimos; supera las demás comprobaciones y usa costos marcados como revisados. Con filtro activo, ambos extremos deben tener reglas aplicables suficientes.
- **Gris:** no existe una lectura completa, confirmada y vigente o la configuración es inválida.

El score es una calificación según supuestos y preferencias, no una probabilidad de ganar dinero o de estar seguro. Las reglas de evitar tienen prioridad sobre cualquier pago; no pueden compensarse con mayor rentabilidad.

Con los valores de inicio, la oferta de **$98.31**, 6.3 + 4.0 km y 14 + 18 min pasa a **35 min** al incluir 3 de espera. Su disponible estimado es **$71.92**, **$123.30/h** y **$6.98/km**; con 5 minutos extra cae a **$105.54/h**. El resultado es **ámbar**, condicionado por los supuestos y las zonas sin verificar. Son cálculos, no ganancias realizadas. La oferta de $69.56 da $119.62/h y rojo por quedar debajo del piso inicial de $120/h.

## Alcance del filtro de colonias

Se compara el texto legible de la colonia de recogida y destino con nombres configurados. Acepta acentos, prefijos de colonia, código postal y nombres que ocupan varios renglones. Si se configura municipio, debe aparecer también en la dirección; de lo contrario la coincidencia es ambigua. Una calle con un nombre parecido no establece por sí sola la colonia.

Cada regla permite evitar, precaución o revisada por mí; recogida, destino o ambos; horario, fuente/motivo, fecha y vigencia. **Revisada por mí** expresa una preferencia del conductor, no una certificación de seguridad. Horarios se comparan con la hora local al evaluar, no con una predicción de la hora de llegada. Revisión vencida o coincidencia ambigua no permite asumir una zona segura.

No se geocodifican direcciones ni se inspecciona el recorrido intermedio. Errores de OCR, direcciones sin colonia, límites geográficos y cambios de nombres pueden impedir una coincidencia. Puede faltar una zona riesgosa en la lista; por eso se conserva el estado sin verificar.

La [página del IIEG consultada con microdatos por colonia](https://iieg.gob.mx/ns/?page_id=22174) indica actualización de noviembre de 2021 y microdatos a octubre de 2021. Ese material no se cargó como clasificación vigente en 2026. Se revisó también la [plataforma de seguridad del IIEG](https://iieg.gob.mx/plataforma_seguridad/), sin obtener y validar en esta tarea una base actual con cobertura de colonias, fechas y criterios suficientes para precargarla. Esto no demuestra que no exista otra fuente; significa que todavía no hay una integrada y validada en esta APK. Una futura capa documentada necesita cobertura, fecha, denominadores y un criterio claro de riesgo, además de revisión local.

## Lectura, privacidad e historial

- OCR incluido en la APK, cálculo sin Internet ni GPT; no tiene permiso de red, ubicación o accesibilidad y no pulsa controles de Uber.
- En captura en vivo exige dos lecturas consecutivas coincidentes de cifras y direcciones normalizadas. Al cambiar los datos vuelve a confirmar. Reduce errores transitorios, pero no prueba la exactitud de un OCR repetidamente equivocado.
- Conserva la caducidad de 1.4 s de la prueba anterior. Al faltar imágenes recientes, cambiar la sesión o desaparecer la oferta se retira el resultado. Una pantalla completamente estática puede dejar de producir imágenes; para una foto utiliza el importador.
- Historial local de hasta 200 evaluaciones con importes, tiempos, distancias, desglose, configuración y reglas utilizadas. Se escribe con archivo atómico y se puede borrar o exportar mediante **Guardar diagnóstico**. Hay un identificador local; no es el identificador oficial de oferta de Uber.
- No guarda capturas, texto OCR completo ni direcciones de pasajeros. Sí guarda los nombres de colonias, municipios y motivos que el usuario introduce en sus reglas. La exportación incluye esas reglas y cifras económicas: compartirla es una acción manual.
- La desaparición de una tarjeta no demuestra aceptación, rechazo o cancelación. El historial mantiene resultado desconocido. Las simulaciones no se incorporan como ofertas observadas; las imágenes importadas se distinguen de captura en vivo.
- Los milisegundos informados corresponden a OCR y cálculo, no a la latencia completa desde que Uber presenta la solicitud.

## Comprobaciones realizadas

- 44 pruebas unitarias aprobadas: parser, vigencia, confirmación, costos PHEV/eléctrico/gasolina, límites, monotonicidad de costos y reglas de zona, incluidos horarios y revisiones vencidas.
- Ocho pruebas instrumentadas aprobadas en Android 16 sin conexión: OCR de las dos ofertas aportadas, guardado mediante formulario, validación, confirmación y persistencia/deduplicación/privacidad del historial, reglas y revisión del perfil. Evidencia: `verification/instrumented-0.2.txt`.
- Compilación release y lint aprobados; lint reportó cero errores y 14 advertencias. No se afirma que el análisis esté libre de advertencias.
- Instalación de 0.1.0 seguida de actualización 0.2.0 aprobada; se conservó el permiso de superposición en el emulador.
- APK release probada con MediaProjection sobre una app de prueba que presenta la foto aportada y un marcador animado. Mostró rojo, $119.62/h, $51.84 por viaje; retiró el resultado ante pantalla vacía. Al detener, Android reportó proyección nula. No fue una solicitud real de Uber en este emulador.
- Historial conservó una sola evaluación tras múltiples fotogramas; se abrió el desglose guardado. Formulario de colonia guardó una regla ficticia solo en el emulador; el simulador la aplicó como rojo con $1,000 de oferta y score económico 89. La regla ficticia no se distribuye en la APK.
- Se revisaron capturas locales de inicio, vehículo, historial/desglose, filtro y simulador. El último ajuste añadió las tasas por hora y por km al desglose; se verificó en el simulador de la APK final.

Pendiente: instalar esta versión en el vivo, medir demora completa y continuidad con varias solicitudes reales, contrastar cifras y decisiones contra la jornada y validar fuentes actuales para una futura capa geográfica de riesgo.

## Próximo incremento

Registrar manualmente el resultado real de viajes y jornadas: ingreso final, km recorridos, espera y tiempo conectado, incluyendo periodos sin pasajero. Eso permitirá calibrar costos y medir si la selección aumenta el disponible por hora de jornada. Después, reposicionamiento por zona y horario y datos geográficos documentados. Incorporar más variables sin mediciones solo haría que el score pareciera más preciso de lo que es.
