# Semáforo de TESIVIL · Beta pública Android 0.3.5

App Android disponible al público, con descarga gratuita y código consultable en este repositorio. Requiere Android 14 o posterior. Lee tarjetas compatibles en español de UberX, UberXL, Priority y Comfort, con Exclusivo/Aceptar, en fondos claros u oscuros, y «Reservar UberX» con advertencia de horario y espera previa. Evalúa costos, metas, zonas y pasajero. La aceptación/rechazo permanece a cargo del conductor. Es un proyecto independiente, sin afiliación ni respaldo de Uber.

**Página:** https://semaforo.uber.tesivil.com/ · **Código:** https://github.com/galjoctavio-ux/semaforo · **Descarga alternativa:** [Release v0.3.5](https://github.com/galjoctavio-ux/semaforo/releases/tag/v0.3.5).

El score compara ofertas con tus ajustes; no es una probabilidad de delito ni garantiza ingresos. Los costos iniciales son ejemplos. El catálogo histórico no cubre todas las colonias ni evalúa la ruta completa. La landing está en [landing/](landing/README.md). Las donaciones son voluntarias y únicas mediante un enlace de Stripe; no condicionan la descarga.

[Publicación y mantenimiento](PUBLICACION.md).

**APK:** [Detector-ofertas-0.3.5.apk](entrega/Detector-ofertas-0.3.5.apk). Instala sobre la anterior sin desinstalar. [Reservar UberX](VERSION_0.3.5.md). [Fondos oscuros, Comfort y horas](VERSION_0.3.4.md). [Lectura durante el efecto azul](VERSION_0.3.3.md). [UberXL y Exclusivo](VERSION_0.3.2.md). [Correcciones de importes, Exclusivo y Priority](VERSION_0.3.1.md). [Defaults y reglas](VERSION_0.3.0.md).

0.3.0 leyó $77.06 como $7706 en una captura real y permitió un cálculo incorrecto de miles de pesos. 0.3.1 exige centavos legibles, reintenta campos numéricos ampliados y descarta incoherencias; si no puede validar, deja gris. Registros anteriores se conservan sin revalidación automática. Actualiza antes de continuar las pruebas.

## Primera prueba

1. Revisa Vehículo y costos, Objetivos y score, Pasajero y filtros y Colonias y zonas. Los defaults son ejemplos editables, no especificaciones ni precios actuales. El perfil PHEV inicia con 100 km eléctricos y electricidad a $0/kWh según lo informado por el usuario; revisa el resto con tus registros.
2. Estacionado, permite la ventana flotante. En este vivo el usuario reportó que funcionó después de autorizar los ajustes restringidos y permisos específicos de esta app. Si aparece ese aviso, sigue sus indicaciones específicas y regresa al permiso de superposición.
3. Prueba una captura guardada para comprobar importe, recogida, trayecto, calificación y contador.
4. Selecciona **Pantalla completa (minimizado)**, inicia la lectura y autoriza una nueva captura en Android. Lee también ofertas flotantes sobre el inicio u otra app. También procesa localmente las otras pantallas visibles mientras está activa.
5. Minimiza el lector y abre Uber. Arrastra el título para evitar tapar la tarjeta. **Solo Uber (mantener visible)** es una alternativa que se pausa cuando Uber deja de ser visible.
6. Termina con Detener en la ventana o notificación. Al bloquear el teléfono o terminar Android la captura, inicia otra sesión. No se reinician automáticamente.

## Incluye

