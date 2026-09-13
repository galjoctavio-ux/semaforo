package mx.tesivil.detector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.WindowManager;
import com.google.mlkit.vision.text.TextRecognizer;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CaptureService extends Service {
    public static final String STOP = "mx.tesivil.detector.STOP";
    private static final String CHANNEL = "lectura_activa";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ReadingWindow gate = new ReadingWindow();
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private HandlerThread captureThread;
    private Handler worker;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private TextRecognizer recognizer;
    private Overlay overlay;
    private volatile boolean ended, visible = true;
    private long lastPublishedAt;
    private int width, height, density;
    private final Runnable updateOverlay = () -> {
        if (overlay != null) overlay.update(Diagnostics.message, Diagnostics.offer, Diagnostics.lastOcrMs);
    };
    // Poll the latest available buffer: throttling an arrival listener can discard
    // the final frame of a short-lived offer or of the offer disappearing.
    private final Runnable capturePoll = new Runnable() {
        @Override public void run() {
            if (ended) return;
            if (reader != null && !processing.get()) readFrame(reader);
            if (!ended) worker.postDelayed(this, 420);
        }
    };
    private final Runnable expire = new Runnable() {
        @Override public void run() {
            if (ended) return;
            if (Diagnostics.offer != null && SystemClock.elapsedRealtime() - lastPublishedAt > ReadingWindow.MAX_FRAME_AGE_MS)
                Diagnostics.clear("Sin lectura reciente · esperando oferta");
            main.postDelayed(this, 300);
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) { finish("Lectura detenida"); return START_NOT_STICKY; }
        if (projection != null) return START_NOT_STICKY;
        Intent consent = intent.getParcelableExtra("consent", Intent.class);
        int resultCode = intent.getIntExtra("resultCode", 0);
        if (consent == null || resultCode != android.app.Activity.RESULT_OK) {
            finish("No se autorizó la captura"); return START_NOT_STICKY;
        }
        try {
            startForeground(1, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            Diagnostics.init(this);
            Diagnostics.begin();
            recognizer = Ocr.create();
            captureThread = new HandlerThread("LocalScreenReader"); captureThread.start();
            worker = new Handler(captureThread.getLooper());
            android.view.Display primaryDisplay = getSystemService(DisplayManager.class)
                    .getDisplay(android.view.Display.DEFAULT_DISPLAY);
            Context windowContext = createWindowContext(primaryDisplay,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null);
            WindowManager windowManager = windowContext.getSystemService(WindowManager.class);
            Rect bounds = windowManager.getMaximumWindowMetrics().getBounds();
            density = getResources().getConfiguration().densityDpi;
            if (Settings.canDrawOverlays(this)) {
                overlay = new Overlay(windowContext, () -> finish("Lectura detenida"));
                overlay.show(); Diagnostics.listeners.add(updateOverlay);
            }
            projection = getSystemService(MediaProjectionManager.class).getMediaProjection(resultCode, consent);
            gate.start();
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { finish("Captura finalizada. Inicia otra sesión para continuar."); }
                @Override public void onCapturedContentResize(int w, int h) {
                    if (!ended && worker != null) worker.post(() -> configureDisplay(w, h));
                }
                @Override public void onCapturedContentVisibilityChanged(boolean isVisible) {
                    visible = isVisible;
                    if (ended) return;
                    if (!visible) {
                        gate.invalidate(); Diagnostics.clear("La aplicación compartida no está visible");
                        if (overlay != null) overlay.hide();
                    } else {
                        gate.start(); Diagnostics.clear("Esperando oferta UberX, UberXL o Priority…");
                        if (overlay != null) {
                            try { overlay.show(); } catch (RuntimeException e) { finish("No se pudo mostrar la ventana flotante"); }
                        }
                    }
                }
            }, main);
            worker.post(() -> configureDisplay(bounds.width(), bounds.height()));
            worker.post(capturePoll);
            Diagnostics.status("Esperando oferta UberX, UberXL o Priority…"); main.post(expire);
        } catch (RuntimeException e) {
            android.util.Log.e("DetectorCapture", "Capture startup failed", e);
            finish("No se pudo iniciar. Revisa los permisos y vuelve a intentarlo.");
        }
        return START_NOT_STICKY;
    }

    private void configureDisplay(int sourceW, int sourceH) {
        if (ended || projection == null || sourceW <= 0 || sourceH <= 0) return;
        // Bound live OCR work to the resolution of the successfully read phone samples.
        double factor = Math.min(1, 1280d / Math.max(sourceW, sourceH));
        int w = Math.max(2, (int) (sourceW * factor)), h = Math.max(2, (int) (sourceH * factor));
        if (w == width && h == height && reader != null) return;
        gate.start(); width = w; height = h;
        try {
            if (reader != null) { reader.setOnImageAvailableListener(null, null); reader.close(); }
            reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 3);
            if (display == null) {
                display = projection.createVirtualDisplay("Detector local", w, h, density,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, worker);
            } else {
                display.resize(w, h, density); display.setSurface(reader.getSurface());
            }
            main.post(() -> { if (!ended) Diagnostics.clear("Esperando oferta UberX, UberXL o Priority…"); });
        } catch (RuntimeException e) { main.post(() -> finish("La captura se interrumpió. Inicia otra sesión.")); }
    }

    private void readFrame(ImageReader source) {
        if (ended) return;
        Image image = null;
        Bitmap padded = null, cropped = null;
        boolean claimed = false;
        try {
            image = source.acquireLatestImage();
            if (image == null) return;
            long now = SystemClock.elapsedRealtime();
            if (!visible || !processing.compareAndSet(false, true)) return;
            claimed = true;
            long token = gate.generation();
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int strideWidth = plane.getRowStride() / plane.getPixelStride();
            padded = Bitmap.createBitmap(strideWidth, image.getHeight(), Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(buffer);
            // The sample's offer occupies the lower screen; retain extra space for layout changes.
            int top = (int) (image.getHeight() * 0.25);
            cropped = Bitmap.createBitmap(padded, 0, top, image.getWidth(), image.getHeight() - top);
            if (padded != cropped) padded.recycle();
            padded = null;
            image.close(); image = null;
            Bitmap taskBitmap = cropped;
            long ocrAt = SystemClock.elapsedRealtime();
            var recognition = Ocr.read(recognizer,taskBitmap);
            cropped = null; // Transfer bitmap ownership only after the recognizer accepts it.
            recognition
                    .addOnSuccessListener(result -> {
                        long completedAt = SystemClock.elapsedRealtime();
                        if (!ended && visible && gate.accepts(token, now, completedAt)) {
                            // Frame age has already passed gate.accepts. Expire the displayed
                            // reading from publication, so slower OCR does not erase confirmation.
                            Diagnostics.reading(result, completedAt - ocrAt, "Captura en vivo");
                            lastPublishedAt = SystemClock.elapsedRealtime();
                        }
                    })
                    .addOnFailureListener(error -> {
                        if (!ended && gate.accepts(token, now, SystemClock.elapsedRealtime()))
                            Diagnostics.clear("No se pudo leer esta imagen. Reintentando…");
                    })
                    .addOnCompleteListener(task -> {
                        taskBitmap.recycle();
                        if (ended || worker == null) { processing.set(false); return; }
                        worker.post(() -> {
                            processing.set(false);
                            worker.removeCallbacks(capturePoll);
                            // Keep at least 420 ms between starts. Slow OCR must not add
                            // another polling interval before acquiring the latest frame.
                            if (!ended) worker.postDelayed(capturePoll,
                                    Math.max(0, 420 - (SystemClock.elapsedRealtime() - now)));
                        });
                    });
            claimed = false; // The task owns the processing flag until completion.
        } catch (RuntimeException e) {
            main.post(() -> { if (!ended) Diagnostics.clear("Imagen no disponible. Esperando la siguiente…"); });
        } finally {
            if (image != null) image.close();
            if (cropped != null && !cropped.isRecycled()) cropped.recycle();
            if (padded != null && !padded.isRecycled()) padded.recycle();
            if (claimed) processing.set(false);
        }
    }

    private Notification notification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Lectura en curso", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, CaptureService.class).setAction(STOP), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_detector)
                .setContentTitle("Detector de ofertas activo").setContentText("Lectura local. Toca Detener al terminar.")
                .setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Detener", stop).build()).build();
    }
    private void finish(String message) {
        if (ended) return;
        ended = true; gate.invalidate();
        Diagnostics.running = false; Diagnostics.clear(message);
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }
    @Override public void onDestroy() {
        boolean explicitFinish = ended;
        ended = true; gate.invalidate(); main.removeCallbacks(expire);
        Diagnostics.running = false; Diagnostics.offer = null;
        Diagnostics.evaluation = null;
        if (!explicitFinish) Diagnostics.message = "Lectura detenida";
        Diagnostics.listeners.remove(updateOverlay);
        if (overlay != null) { try { overlay.hide(); } catch (RuntimeException ignored) { } }
        if (projection != null) { try { projection.stop(); } catch (RuntimeException ignored) { } }
        if (worker != null) worker.post(() -> {
            worker.removeCallbacks(capturePoll);
            if (display != null) { display.release(); display = null; }
            if (reader != null) { reader.close(); reader = null; }
            if (captureThread != null) captureThread.quitSafely();
        });
        if (recognizer != null) recognizer.close();
        Diagnostics.notifyUi(); super.onDestroy();
    }
    @Override public void onTaskRemoved(Intent rootIntent) {
        // Keep the authorized foreground session when its settings activity is dismissed.
        // Stop remains available in the overlay and notification; Android can also end projection.
        super.onTaskRemoved(rootIntent);
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
