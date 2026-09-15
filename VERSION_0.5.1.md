# Semáforo 0.5.1 · Uso compartido y plan mensual

Entrega local preparada el 13 de septiembre de 2026. No acredita publicación ni actualización de la descarga pública.

El cálculo anterior asignaba todos los gastos mensuales a las horas de Uber. Con pocas horas y una mensualidad podía marcar casi cualquier oferta roja, aunque el viaje aportara dinero después de costos por kilometraje. El onboarding pregunta ahora por uso exclusivo o compartido y asigna únicamente la parte correspondiente de fijos compartidos. Los fijos exclusivos de Uber se cargan completos; mantenimiento y llantas no se prorratean dos veces.

Los perfiles nuevos comparan el aporte del viaje antes de fijos y muestran aparte el saldo tras fijos y pagos. Una oferta que pierde dinero o incumple mínimos sigue roja. Un aporte positivo con saldo negativo tras fijos requiere revisión ámbar; las demás reglas pueden conservar rojo. Con carro financiado se conserva la reserva de desgaste para elegir viajes; el saldo para pagos usa la mensualidad en su lugar. Se distinguen reservas y efectivo, sin presentar la mensualidad entera como gasto contable.

Plan del mes usa una entrada explícita del conductor por hora de jornada, incluyendo espera entre ofertas. Permite cambiar uso, horas y base sin reemplazar el vehículo. Campo vacío significa desconocido, no cero. Muestra saldo mensual, faltante y horas para cubrir fijos sin sueldo. La parte compartida fuera de Uber y la obligación completa del carro permanecen visibles; no se supone otro ingreso. Una oferta no acredita ingresos futuros ni cambia el presupuesto mensual por sí sola.

Las actualizaciones conservan campos anteriores y su comparación después de gastos (`TOTAL`, 100%). Adoptar aporte se elige en Plan del mes o al aceptar un preset nuevo. Cambiar de vehículo invalida el supuesto de aporte mensual anterior. El catálogo pasa a v2, Android a 13 / 0.5.1 y diagnóstico a esquema 8, manteniendo el significado de los campos históricos de saldo después de fijos. El detalle técnico queda disponible aparte del resumen.

Consulta [ONBOARDING.md](ONBOARDING.md) y [EJERCICIO_POCAS_HORAS.md](EJERCICIO_POCAS_HORAS.md). Las cifras precargadas siguen siendo supuestos, no gastos verificados ni ingresos típicos.

El Aveo aparece en sedán, junto con Versa, Vento y K3. March y Spark son ejemplos del perfil compacto. Esto orienta la selección; no sustituye un perfil calibrado para cada modelo y año.

## Verificación

El [APK firmado 0.5.1](entrega/Detector-ofertas-0.5.1.apk) está preparado localmente, con [SHA-256](entrega/Detector-ofertas-0.5.1.sha256.txt). La [evidencia de entrega](verification/release-0.5.1.json) registra 162 pruebas unitarias aprobadas, lint con 0 errores y 21 advertencias, y 85 pruebas Android únicas con resultado final aprobado en un emulador propio Android 16.

La primera ejecución Android tuvo cuatro fallos en selectores del código de pruebas. Se corrigieron los selectores y se repitieron los 12 casos de onboarding y plan mensual sobre el APK final: 12/12 aprobados. Los otros 73 casos aprobaron en la ejecución inicial; el registro conserva ambos resultados, sin presentar una sola ejecución completa como limpia.

Se verificó una actualización real desde 0.5.0 al APK final: todos los campos anteriores y las reglas de zona coincidieron con la instantánea previa, incluyendo un perfil PHEV con carga a $0, autonomía restante de 74 km y lista de zonas voluntariamente vacía. El borrador de uso compartido conserva 10 horas semanales y 50% al girar la pantalla y volver a vertical. Se revisaron las pantallas finales de carro, uso y resultado mensual.

No se publicó esta versión ni se validó el nuevo flujo en teléfonos físicos de conductores. Quedan pendientes pruebas de comprensión con usuarios nuevos y calibración de los costos precargados contra gastos reales.
