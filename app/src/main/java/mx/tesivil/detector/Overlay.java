package mx.tesivil.detector;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Overlay {
    private final WindowManager windows;
    private final LinearLayout root;
    private final TextView body;
    private final TextView title;
    private final WindowManager.LayoutParams params;
    private boolean attached;
    Overlay(Context context, Runnable stop) {
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        root = new LinearLayout(context); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(context, 12), Ui.dp(context, 8), Ui.dp(context, 12), Ui.dp(context, 10));
        root.setBackground(Ui.shape(Color.rgb(24, 42, 52), 14, context));
        root.setElevation(Ui.dp(context, 6));
        LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
        title = Ui.text(context, "SEMÁFORO · 0.3.3", 11, true); title.setTextColor(Color.rgb(122, 221, 238));
        header.addView(title, new LinearLayout.LayoutParams(0, Ui.dp(context, 32), 1));
        TextView close = Ui.text(context, "Detener", 12, true); close.setTextColor(Color.WHITE);
        close.setPadding(Ui.dp(context, 8), 0, 0, 0); close.setGravity(Gravity.CENTER);
        header.addView(close, new LinearLayout.LayoutParams(Ui.dp(context, 66), Ui.dp(context, 40)));
        close.setOnClickListener(v -> stop.run()); root.addView(header);
        body = Ui.text(context, "Esperando oferta…", 13, false); body.setTextColor(Color.WHITE);
        root.addView(body);
        params = new WindowManager.LayoutParams(Ui.dp(context, 262), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Ui.dp(context, 14); params.y = Ui.dp(context, 56);
        title.setOnTouchListener(new View.OnTouchListener() {
            float startX, startY; int oldX, oldY;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = event.getRawX(); startY = event.getRawY(); oldX = params.x; oldY = params.y; return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    android.graphics.Rect bounds = windows.getCurrentWindowMetrics().getBounds();
                    params.x = Math.max(0, Math.min(bounds.width() - params.width, oldX + Math.round(event.getRawX() - startX)));
                    params.y = Math.max(0, Math.min(bounds.height() - root.getHeight(), oldY + Math.round(event.getRawY() - startY)));
                    if (attached) windows.updateViewLayout(root, params); return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) { v.performClick(); return true; }
                return false;
            }
        });
    }
    void update(String status, OfferParser.Offer offer, long ms) {
        ScoreEngine.Evaluation e=Diagnostics.evaluation;
        title.setText(offer==null?"SEMÁFORO · 0.3.3":"TIPO: "+offer.typeLabel().toUpperCase(java.util.Locale.ROOT));
        if(offer==null||e==null){body.setText(status);root.setBackground(Ui.shape(Ui.signalColor(null),14,root.getContext()));return;}
        body.setText(e.label()+" · "+e.score+"/100\n"+Ui.evaluationSummary(e)
                +"\n"+offer.rider.summary()
                +"\n"+(Diagnostics.config.zoneFilter?(e.zones!=null&&e.zones.unknown?"Zonas: sin verificar":"Zonas: reglas aplicadas"):"Zonas: filtro desactivado"));
        root.setBackground(Ui.shape(Ui.signalColor(e),14,root.getContext()));
    }
    void show() { if (!attached) { windows.addView(root, params); attached = true; } }
    void hide() { if (attached) { windows.removeView(root); attached = false; } }
}
