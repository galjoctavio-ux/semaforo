# Landing de Semáforo

Página estática sin dependencias ni proceso de compilación. El directorio raíz del proyecto en Vercel es `landing`; framework **Other**, salida `.`. Código HTML/CSS/JS y descargas públicos. No requiere claves de Stripe: el botón abre un Payment Link alojado por Stripe con importe libre en MXN y aportación única.

Dominio solicitado: https://semaforo.uber.tesivil.com/

Repositorio: https://github.com/galjoctavio-ux/semaforo

APK publicada: `Detector-ofertas-0.5.4.apk` en GitHub Releases, exactamente la APK firmada y verificada de `entrega/`, SHA-256 `cf5012511874cdb792b7e3624bdf4be0273cc48f3c281b76f012fa6a3420cc00`. Los botones apuntan directamente al artefacto de la versión para impedir que la landing quede ofreciendo una APK anterior. La landing no recompila Android. En cada actualización, publica el archivo final y su huella, y actualiza versión, enlaces y texto de descarga juntos.

## Vista local

Desde este directorio, sirve los archivos con cualquier servidor estático, por ejemplo `python -m http.server 4173 --bind 127.0.0.1`. La página usa rutas absolutas: abrir `index.html` directamente mediante `file://` no reproduce la publicación.

## Actualizaciones

Actualiza versión, tamaño, huella, enlaces y política de privacidad juntos cuando se entregue otra APK. La ilustración es un ejemplo, no una interfaz incrustada ni una oferta real. Las fotos originales y las credenciales de firma no se publican.

Referencias de configuración: [Vercel: conectar dominio](https://vercel.com/docs/domains/working-with-domains/add-a-domain), [Stripe: Payment Links](https://docs.stripe.com/payment-links/create).
