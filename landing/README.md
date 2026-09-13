# Landing de Semáforo

Página estática sin dependencias ni proceso de compilación. El directorio raíz del proyecto en Vercel es `landing`; framework **Other**, salida `.`. Código HTML/CSS/JS y descargas públicos. No requiere claves de Stripe: el botón abre un Payment Link alojado por Stripe con importe libre en MXN y aportación única.

Dominio solicitado: https://semaforo.uber.tesivil.com/

Repositorio: https://github.com/galjoctavio-ux/semaforo

APK servida: `descargas/Detector-ofertas-0.4.0.apk`, exactamente la APK firmada y verificada de `entrega/`, SHA-256 `3825b17c18f956d2277546da81793111468534b29a5748c3c52c4d654ffb7ccd`. La landing no recompila Android. En cada actualización, copia el archivo final y su huella, y actualiza versión, enlaces y encabezado de descarga juntos.

## Vista local

Desde este directorio, sirve los archivos con cualquier servidor estático, por ejemplo `python -m http.server 4173 --bind 127.0.0.1`. La página usa rutas absolutas: abrir `index.html` directamente mediante `file://` no reproduce la publicación.

## Actualizaciones

Actualiza versión, tamaño, huella, enlaces y política de privacidad juntos cuando se entregue otra APK. La ilustración es un ejemplo, no una interfaz incrustada ni una oferta real. Las fotos originales y las credenciales de firma no se publican.

Referencias de configuración: [Vercel: conectar dominio](https://vercel.com/docs/domains/working-with-domains/add-a-domain), [Stripe: Payment Links](https://docs.stripe.com/payment-links/create).
