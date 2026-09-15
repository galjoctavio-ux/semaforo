# Semáforo 0.5.0 · Onboarding con perfiles

Entrega preparada localmente el 13 de septiembre de 2026. No acredita publicación en GitHub, Vercel ni actualización de la descarga pública.

Se añade un asistente de cinco pasos con seis perfiles por tipo de carro, tres niveles de exigencia y cálculo de renta o mensualidad. PHEV y eléctricos añaden carga y autonomía restante. La opción explícita de carga gratuita permite el caso de paneles; la autonomía debe actualizarse conforme se conduce.

El perfil se guarda solo al empezar desde el resumen. Los costos precargados quedan etiquetados como estimaciones; aceptarlos no los marca como medidos o revisados con comprobantes. La app conserva los ajustes de usuarios anteriores al actualizar y mantiene sus filtros y escenarios al crear un perfil nuevo. Los detalles manuales están disponibles en Ajustes avanzados.

Consulta [ONBOARDING.md](ONBOARDING.md) para recorrido, supuestos numéricos, tratamiento de pagos, estados y límites. La versión Android es 12 / 0.5.0; el diagnóstico pasa a esquema 7.

## Verificación local

- 146 pruebas unitarias y 79 pruebas Android pasan; lint sin errores (19 advertencias).
- Actualización firmada desde 0.4.1: todos los campos previos del perfil y las reglas de zonas coinciden con el registro anterior, incluida una lista de zonas vacía por eliminación voluntaria.
- El borrador PHEV conserva página, carga a $0 y autonomía restante después de recrear la actividad por rotación.
- Pantallas de carro, carga, metas y resumen revisadas a 896 × 1920 / 420 dpi en un emulador Android 16 propio. No se afirma una prueba del nuevo flujo en el teléfono físico del conductor.

[APK local](entrega/Detector-ofertas-0.5.0.apk) · [SHA-256](entrega/Detector-ofertas-0.5.0.sha256.txt) · [Evidencia](verification/release-0.5.0.json). La prueba que necesita una proyección autorizada y una oferta auxiliar se excluye de la suite automática y queda documentada como preparación manual separada.

Pendiente: comprobar que un conductor nuevo completa el flujo sin ayuda y calibrar los supuestos con gastos reales. Los perfiles completos no equivalen a costos medidos.
