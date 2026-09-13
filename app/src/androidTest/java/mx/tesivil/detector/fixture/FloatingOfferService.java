package mx.tesivil.detector.fixture;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Emulator fixture for an offer floating above the launcher with both activities minimized. */
public final class FloatingOfferService extends Service {
    private WindowManager wm;
    private View root,marker;
    private Bitmap bitmap;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int tick;
    private final Runnable animate=new Runnable(){public void run(){if(marker!=null){marker.setTranslationX((tick++%20)*12);handler.postDelayed(this,300);}}};
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(root!=null){wm.removeViewImmediate(root);root=null;}bitmap=null;
        android.view.Display display=getSystemService(android.hardware.display.DisplayManager.class).getDisplay(android.view.Display.DEFAULT_DISPLAY);
        android.content.Context windowContext=createWindowContext(display,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,null);
        wm=windowContext.getSystemService(WindowManager.class);FrameLayout frame=new FrameLayout(windowContext);frame.setBackgroundColor(Color.WHITE);
        int width=wm.getMaximumWindowMetrics().getBounds().width()-32;
        if(intent.getBooleanExtra("newRider",false)) {
            TextView text=new TextView(this);text.setTextColor(Color.BLACK);text.setTextSize(24);text.setPadding(32,24,32,24);
            text.setText("UberX\n$500.00\nNuevo\nA 1 min (1.0 km)\nColonia Ejemplo, Zapopan\nViaje: 10 min (4.0 km)\nOtra colonia, Zapopan\nViaje disponible");frame.addView(text,new FrameLayout.LayoutParams(-1,-2));
        } else {
            try(java.io.InputStream in=getAssets().open(intent.getStringExtra("asset")==null?"user-offer-home.jpg":intent.getStringExtra("asset"))) {
                Bitmap full=BitmapFactory.decodeStream(in);int top=(int)(full.getHeight()*(intent.hasExtra("cardTop")?intent.getDoubleExtra("cardTop",.438):.438));
                bitmap=Bitmap.createBitmap(full,0,top,full.getWidth(),full.getHeight()-top);if(bitmap!=full)full.recycle();
                ImageView image=new ImageView(this);image.setImageBitmap(bitmap);image.setScaleType(ImageView.ScaleType.FIT_CENTER);frame.addView(image,new FrameLayout.LayoutParams(-1,-1));
            }catch(Exception e){throw new IllegalStateException(e);}
        }
        marker=new View(this);marker.setBackgroundColor(Color.GRAY);frame.addView(marker,new FrameLayout.LayoutParams(24,8));
        int height=intent.getBooleanExtra("newRider",false)?1120:(int)(wm.getMaximumWindowMetrics().getBounds().height()*.60);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.y=80;root=frame;wm.addView(root,p);handler.removeCallbacks(animate);handler.post(animate);return START_NOT_STICKY;
    }
    @Override public void onDestroy(){handler.removeCallbacks(animate);if(root!=null)wm.removeViewImmediate(root);bitmap=null;super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
