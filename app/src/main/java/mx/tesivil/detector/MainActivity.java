package mx.tesivil.detector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.mlkit.vision.text.TextRecognizer;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int CAPTURE = 10, IMAGE = 11, EXPORT = 12, NOTIFY = 13;
    private TextView status, result, stats, permission, monthly;
    private Button start, imageButton;
    private android.widget.Spinner captureMode;
    private final java.util.List<Button> setupButtons = new java.util.ArrayList<>();
    private String pendingExport;
    private TextRecognizer recognizer;
    private boolean importing;
    private volatile boolean destroyed;
    private volatile long importGeneration;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Runnable repaint = this::refresh;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        boolean firstSetup=state==null && ConfigStore.needsOnboarding(this);
        Diagnostics.init(this);
        recognizer = Ocr.create();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Ui.BG);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 22); page.setPadding(pad, pad, pad, pad);
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            page.setPadding(pad + bars.left, pad + bars.top, pad + bars.right, pad + bars.bottom);
            return insets;
        });
        page.addView(Ui.text(this, "TESIVIL  /  SEMÁFORO 0.5.5", 12, true));
        addText(page, "Evalúa tus viajes", 29, true, 14);
        addText(page, "Elige un perfil de carro y responde unas pocas preguntas para empezar. Compara lo disponible después de costos y revisa los motivos de cada oferta.", 16, false, 8);
        addSetup(page,"Configurar con un perfil",OnboardingActivity.class,"onboarding");

        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad); card.setBackground(Ui.shape(android.graphics.Color.WHITE, 18, this));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.topMargin = Ui.dp(this, 22); page.addView(card, cp);
        status = Ui.text(this, "Listo para probar", 17, true); status.setTextColor(Ui.TEAL); card.addView(status);
        result = addText(card, "Los datos aparecerán aquí.", 17, false, 12);
        stats = addText(card, "", 12, false, 12);
        Button details = button(card,"Ver cálculo y motivos",false);
        details.setOnClickListener(v -> {
            ScrollView detailScroll=new ScrollView(this);
            TextView text=Ui.text(this,Ui.details(Diagnostics.evaluation,Diagnostics.config),15,false);
            int detailPad=Ui.dp(this,20);text.setPadding(detailPad,detailPad,detailPad,detailPad);detailScroll.addView(text);
            new AlertDialog.Builder(this).setTitle("Detalle de la evaluación").setView(detailScroll).setPositiveButton("Entendido",null)
                    .setNeutralButton("Datos del cálculo",(d,w)->{
                        ScrollView technical=new ScrollView(this);
                        TextView all=Ui.text(this,Ui.technicalDetails(Diagnostics.evaluation,Diagnostics.config),15,false);
                        all.setPadding(detailPad,detailPad,detailPad,detailPad);technical.addView(all);
                        new AlertDialog.Builder(this).setTitle("Datos del cálculo").setView(technical).setPositiveButton("Entendido",null).show();
                    }).show();
        });
        monthly=addText(page,"",14,true,16);
        addSetup(page,"Plan del mes",MonthlyPlanActivity.class,"monthly");
        permission = addText(page, "", 13, false, 20);

        Button overlay = button(page, "1. Permitir ventana flotante", false);
        overlay.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) { alert("Permiso listo", "La ventana flotante ya está permitida."); return; }
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        });
        captureMode=FormUi.spinner(page,"Modo de lectura",new String[]{"Pantalla completa (minimizado)","Solo Uber (mantener visible)"},Diagnostics.config.captureWholeScreen?0:1);
        start = button(page, "2. Iniciar prueba con Uber", true);
        start.setOnClickListener(v -> begin());
        Button stop = button(page, "Detener lectura", false);
        stop.setOnClickListener(v -> stopService(new Intent(this, CaptureService.class)));
        imageButton = button(page, "Probar una captura guardada", false);
        imageButton.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick, IMAGE);
        });
        addText(page,"Ajustes avanzados",18,true,18);
        addSetup(page,"Vehículo y costos",SettingsActivity.class,"vehicle");
        addSetup(page,"Objetivos y score",SettingsActivity.class,"goals");
        addSetup(page,"Pasajero y filtros",SettingsActivity.class,"rider");
        addSetup(page,"Colonias y zonas",SettingsActivity.class,"zones");
        addSetup(page,"Simulador",ReviewActivity.class,"simulator");

        addSetup(page,"Resultados y precisión",ReviewActivity.class,"results");
        Button export = button(page, "Guardar diagnóstico", false);
        export.setOnClickListener(v -> {
            try {
                pendingExport = Diagnostics.export(this);
                startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json")
                        .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "diagnostico-lector-" + System.currentTimeMillis() + ".json"), EXPORT);
            } catch (Exception e) { alert("No se pudo guardar", "Inténtalo de nuevo."); }
        });
        addText(page, "Cómo probar", 18, true, 22);
        addText(page, "Estando estacionado, permite la ventana flotante e inicia la prueba. Pantalla completa lee también la oferta flotante de Uber sobre el inicio u otra app. Solo Uber requiere que Uber esté visible. Android pedirá tu autorización.\n\nPuedes minimizar este lector durante la sesión. Arrastra la franja lejos de la tarjeta y usa × para detener al terminar. Al bloquear el teléfono tendrás que iniciar otra sesión.", 15, false, 8);
        addText(page, "Solo lectura local", 18, true, 18);
        addText(page, "Sin Internet ni pulsaciones en Uber. En pantalla completa se procesan también las otras apps visibles mientras la lectura esté activa. Imágenes y direcciones se procesan en memoria. Se guardan hasta 200 versiones numéricas de ofertas, calificación y contador anónimos, perfil, reglas y resultados que tú registres. Puedes exportar y borrar el historial. Los costos reales vacíos permanecen desconocidos; una oferta observada nunca acredita un viaje completado.\n\nLista editable de colonias ZMG con antecedentes de Fiscalía / IIEG y fuentes municipales. Conserva periodos de referencia y separa tu fecha de revisión. Los niveles iniciales son de la app. Verde cumple tus parámetros sin garantizar ingresos o seguridad. Lee tarjetas compatibles en español de UberX, UberXL, Priority, Comfort y Black, Exclusivo, Reservar UberX, fondos claros u oscuros e importes $ o MXN. Convierte horas a minutos. Las paradas adicionales, el horario de reserva y la ruta intermedia requieren revisión. El regreso configurado es un escenario, no una predicción de demanda.", 14, false, 8);
        setContentView(scroll); refresh();
        if(firstSetup){ConfigStore.markOnboardingEntry(this);startActivity(new Intent(this,OnboardingActivity.class));}
    }

    private void begin() {
        if (Diagnostics.running || importing) return;
        if (!Settings.canDrawOverlays(this)) { alert("Falta un permiso", "Primero toca «Permitir ventana flotante» y activa el permiso para esta app."); return; }
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFY); return;
        }
        requestCapture();
    }
    private void requestCapture() {
        boolean wholeScreen=captureMode.getSelectedItemPosition()==0;
        new AlertDialog.Builder(this).setTitle("Compartir Uber para leer ofertas")
                .setMessage(wholeScreen?"Se compartirá la pantalla completa para leer las ofertas flotantes cuando Uber esté minimizado. También se procesarán las otras apps que abras mientras la sesión esté activa. El análisis es local; las imágenes no se guardan ni se envían. Usa Detener al terminar.":"Selecciona solo Uber Driver en el aviso de Android. La lectura se pausa cuando Uber deja de estar visible. El análisis es local; las imágenes no se guardan ni se envían.")
                .setNegativeButton("Ahora no", null).setPositiveButton("Continuar", (d, w) -> {
                    try {DriverConfig config=ConfigStore.load(this);config.captureWholeScreen=wholeScreen;ConfigStore.save(this,config);Diagnostics.reloadConfig();
                        MediaProjectionManager manager=getSystemService(MediaProjectionManager.class);
                        startActivityForResult(wholeScreen?manager.createScreenCaptureIntent(android.media.projection.MediaProjectionConfig.createConfigForDefaultDisplay()):manager.createScreenCaptureIntent(),CAPTURE);
                    } catch(RuntimeException e){alert("No se pudo iniciar","Revisa la configuración y vuelve a intentarlo.");}
                }).show();
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == NOTIFY) requestCapture(); // A denied notification permission does not grant or deny screen capture.
    }
    @Override protected void onActivityResult(int code, int resultCode, Intent data) {
        super.onActivityResult(code, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (code == CAPTURE) {
            startForegroundService(new Intent(this, CaptureService.class).putExtra("resultCode", resultCode).putExtra("consent", data));
            main.postDelayed(() -> {
                if (destroyed) return;
                Intent uber = getPackageManager().getLaunchIntentForPackage("com.ubercab.driver");
                if (uber != null) startActivity(uber);
            }, 400);
        } else if (code == IMAGE && data.getData() != null) {
            importImage(data.getData());
        } else if (code == EXPORT && data.getData() != null && pendingExport != null) {
            Uri destination = data.getData(); String contents = pendingExport; pendingExport = null;
            io.execute(() -> {
                try (OutputStream out = getContentResolver().openOutputStream(destination, "wt")) {
                    if (out == null) throw new java.io.IOException("No output");
                    out.write(contents.getBytes(StandardCharsets.UTF_8));
                    main.post(() -> { if (!destroyed) alert("Diagnóstico guardado", "El archivo incluye solo datos numéricos y del dispositivo."); });
                } catch (Exception e) { main.post(() -> { if (!destroyed) alert("No se pudo guardar", "Elige otra ubicación."); }); }
            });
        }
    }
    private void importImage(Uri uri) {
        if (Diagnostics.running || importing) return;
        importing = true; long generation = ++importGeneration;
        Diagnostics.source = "Captura seleccionada"; Diagnostics.clear("Leyendo imagen…"); refresh();
        io.execute(() -> {
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri), (decoder, info, src) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                    double scale = Math.min(1, 1920d / Math.max(w, h));
                    decoder.setTargetSize(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)));
                });
                if (destroyed || generation != importGeneration) { bitmap.recycle(); return; }
                long startAt = SystemClock.elapsedRealtime();
                Ocr.read(recognizer,bitmap)
                        .addOnSuccessListener(result -> {
                            if (!destroyed && generation == importGeneration)
                                Diagnostics.reading(result, SystemClock.elapsedRealtime() - startAt, "Captura seleccionada");
                        }).addOnFailureListener(error -> {
                            if (!destroyed && generation == importGeneration) Diagnostics.clear("No se pudo reconocer el texto");
                        }).addOnCompleteListener(task -> { bitmap.recycle(); importing = false; if (!destroyed) refresh(); });
            } catch (Exception e) {
                main.post(() -> { importing = false; if (!destroyed) Diagnostics.clear("No se pudo abrir la imagen"); });
            }
        });
    }

    private void refresh() {
        if (status == null || destroyed) return;
        status.setText(Diagnostics.message);
        status.setTextColor(Ui.signalColor(Diagnostics.evaluation));
        result.setText(Diagnostics.offer == null ? "Espera una oferta, abre una captura o usa el simulador." :
                (Diagnostics.evaluation == null ? Ui.summary(Diagnostics.offer) : Ui.evaluationSummary(Diagnostics.evaluation)+(Diagnostics.offerChange.isEmpty()?"":"\n"+Diagnostics.offerChange)+"\n\n"+Ui.summary(Diagnostics.offer)));
        stats.setText(Diagnostics.source + "\nImágenes leídas: " + Diagnostics.frames + " · completas: " + Diagnostics.validFrames
                + "\nOCR: " + Diagnostics.lastOcrMs + " ms · cálculo: "+Diagnostics.lastDecisionMs+" ms\nNo es latencia completa · "+Diagnostics.config.profileStatus()
                + (HistoryStore.storageError.isEmpty()?"":"\n"+HistoryStore.storageError));
        monthly.setText(Ui.monthlySummary(Diagnostics.config));
        monthly.setTextColor(Ui.color(MonthlyPlan.evaluate(Diagnostics.config).color));
        permission.setText("Ventana flotante: " + (Settings.canDrawOverlays(this) ? "permitida" : "pendiente")
                + "  ·  " + Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE);
        start.setEnabled(!Diagnostics.running && !importing); imageButton.setEnabled(!Diagnostics.running && !importing);
        captureMode.setEnabled(!Diagnostics.running && !importing);
        for(Button b:setupButtons)b.setEnabled(!Diagnostics.running && !importing);
        start.setText(Diagnostics.running ? "Lectura en curso" : "2. Iniciar prueba con Uber");
    }
    private TextView addText(LinearLayout parent, String text, float size, boolean bold, int margin) {
        TextView v = Ui.text(this, text, size, bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = Ui.dp(this, margin); parent.addView(v, p); return v;
    }
    private Button button(LinearLayout parent, String text, boolean primary) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); button.setTextSize(15);
        button.setTextColor(primary ? android.graphics.Color.WHITE : Ui.INK);
        button.setBackground(Ui.shape(primary ? Ui.TEAL : android.graphics.Color.WHITE, 12, this));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54)); p.topMargin = Ui.dp(this, 10);
        parent.addView(button, p); return button;
    }
    private void alert(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Entendido", null).show(); }
    private void addSetup(LinearLayout page,String label,Class<?> activity,String section){
        Button b=button(page,label,false);setupButtons.add(b);b.setOnClickListener(v->startActivity(new Intent(this,activity).putExtra("section",section)));
    }
    @Override protected void onResume() {
        super.onResume();long before=Diagnostics.config.revision;Diagnostics.reloadConfig();
        if(before!=Diagnostics.config.revision)Diagnostics.clear("Perfil actualizado; evalúa otra oferta");
        if (!Diagnostics.listeners.contains(repaint)) Diagnostics.listeners.add(repaint); refresh();
    }
    @Override protected void onPause() { Diagnostics.listeners.remove(repaint); super.onPause(); }
    @Override protected void onDestroy() { destroyed = true; importGeneration++; Diagnostics.listeners.remove(repaint); recognizer.close(); io.shutdown(); super.onDestroy(); }
}
