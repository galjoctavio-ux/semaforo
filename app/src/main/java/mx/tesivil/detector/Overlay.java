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
    private final WindowManager.LayoutParams params;
    private boolean attached;
    private int shownColor=Integer.MIN_VALUE;
    Overlay(Context context, Runnable stop) {
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        root = new LinearLayout(context); root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(Ui.dp(context, 14), Ui.dp(context, 4), Ui.dp(context, 4), Ui.dp(context, 4));
        root.setBackground(Ui.shape(Ui.signalColor(null), 16, context));
        root.setElevation(Ui.dp(context, 4));
        body = Ui.text(context, "Esperando…", 22, true); body.setTextColor(Color.WHITE);
        body.setGravity(Gravity.CENTER_VERTICAL);body.setIncludeFontPadding(false);body.setSingleLine(true);
        body.setAutoSizeTextTypeUniformWithConfiguration(14,22,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        root.addView(body,new LinearLayout.LayoutParams(0,Ui.dp(context,48),1));
        TextView close = Ui.text(context, "×", 26, false); close.setTextColor(Color.WHITE);
        close.setGravity(Gravity.CENTER);close.setIncludeFontPadding(false);
        close.setContentDescription("Detener lectura");close.setTooltipText("Detener lectura");
        close.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33ffffff),null,Ui.shape(Color.WHITE,12,context)));
        root.addView(close, new LinearLayout.LayoutParams(Ui.dp(context, 48), Ui.dp(context, 48)));
        close.setOnClickListener(v -> stop.run());
        params = new WindowManager.LayoutParams(Ui.dp(context, 216), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Ui.dp(context, 14); params.y = Ui.dp(context, 56);
        body.setOnTouchListener(new View.OnTouchListener() {
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
        String content=offer==null?"Esperando…":e==null?"Leyendo…":Ui.compact(e);
        int color=Ui.signalColor(offer==null?null:e);
        String spoken=offer==null||e==null?"Semáforo. "+status
                :e.color==ScoreEngine.Color.GRIS?"Semáforo sin datos. "+e.reason()
                :"Semáforo "+e.label()+". "+(e.tripBasis?"Aporte antes de fijos: ":"Saldo tras fijos: ")
                +Ui.amount(e.decisionHourly)+" por hora, estimado. "+e.reason();
        spoken+=". Arrastra la franja para moverla.";
        // The overlay is itself in a full-screen capture. Avoid triggering another
        // layout/frame when the displayed text and color have not changed.
        if(!content.contentEquals(body.getText()))body.setText(content);
        if(!spoken.contentEquals(body.getContentDescription()==null?"":body.getContentDescription()))body.setContentDescription(spoken);
        if(shownColor!=color){shownColor=color;root.setBackground(Ui.shape(color,16,root.getContext()));}
    }
    void show() { if (!attached) { windows.addView(root, params); attached = true; } }
    void hide() { if (attached) { windows.removeView(root); attached = false; } }
}
