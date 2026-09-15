# Pocas horas, carro financiado y dos decisiones diferentes

Ejercicio reproducible con el motor 0.5.1 y `CostAllocationTest`. Todos los importes son MXN antes de impuestos. **Los ingresos y gastos son supuestos del ejercicio**, no promedios comprobados de Uber ni gastos del usuario.

## Supuestos

- Compacto a gasolina del catálogo v2: gasolina $25/L y 12 km/L; servicio $2,500/10,000 km; llantas $8,000/50,000 km; reserva de desgaste $0.70/km.
- Mensualidad de $5,000 y otros gastos fijos compartidos estimados de $3,000: $8,000/mes completos. La mensualidad es una salida de efectivo; no se identifica toda como gasto contable ni se presume que desaparece al repartir el uso.
- Uso exclusivo de Uber inicialmente. Conversión mensual: horas semanales × 52/12.
- Aporte **para pagos** de $150 por hora de jornada: después de costos por kilometraje y extras, antes de fijos e impuestos, incluyendo espera entre ofertas. Con carro financiado, este presupuesto de efectivo descuenta la mensualidad y no descuenta adicionalmente la reserva de depreciación. Es una entrada independiente del aporte económico de un viaje.
- No incluye sueldo objetivo. Cubrir fijos no significa que el trabajo remunere suficientemente al conductor.

## Presupuesto mensual

| Uber por semana | Horas al mes | Aporte para pagos | Fijos asignados | Saldo del supuesto |
|---|---:|---:|---:|---:|
| 5 h | 21.67 | $3,250 | $8,000 | **−$4,750** |
| 10 h | 43.33 | $6,500 | $8,000 | **−$1,500** |
| 20 h | 86.67 | $13,000 | $8,000 | **$5,000** |

Con ese aporte, cubrir $8,000 requiere 53.33 horas al mes, aproximadamente **12.31 horas por semana, sin sueldo**. No es una garantía de ingresos ni una recomendación de trabajar exactamente esas horas. Si el aporte real por hora cae al aumentar la jornada, el umbral cambia.

Para conservar además $120 por hora de jornada después de esos fijos, con aporte $150/h harían falta $8,000 ÷ ($150 − $120) = 266.67 horas/mes, o 61.54 h/semana. Es otro supuesto: ilustra por qué comprar un carro para pocas horas de Uber puede no ser conveniente aunque algunos viajes sí convengan. La app no cambia automáticamente las metas del conductor para ocultar este problema.

## Comparación de una oferta concreta

Oferta hipotética de $130; recogida 2 min/1 km; trayecto 25 min/10 km; espera del pasajero 3 min. Total 30 min/11 km. Para aislar las cuentas en esta prueba se desactivan filtros de pasajero y zonas; **no se recomienda desactivarlos en operación**.

- Gasolina: $22.92.
- Reserva de mantenimiento y llantas: $4.51.
- Reserva de desgaste al comparar un viaje financiado: $7.70.
- **Aporte económico antes de fijos: $94.87 por viaje, $189.75/h y $8.62/km.** La mensualidad no elimina el desgaste generado por esos 11 km.
- Saldo de efectivo antes de fijos, sin reservar depreciación adicional: $102.57. El saldo tras pagos descuenta la mensualidad prorrateada en su lugar, para evitar contarla además de la reserva al interpretar ese presupuesto como efectivo.

Con 10 h/semana, se asignan $92.31 de fijos a esos 30 min: saldo tras pagos **$10.27**. Comparar contra un mínimo de $120/h después de pagos da rojo; comparar el aporte del viaje contra mínimo $120/h y meta $180/h puede dar verde económico. Entretanto, el presupuesto mensual con aporte $150/h sigue rojo: **faltan $1,500**. El ritmo de una oferta no se convierte en rendimiento de toda la jornada.

Con 5 h/semana, se asignan $184.62 de fijos a esos 30 min: saldo tras pagos **−$82.04**, pero el mismo viaje sigue aportando $94.87 después de reservas por kilometraje. La app conserva la advertencia financiera y limita a **ámbar**, en lugar de convertirlo automáticamente en un viaje que genera una pérdida adicional. Un viaje cuyo aporte es negativo o incumple mínimos permanece rojo. Una regla de zona, pasajero, recogida o autonomía puede mantener cualquier oferta en rojo.

## Uso compartido

Si de verdad 50% de los km corresponde a Uber y todos esos fijos son compartidos, Uber recibe $4,000/mes. A 10 h/semana, su saldo asignado sería **+$2,500**. Los costos por cada km de Uber no se reducen otra vez.

**Los otros $4,000 no desaparecen.** Si Uber debe pagar todos los fijos del carro, todavía faltan $1,500 en el ejercicio. El plan muestra esa cuenta y se mantiene ámbar cuando cubre la parte de Uber, pero la parte restante necesita otra fuente de pago. No se presume la existencia de otro ingreso. Los gastos exclusivos de Uber siempre se suman completos, aunque el uso compartido sea 25%.

En perfiles anteriores los fijos podían estar ya asignados a Uber. Se conserva 100% inicialmente: si el importe ya es solo su parte, se mantiene 100% o se captura primero el gasto total compartido. No se reparte el mismo gasto dos veces.

## Decisión recomendada

Evaluar ofertas con aporte por viaje y límites personales, y evaluar la sostenibilidad con jornadas completas y gastos mensuales. Si la jornada real no cubre el presupuesto, revisar primero gastos comprobados, fuente de pago, horas factibles y costo del vehículo. Dejar de bajar umbrales o porcentajes solo para obtener verdes. Comprar o rentar un carro exclusivamente para Uber requiere que el plan completo, incluido el sueldo esperado, sea viable.

Fundamento: [ACCA, flujos incrementales](https://www.accaglobal.com/gb/en/student/exam-support-resources/foundation-level-study-resources/ffm/ffm-technical-articles/incremental.html) distingue costos que cambian por una decisión y compromisos que se pagarían de todas formas. Esto permite comparar un viaje; no demuestra viabilidad a largo plazo. [AAA, metodología 2025](https://newsroom.aaa.com/wp-content/uploads/2025/09/AAA-Brochure-Your-Driving-Cost-9.2025.pdf) separa operación por distancia y propiedad, con ajuste de depreciación por kilometraje; no se trasladan sus precios estadounidenses al perfil mexicano.
