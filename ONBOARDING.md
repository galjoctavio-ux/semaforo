# Onboarding con perfiles · 0.5.1 local

Objetivo: empezar sin explicar ni capturar todas las variables del motor de costos. El perfil contiene los parámetros necesarios para calcular; eso no significa que sus costos estén medidos o que exista información de todas las zonas.

## Recorrido

1. **¿Qué carro manejas?** Compacto (March, Spark), sedán (Aveo, Versa, Vento, K3) o SUV a gasolina, híbrido sin enchufe, PHEV o 100% eléctrico. Se elige el tipo más parecido; los modelos mencionados son ejemplos de la categoría, no fichas técnicas.
2. **¿Cómo pagas tu carro?** Pagado, financiado o rentado. Solo se pide mensualidad o renta semanal cuando corresponde. En renta se puede indicar si incluye mantenimiento y llantas.
3. **Tu jornada y el uso del carro.** Horas previstas por semana, incluyendo espera de ofertas, y si el carro también tiene uso personal u otro trabajo. Uso exclusivo asigna 100%; uso compartido pide una aproximación de km de Uber / todos los km, con accesos 25%, 50%, 75% y entrada editable. Debe elegirse el uso y el porcentaje: no se deducen de las horas ni se presume 50%.
4. **¿Qué tan exigente quieres ser?** Flexible, equilibrado o selectivo. Cada opción muestra mínimo y meta por hora y km del aporte: después de energía, reservas por kilometraje y extras, antes de gastos fijos e impuestos. El saldo tras fijos se muestra aparte.
5. **Tu perfil está listo.** Resumen del carro, pago, horas, uso, energía y metas, con fijos asignados y la base del semáforo. El botón «Empezar con costos estimados» guarda el perfil. El desglose completo es opcional. No predice que los ingresos del mes alcanzarán.

PHEV y eléctricos añaden **Carga y autonomía** antes de las metas: cómo se carga y cuántos km eléctricos quedan ahora. Casa y carga pública usan estimaciones editables; «No pago la carga» elige explícitamente $0. La autonomía no se precarga como si estuviera confirmada: el usuario escribe la lectura actual. Para la Captiva PHEV del usuario, 100 km al iniciar una carga y $0 con paneles son entradas posibles; no se declaran especificaciones universales del fabricante.

## Supuestos completos del catálogo v2

Estas cifras son **supuestos iniciales de desarrollo**, no cotizaciones, promedios comprobados, recomendaciones del fabricante ni mediciones de esos modelos. El costo de gasolina es un ejemplo de $25/L. Casa y carga pública son ejemplos de $3 y $6/kWh. Los valores deben calibrarse con registros reales antes de atribuir precisión al perfil.

| Perfil | Gasolina, km/L | Consumo eléctrico, kWh/100 km | Servicio / 10,000 km | Llantas / duración | Otro desgaste por km |
|---|---:|---:|---:|---|---:|
| Compacto a gasolina | 12 | No se usa | $2,500 | $8,000 / 50,000 km | $0.70 |
| Sedán a gasolina | 11 | No se usa | $3,000 | $10,000 / 50,000 km | $1.00 |
| SUV a gasolina | 8 | No se usa | $4,000 | $14,000 / 50,000 km | $1.40 |
| Híbrido sin enchufe | 18 | No se calcula por separado | $3,500 | $10,000 / 50,000 km | $1.10 |
| PHEV | 12 | 20 | $3,000 | $12,000 / 50,000 km | $1.16 |
| Eléctrico | No se usa | 18 | $2,000 | $12,000 / 40,000 km | $1.20 |

Los perfiles nuevos reservan $3,000/mes para gastos fijos compartidos de seguro, teléfono y otros gastos, visibles en el resumen. No es un promedio comprobado. Las horas mensuales se calculan con horas semanales × 52/12. Una renta semanal se convierte igual; una mensualidad se suma una vez a esa base compartida. Uber recibe **fijos compartidos × porcentaje de uso + fijos exclusivos de Uber completos**. Los exclusivos pueden ajustarse sin que el porcentaje los reduzca.

Mantenimiento, llantas y desgaste por km se calculan por la distancia del viaje, incluyendo recogida y reposición: no se vuelven a reducir por el porcentaje de Uber. El catálogo usa una reserva de desgaste por km; no calcula separadamente depreciación por antigüedad a partir del precio de compra o reventa. El perfil no afirma que esta reserva sea una valoración del vehículo.

Con financiamiento, el aporte para comparar viajes conserva una reserva de desgaste (`financedWearPerKm`); pagar mensualidad no elimina el desgaste causado por otro km. La vista de saldo después de pagos usa la mensualidad en su lugar y no vuelve a descontar esa reserva. No es utilidad contable ni se calcula interés/capital por separado. En renta se elimina la reserva de valor del vehículo y, si incluye mantenimiento y llantas, también sus reservas. Los otros costos por km se conservan.

| Nivel | Mínimo por hora | Meta por hora | Mínimo por km | Meta por km |
|---|---:|---:|---:|---:|
| Flexible | $100 | $140 | $4 | $6 |
| Equilibrado | $120 | $180 | $5 | $8 |
| Selectivo | $150 | $220 | $6 | $10 |

