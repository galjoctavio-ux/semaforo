# Semáforo 0.2.1 — base histórica editable de la ZMG

La APK `entrega/Detector-ofertas-0.2.1.apk` agrega una lista inicial de **64 colonias** basada en datos oficiales de la Fiscalía del Estado de Jalisco publicados por el IIEG. Se instala sobre 0.2.0 con el mismo paquete y firma. Costos, objetivos y cálculo económico permanecen como en [0.2.0](VERSION_0.2.0.md).

## Fuente y selección

- [Página de incidencia delictiva del IIEG](https://iieg.gob.mx/ns/?page_id=22174).
- [ZIP de microdatos por colonia, octubre de 2021](https://iieg.gob.mx/ns/wp-content/uploads/2021/11/oct2021.zip), con CSV y descriptores de la Fiscalía. Descargado el 12 de septiembre de 2026.
- Se suman **robo a persona** y **robo a vehículos particulares**, solo enero–octubre de 2021. Se seleccionan hasta diez colonias por municipio con al menos cinco registros de ambos delitos sumados. Se excluyen datos sin colonia y cabeceras municipales imprecisas.
- Las cantidades y nombres proceden de la fuente. La selección y el nivel inicial **Medio · Precaución** son criterios de la app; la Fiscalía no publica en este archivo niveles de peligrosidad para conductores.

| Municipio | Colonias iniciales |
| --- | ---: |
| Guadalajara | 10 |
| Zapopan | 10 |
| San Pedro Tlaquepaque | 10 |
| Tonalá | 10 |
| Tlajomulco de Zúñiga | 10 |
| El Salto | 10 |
| Ixtlahuacán de los Membrillos | 2 |
| Juanacatlán | 1 |
| Zapotlanejo | 1 |

Consulta el [listado completo, cantidades y método reproducible](COLONIAS_ZMG.md). El archivo conserva nombres separados de secciones; no se amplía una sección de Loma Dorada a toda la colonia. Se expanden abreviaturas escritas como FRACC., HDA., JARDS., SECC. y OTE. para comparar texto, conservando también el nombre original.

Las cantidades absolutas no controlan población ni tránsito. Una colonia concurrida puede registrar más robos sin tener mayor probabilidad por visita. Este catálogo no mide homicidios, extorsión, desapariciones u otros delitos, ni certifica seguridad en las zonas ausentes. No es exhaustivo ni actual. La lista histórica se mantiene activa porque el usuario pidió usarla aun siendo antigua; la fecha no se cambia para simular actualidad.

## Uso

1. Abre **Colonias y zonas** en la parte inferior de la pantalla principal. La base se carga automáticamente una sola vez al abrir la nueva versión.
2. Busca por colonia o municipio. Toca una ficha para editarla y **Ver fuente y conteos** para revisar su antecedente oficial.
3. Cambia el nivel: **Alto · Evitar** obliga a rojo; **Medio · Precaución** resta la penalización configurada y permite como máximo ámbar; **Bajo · Revisada por mí** retira esa restricción según tu criterio. Los demás filtros y mínimos siguen aplicando.
4. Puedes elegir recogida, destino o ambos y horarios. **Eliminar** quita esa colonia; **Vaciar lista** quita todas después del aviso de la app. **Añadir colonia** crea una regla personal.
5. Las reglas iniciales quedan sin vencimiento automático. Puedes desmarcarlo y cambiar fecha y vigencia. Cambiar solo el nivel conserva la fuente; cambiar la identidad de colonia o municipio desvincula el antecedente oficial y crea una regla personal.

Se conserva el filtro activado/desactivado que ya tengas. Las reglas personales existentes tienen prioridad frente a una precarga de la misma colonia y municipio; se evitan duplicados. Una eliminación o cambio se conserva al reiniciar y no se sobrescribe con el valor de fábrica. El máximo aumenta a 300 reglas.

El filtro compara el texto que pudo leer de la recogida y destino. Si no puede identificar colonia y municipio con suficiente precisión, queda sin verificar; no busca coordenadas ni evalúa el trayecto intermedio. Con el filtro activo, una zona pendiente impide el verde. La autonomía y los demás costos siguen siendo configurables manualmente.

## Datos y comprobación

La APK contiene únicamente el catálogo agregado, funciona sin conexión y no incorpora las fotos del usuario ni el CSV completo. El historial y la exportación guardan el nivel aplicado y su referencia histórica para entender el cálculo posterior.

Las pruebas nuevas verifican carga inicial, cantidades de la fuente, compatibilidad con configuraciones anteriores, prioridad de ajustes personales, borrado permanente, niveles que afectan al semáforo, conservación de procedencia y edición desde la interfaz. Se comprobaron por separado todos los conteos del catálogo contra el CSV original, sin filas duplicadas exactas en el corte 2021. Evidencias de ejecución y firma se conservan en `verification/`.

Resultado de esta entrega: **48 pruebas unitarias y 14 instrumentadas aprobadas** en Android 16 emulado sin conexión. Compilación release aprobada, lint con cero errores y 17 advertencias. Actualización desde 0.2.0 aprobada conservando el permiso de superposición; una actualización con una regla personal previa mostró 65 reglas (64 nuevas y la personal). Se revisaron visualmente la lista, búsqueda y consulta de fuente en la APK release. La copia instalada coincide byte por byte con la entregada y conserva la firma anterior. Estos resultados no sustituyen la prueba en el vivo.

Siguiente comprobación en el vivo: actualizar sin desinstalar, revisar algunas colonias conocidas en la lista y probar una captura o simulación con colonia y municipio legibles. La nueva lista aporta antecedentes; medir la calidad de identificación en las ofertas reales sigue siendo necesario.
