# Semáforo 0.3.0 · prueba privada

Instala `entrega/Detector-ofertas-0.3.0.apk` sobre 0.2.1 **sin desinstalar**. Conserva el mismo paquete y certificado, los costos, metas y reglas guardadas. Requiere Android 14 o posterior.

## Leer con las apps minimizadas

Selecciona **Pantalla completa (minimizado)** e inicia una nueva sesión. Android pedirá autorización. Este modo puede leer la tarjeta flotante de Uber sobre el inicio u otra aplicación. También procesa localmente las otras pantallas visibles durante la sesión; las imágenes no se guardan ni se envían en el APK entregado.

**Solo Uber (mantener visible)** sigue disponible, pero se pausa cuando Uber deja de estar visible; compartir una aplicación no garantiza capturar su ventana flotante sobre el inicio. [Documentación de Android](https://developer.android.com/media/grow/media-projection).

El servicio continúa al minimizar el lector o quitar su pantalla de recientes. Termina con **Detener** en la ventana o notificación, o cuando Android finaliza la captura, por ejemplo al bloquear el teléfono. No evade cierres forzados ni restricciones del fabricante, y no reinicia sesiones automáticamente.

La ventana se compactó y se colocó más arriba para reducir la posibilidad de tapar la tarjeta. Arrastra su título si la tapa. La fila de recogida permanece en el desglose principal; en la ventana se reemplazó por el pasajero.

El OCR en vivo limita el lado mayor a 1280 px. Requiere dos lecturas coincidentes, incluido el pasajero. Descarta resultados con más de 1.4 s desde adquirir el fotograma. La lectura publicada caduca 1.4 s después de su última publicación válida (revisión cada 0.3 s). Se corrigió el uso del instante anterior al OCR, que podía borrar la confirmación si el procesamiento tardaba más. Lecturas incompletas, cambios de superficie y fin de sesión invalidan el resultado. El contenido totalmente estático puede dejar de producir fotogramas; para una foto fija usa el importador.

## Pasajero: reglas iniciales editables

Abre **Pasajero y filtros**:

| Regla | Resultado inicial |
|---|---|
| Etiqueta explícita «Nuevo» | Rojo automático, aunque pague bien. Conserva el bloqueo si otros datos del encabezado son contradictorios; se muestra la ambigüedad. |
| Contador visible menor a 5 | Rojo por tu umbral de cuenta nueva. No es una definición oficial de Uber. |
| Calificación menor a 4.70 | Rojo por tu mínimo. |
| Calificación desde 4.70 pero menor a 4.85, o contador desde 5 pero menor a 20 | Precaución: descuento de 10 puntos y sin verde. |
| Calificación desde 4.85 y contador desde 20 | Permite verde si también cumplen costos, metas, zonas y otras reglas. |
| Datos ausentes/ambiguos sin etiqueta Nuevo | Sin verde; no se convierten en cero ni en cuenta nueva. Puede haber rojo por otra regla económica o de zona. |

Todos los umbrales se pueden guardar y cambiar. Se puede desactivar el bloqueo por Nuevo o todo el filtro. 5.00 estrellas e identidad verificada no anulan el bloqueo por contador bajo. El pasajero afecta el score y límites de aceptación, sin inventar costos monetarios.

Se etiqueta el número entre paréntesis como **contador visible**, con procedencia `ADJACENT_TO_RATING`. [Uber describe calificación y número de viajes en Perú](https://www.uber.com/pe/es/newsroom/cual-es-la-app-de-movilidad-mas-segura-en-peru/), pero esa publicación no confirma la semántica exacta de esta interfaz en Guadalajara. Falta comprobarla en México; no se presenta como total certificado de viajes.

No se identifica ni consulta la cuenta del pasajero. Ser nuevo no demuestra peligrosidad; estrellas o historial largo tampoco certifican seguridad. El score expresa conveniencia según preferencias, no probabilidad de delito.

El simulador añade calificación, contador y etiqueta Nuevo; campos vacíos representan datos ausentes. Historial y diagnóstico guardan cifras anónimas, procedencia, perfil y motivos, sin nombres/direcciones capturadas. Registros anteriores siguen legibles. Las 200 evaluaciones máximas son ofertas observadas, no viajes realizados o ganancias cobradas.

## Colonias y grupo

Instalación nueva: **90 reglas**, con 26 lugares nuevos sobre la base de 64. El complemento aporta 42 referencias a 42 lugares (16 ya existentes): comunicados de Zapopan y Tlajomulco/C4, programas municipales de Tonalá y Tlaquepaque, y un relato comunitario sobre Portillo López identificado como sin corroboración. [Listado, fuentes, periodos y criterios](FUENTES_COLONIAS.md).

Todos los lugares añadidos empiezan en **Precaución**, decidido por la app. Se conservan periodos separados; no se suman fuentes superpuestas. No se infieren probabilidades. Se excluyen cabeceras imprecisas y condiciones como pobreza/consumo de sustancias por sí solas. Se comparan extremos por colonia y municipio completos; no se evalúa la ruta intermedia.

La actualización conserva niveles, horarios, vigencias y costos propios. Las colonias originales eliminadas no reaparecen. Si se había vaciado toda la lista en 0.2.1, sigue vacía. Los lugares nuevos se añaden una vez cuando hay una lista; eliminarlos después no los recarga.

Se pudo consultar el grupo privado autorizado. La exploración inicial encontró temas de pago, espera, calles deterioradas, alquiler, bloqueos y formatos variados. [Hallazgos y propuestas priorizadas](HALLAZGOS_GRUPO_CHOFERES.md). No es una encuesta representativa ni una comprobación de delitos. No se cargan automáticamente todas las menciones ni se publican datos personales del grupo.

## Prueba siguiente

Verificación: **70 pruebas unitarias y 20 en Android aprobadas; lint sin errores** (15 avisos). En el emulador Android 16, la oferta flotante de $62.10 produjo una evaluación estable durante más de 30 segundos con el inicio al frente y ambas actividades minimizadas: se leyeron 4.92 y 149. [Captura de la prueba](verification/device/030-final-floating-3.png). Nuevo produjo rojo con tarifa sintética alta; desaparecer la oferta borró el resultado y Detener retiró la captura.

El APK firmado se actualizó sobre 0.2.1 sin desinstalar entre versiones: conservó gasolina a $28/L configurada en la versión anterior y mostró 90 reglas. El certificado coincide y el hash del APK instalado coincide con la entrega. [Registro de pruebas, firma y contenido](verification/release-0.3.0.json).

Las pruebas usan el emulador aislado del proyecto y una APK de pruebas separada. La oferta flotante es una superficie de prueba con tu captura, no una solicitud real de Uber. Las fotos y el servicio de prueba están ausentes del APK entregado. El caso Nuevo de integración es sintético; no se recibió una captura real de Nuevo. Falta verificar 0.3.0 en el vivo.

El siguiente paso es probar **estacionado en el vivo** una captura guardada, luego Uber visible y finalmente su tarjeta flotante sobre el inicio. Confirmar que se leen pasajero y trayecto y que la ventana no tapa la tarjeta. Una sesión finalizada debe iniciarse otra vez.

Antes de añadir mapas o más funciones, reunir 20–30 ofertas y medir extracción correcta, tiempo hasta el semáforo y casos fallidos. Formatos con horas, múltiples destinos y otras categorías siguen pendientes. Medir ingresos reales con una bitácora voluntaria antes de afirmar que la app los mejora.