Estos niveles son preferencias de partida, no ingresos típicos comprobados. Cambiar el nivel no anula restricciones de recogida, pasajero, autonomía o zona.

## Viaje y plan del mes

El perfil nuevo compara ofertas con **aporte antes de fijos**. También muestra el saldo después de fijos y pagos. Una oferta que pierde dinero después de costos por km o incumple mínimos sigue roja. Una que cumple mínimos y aporta, pero tiene saldo negativo tras los fijos asignados al tiempo, se limita a ámbar; la advertencia no puede eliminar otro rojo. Una oferta que cumple metas puede ser verde mientras el presupuesto del mes resulte insuficiente: son decisiones distintas.

**Plan del mes**, disponible en el inicio, permite cambiar uso, horas y base de comparación sin rehacer el vehículo. Los mismos mínimos se comparan con la base elegida; no se bajan automáticamente. Los perfiles anteriores conservan `TOTAL`, 100% y sus importes al instalar. Si sus fijos ya son solo la parte de Uber, deben mantener 100% o capturar primero el gasto total compartido, para no prorratear dos veces. Si se cambia un perfil financiado anterior a aporte y no hay reserva de desgaste, queda pendiente de revisión; rehacer el preset v2 carga una reserva estimada por tipo.

El presupuesto pide un **aporte para pagos por hora de jornada**, después de costos por km y extras, incluyendo espera entre ofertas y antes de fijos e impuestos. Es un supuesto del conductor, no un ingreso inferido de una oferta, una previsión de demanda ni una medición. Con financiamiento, este presupuesto de efectivo usa la mensualidad, sin descontar además la reserva de depreciación. El campo vacío permanece desconocido; 0 explícito se distingue de desconocido.

Saldo mensual = aporte previsto por hora de jornada × horas al mes − fijos asignados. Se muestra aporte mínimo por hora para cubrir fijos y horas de equilibrio **sin sueldo objetivo**. La parte compartida fuera de Uber y el pago completo siguen visibles. Si la parte Uber se cubre pero, sin otra fuente, todavía faltaría dinero para todos los fijos del carro, se muestra ese faltante y se mantiene ámbar. Compartir costos no prueba que otro ingreso pague el resto.

Ver [EJERCICIO_POCAS_HORAS.md](EJERCICIO_POCAS_HORAS.md): $5,000 de mensualidad + $3,000 de fijos estimados, jornadas de 5/10/20 h y aporte supuesto $150/h.

En una instalación nueva se conservan los demás ejemplos del motor: recogida máxima 15 min / 7 km; espera 3 min; escenario extra 5 min; pesos 55/30/15; verde desde 75 y ámbar desde 45. Reposición y regreso desactivados/cero, extras y descuento adicional cero. Pasajero y zonas mantienen las reglas iniciales documentadas. El motor muestra que el regreso no fue considerado y evita verde ante datos ausentes según los filtros activos.

## Conservación y claridad

- Primera instalación: el asistente se abre una vez. Se puede salir y volver desde «Configurar con un perfil».
- Actualización: no se abre automáticamente si ya hay un perfil. Instalar no reemplaza la Captiva, carga a $0, costos ni metas existentes.
- Rehacer el asistente reemplaza vehículo, costos y metas solo al pulsar el botón final, con aviso en el resumen. Se conservan las reglas de pasajero, zonas, recogida, pesos, esperas, regreso, extras, descuento adicional y modo de captura existentes.
- Cambiar de vehículo con un preset invalida el supuesto de aporte por hora para pagos; no se reutiliza un ingreso supuesto con costos de otro carro. Se conservan los gastos exclusivos de Uber explícitos.
- Cancelar, ir atrás y elegir opciones no escribe el borrador. Las entradas se conservan al retroceder y al recrear la actividad. Si cambia la revisión del perfil durante el asistente, se bloquea la sustitución con el borrador antiguo.
- Aceptar supuestos se registra como `presetAccepted`, `presetId`, `presetVersion` y `onboardingCompleted`. No activa `energyReviewed`, `costsReviewed` ni `calibrated` como si hubiera comprobación con registros.
- El inicio, el cálculo y el detalle indican «costos estimados». El preset aceptado permite calcular sin obligar a abrir todas las variables; no certifica costos, seguridad, ganancias ni viajes completados.
- El detalle muestra dinero, reglas y pendientes; «Datos del cálculo» conserva puntos, pesos y fuentes completas. La ventana flotante identifica la base elegida y muestra aporte y saldo tras fijos.
- El diagnóstico esquema 8 incluye ambos importes, base de decisión, fijos asignados, reserva de desgaste financiado y versión 0.5.1. Los registros antiguos no se reescriben.

## Siguiente validación del producto

Probar que conductores nuevos completan el asistente sin ayuda y entienden dos puntos: lo disponible descuenta costos estimados, y rojo puede significar una regla personal. Después calibrar los supuestos por categoría con jornadas y gastos informados; no ajustar valores solo para aumentar la cantidad de verdes.

La entrega 0.5.1 es local. La publicación vigente y la landing se documentan por separado en PUBLICACION.md.
