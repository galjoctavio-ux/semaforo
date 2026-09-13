package mx.tesivil.detector.fixture;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import java.io.InputStream;

/** Displays the supplied offer as a separate app for MediaProjection integration checks. */
public final class SampleOfferActivity extends Activity {
    private ImageView image;
    private Bitmap sample;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View progress;
    private int step;
    private final Runnable animate = new Runnable() {
        @Override public void run() {
            // A moving marker generates frames like the countdown of a live offer.
            progress.setTranslationX((step++ % 20) * 12);
            handler.postDelayed(this, 300);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        image = new ImageView(this);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setBackgroundColor(android.graphics.Color.WHITE);
        try (InputStream input = getAssets().open("user-offer.jpg")) {
            sample = BitmapFactory.decodeStream(input);
        } catch (Exception e) { throw new IllegalStateException("Test image missing", e); }
        FrameLayout surface = new FrameLayout(this);
        surface.addView(image, new FrameLayout.LayoutParams(-1, -1));
        progress = new View(this); progress.setBackgroundColor(android.graphics.Color.DKGRAY);
        surface.addView(progress, new FrameLayout.LayoutParams(30, 8));
        setContentView(surface);
        getWindow().getInsetsController().hide(WindowInsets.Type.systemBars());
        show(getIntent());
        handler.post(animate);
    }
    @Override public void onNewIntent(Intent intent) { super.onNewIntent(intent); show(intent); }
    private void show(Intent intent) {
        if(intent.getBooleanExtra("floating",false)) {
            image.setImageBitmap(null);
            startService(new Intent(this,FloatingOfferService.class).putExtra("newRider",intent.getBooleanExtra("newRider",false)).putExtra("asset",intent.getStringExtra("asset")).putExtra("cardTop",intent.getDoubleExtra("cardTop",.438)));
        } else image.setImageBitmap(intent.getBooleanExtra("blank", false) ? null : sample);
    }
    @Override public void onDestroy() { handler.removeCallbacks(animate); if (sample != null) sample.recycle(); super.onDestroy(); }
}
