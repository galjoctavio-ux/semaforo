# Publicación de Semáforo

Publicado el 12 de septiembre de 2026 como **beta pública gratuita**.

- Sitio: https://semaforo.uber.tesivil.com/
- Repositorio público: https://github.com/galjoctavio-ux/semaforo
- Release pública: https://github.com/galjoctavio-ux/semaforo/releases/tag/v0.3.4
- APK directa: https://semaforo.uber.tesivil.com/descargas/Detector-ofertas-0.3.4.apk
- Aportación voluntaria y única, importe elegido por el cliente en MXN: https://buy.stripe.com/28E14o0cKcddef975EfIs00

## Hosting y actualizaciones

Vercel: proyecto `semaforo`, equipo `tavo1992tavo16-6795s-projects`. Repositorio conectado, rama de producción `main`, directorio raíz `landing`, framework Other/sin compilación. Las actualizaciones se publican mediante push a `main` y se deben verificar en Vercel y en el dominio antes de anunciarse.

Squarespace: CNAME `semaforo.uber` → `52fc19b076683333.vercel-dns-017.com`, TTL inicial 30 minutos. El nombre resultante es `semaforo.uber.tesivil.com`. Se agregó solo ese registro.

## Comprobaciones

El dominio responde por HTTPS sin autenticación. Portada, privacidad, CSS, JavaScript, favicon y huella de descarga respondieron 200. Se descargó íntegro el APK desde el dominio público y coincidieron tamaño **50,421,532 bytes** y SHA-256 **0aabf83b2f80f5eb4ac3466b02ea57c67ec98631361f74292af316fa346492db** con la entrega firmada. Se comprobó el diseño en escritorio y anchos móviles de 390 y 320 píxeles; se corrigió el desbordamiento horizontal.

El checkout público de Stripe muestra TESIVIL, el título de Semáforo y un campo de importe libre en MXN. No se realizó una transacción ni se acredita todavía el resultado de un cobro real.

La landing distribuye la entrega Android firmada 0.3.4, con las mejoras de formatos descritas en `VERSION_0.3.4.md`. Las notas de versiones anteriores documentan el desarrollo y pruebas; sus APK públicas siguen en los releases de GitHub. Las claves de firma, capturas privadas y diagnósticos de sesiones se conservan fuera de Git.
