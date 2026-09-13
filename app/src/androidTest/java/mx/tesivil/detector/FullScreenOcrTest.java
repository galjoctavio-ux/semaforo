package mx.tesivil.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.text.TextRecognizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Private regression samples: dark in-app cards and light cards over the launcher. */
@RunWith(AndroidJUnit4.class)
public class FullScreenOcrTest {
    @Test public void dark11433() throws Exception { verify(1); }
    @Test public void launcher9292() throws Exception { verify(2); }
    @Test public void launcher6106() throws Exception { verify(3); }
    @Test public void dark11803() throws Exception { verify(4); }

    private void verify(int number) throws Exception {
        TextRecognizer reader=Ocr.create();
        try { for(boolean liveRegion:new boolean[]{false,true}) {
            Bitmap b;
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-041-"+number+".jpg")) { b=BitmapFactory.decodeStream(in); }
            if(liveRegion) { int top=b.getHeight()/4; Bitmap crop=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top); if(crop!=b)b.recycle(); b=crop; }
            try {
                long started=android.os.SystemClock.elapsedRealtime();
                OfferParser.Result r=Tasks.await(Ocr.read(reader,b),45,TimeUnit.SECONDS);
                android.util.Log.i("Ocr041Sample","Photo "+number+" crop="+liveRegion+" result="+r.reason+" ms="+(android.os.SystemClock.elapsedRealtime()-started));
                if(r.offer==null && "true".equals(InstrumentationRegistry.getArguments().getString("diagnosticText")))
                    android.util.Log.i("Ocr041Private",Ocr.orderedText(Tasks.await(reader.process(Ocr.image(b)),45,TimeUnit.SECONDS)));
                assertNotNull("Photo "+number+" crop="+liveRegion+": "+r.reason,r.offer);
                OfferParser.Offer o=r.offer;
                assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);
                assertEquals(new long[]{11433,9292,6106,11803}[number-1],o.cents);
                assertEquals(new int[]{8,18,10,13}[number-1],o.pickupMinutes);
                assertEquals(new double[]{4.1,8.7,3.0,3.9}[number-1],o.pickupKm,.001);
                assertEquals(new int[]{22,10,12,18}[number-1],o.tripMinutes);
                assertEquals(new double[]{10.5,4,3.5,9}[number-1],o.tripKm,.001);
                assertEquals(new double[]{4.75,4.91,4.92,4.81}[number-1],o.rider.rating,.001);
                assertEquals(Integer.valueOf(new int[]{55,107,289,390}[number-1]),o.rider.count);
                assertEquals(new double[]{7.88,7.32,9.25,9.15}[number-1],o.displayedRate,.001);
                assertFalse(o.rider.ambiguous); assertFalse(o.exclusive); assertFalse(o.reserved);
                assertTrue(ZoneRule.norm(o.pickupAddress).contains(new String[]{"pieras","fuente brillante","violeta","borgo"}[number-1]));
                assertTrue(ZoneRule.norm(o.destinationAddress).contains(new String[]{"radio 21","base militar","privada las","base militar"}[number-1]));
            } finally { b.recycle(); }
        } } finally { reader.close(); }
    }
}