- Margen estimado por hora y km, con energía, mantenimiento, llantas, desgaste, fijos asignados y extras. [Fórmulas y costos](VERSION_0.2.0.md).
- Nuevo explícito o contador menor a 5 genera rojo por defecto; calificación menor a 4.70 también. Para permitir verde se requieren 4.85 y 20 visibles, además de los otros criterios. Umbrales editables y filtros desactivables. Un dato ausente no es cero.
- Instalación nueva: 90 reglas ZMG con antecedentes oficiales históricos y un reporte comunitario separado. [Base original](COLONIAS_ZMG.md) y [fuentes adicionales](FUENTES_COLONIAS.md). Se puede editar nivel, municipio, rol, horario y vigencia, eliminar y añadir. Sin datos no se asume seguridad.
- Simulador, desglose y hasta 200 evaluaciones locales. Son ofertas observadas, no viajes realizados o ingresos cobrados.
- Duraciones como «1 h 3 min» se convierten a 63 minutos. La tarifa visible por km admite $ o MXN y se contrasta con el importe principal; no se suma otra vez el recargo descrito en Priority. El aviso «1 destino» se conserva separado de las direcciones y limita la recomendación a ámbar o rojo, con paradas y esperas pendientes de revisar.
- «Reservar UberX» se identifica como reserva. Admite recogidas como «A 24 min y (14.4 km)» y ofertas sin tarifa visible por km. Conserva el importe principal, avisa que falta revisar horario y espera previa y no recomienda verde; mantiene cualquier rojo de las otras reglas. No inventa minutos de espera ni añade otra tarifa de reserva.
- Diagnóstico JSON con perfil, cifras anónimas del pasajero y motivos. Excluye capturas, OCR completo y direcciones capturadas. Tus reglas y motivos escritos sí están en el archivo que exportas voluntariamente.

El número junto a la calificación se etiqueta como **contador visible**: falta confirmar su significado exacto en esta interfaz de México. No se consulta ni identifica la cuenta del pasajero. El score no es una probabilidad de delito y las estrellas/historial largo no certifican seguridad.

## Si falla

Comprueba que la sesión siga activa y el modo corresponda a la superficie donde aparece Uber. Evita tapar encabezado o datos con la ventana. Prueba una captura de esa tarjeta para distinguir captura de OCR/formato; guarda diagnóstico si falla. Si Android terminó la sesión, inicia otra.

Se requieren dos lecturas coincidentes. Se descartan resultados con más de 1.4 s desde adquisición; la lectura caduca 1.4 s después de su publicación válida (revisión cada 0.3 s). Android puede dejar de producir fotogramas en contenido estático; para fotos fijas usa el importador.

Las tarjetas recortadas sin botón no se califican. Múltiples destinos, paradas adicionales y categorías distintas de UberX/UberXL/Priority/Comfort siguen pendientes. «1 destino» no acredita una ruta completamente evaluada. La app no lee ofertas ausentes/ocultas/protegidas ni evade cierres forzados/restricciones del fabricante. Importar una foto originada en iOS no convierte esta APK en una app para iPhone. Las pruebas reales en distintos Android siguen pendientes; la verificación local está en verification/release-0.3.5.json.

## Privacidad y compilación

OCR incluido y local. Sin Internet, cámara, micrófono, ubicación, lectura de notificaciones ajenas o accesibilidad. No pulsa Uber ni envía datos a GPT. Usa MediaProjection autorizada, ventana y servicio en primer plano con Detener. Fotos y superficies sintéticas existen solo en una APK de pruebas separada.

Java 17, AGP 8.11.0, Gradle 8.14.3, SDK 36 y ML Kit 16.0.1:

```powershell
.\gradlew.bat --no-daemon --max-workers=2 :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest
```

SDK en local.properties. La firma local de pruebas está en .signing/, fuera de control de versiones; conservarla para actualizar y no distribuirla. Los catálogos se reproducen con scripts/build_zone_catalog.py y scripts/build_zone_supplement.py.

Para compilar desde una copia nueva, configura el SDK con `ANDROID_HOME` o `local.properties`. Ejecuta `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (en Windows, `gradlew.bat`). La release distribuida usa una clave de firma custodiada fuera del repositorio; para tu propia release configura una firma propia. No publiques claves, contraseñas ni archivos de permisos locales.

Las clases de pruebas Android se incluyen, pero las fotos privadas usadas en ciertas regresiones no se redistribuyen. Para ejecutar esos casos aporta tus propias muestras a las rutas indicadas en las clases. Las 108 pruebas unitarias no dependen de esas fotos. La verificación de 57 pruebas Android de la release se hizo localmente con esas muestras; no se afirma que se ejecute completa sin ellas. `-PtestRelease` compila la APK de pruebas con el tipo release para verificar la aplicación firmada. Excluye `ReleaseEvidenceTest` de la suite normal con `-e notClass mx.tesivil.detector.ReleaseEvidenceTest`; sus métodos preparan y comprueban una actualización y se seleccionan individualmente, solo en un emulador propio.

El código distribuible, las fórmulas y los catálogos son públicos. Las capturas personales, diagnósticos de sesiones, notas de lectura de grupos privados y claves de firma no forman parte de la distribución.
