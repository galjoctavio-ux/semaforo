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

/** Local, private samples: currency prefixes, dark cards and hour durations. */
@RunWith(AndroidJUnit4.class)
public class DarkThemeOcrTest {
    @Test public void croppedDarkCardCannotBecomeARecommendation() throws Exception { verify(1); }
    @Test public void darkComfortWithHours() throws Exception { verify(2); }
    @Test public void lightMxnAndWrappedRider() throws Exception { verify(3); }
    @Test public void darkExclusiveComfortWithHours() throws Exception { verify(4); }
    @Test public void darkExclusivePriorityWithLabeledSurcharge() throws Exception { verify(5); }

    private void verify(int number) throws Exception {
        TextRecognizer reader=Ocr.create();
        try { for(boolean liveRegion:new boolean[]{false,true}) {
            Bitmap b;
            try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-034-"+number+".jpg")) { b=BitmapFactory.decodeStream(in); }
            if(liveRegion) { int top=b.getHeight()/4; Bitmap crop=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top); if(crop!=b)b.recycle(); b=crop; }
            try {
                long started=android.os.SystemClock.elapsedRealtime();
                OfferParser.Result r=Tasks.await(Ocr.read(reader,b),45,TimeUnit.SECONDS);
                android.util.Log.i("Ocr034Sample","Photo "+number+" crop="+liveRegion+" result="+r.reason+" ms="+(android.os.SystemClock.elapsedRealtime()-started));
                if(number==1) { assertNull("Cropped card lacks its action",r.offer); assertTrue(r.reason.contains("incompleta")); continue; }
                assertNotNull("Photo "+number+" crop="+liveRegion+": "+r.reason,r.offer);
                OfferParser.Offer o=r.offer;
                assertEquals(number==3?"UBER_X":number==5?"PRIORITY":"COMFORT",o.serviceType.name());
                assertEquals(number==2||number==4?1:0,o.destinationNoticeCount);
                assertEquals(number==3?13853:number==5?6603:23582,o.cents);
                assertEquals(number==2?4:number==3?1:number==4?2:11,o.pickupMinutes);
                assertEquals(number==2?1:number==3?.1:number==4?.6:6.7,o.pickupKm,.001);
                assertEquals(number==3?35:number==5?5:63,o.tripMinutes);
                assertEquals(number==3?26.1:number==5?1.3:16.3,o.tripKm,.001);
                assertEquals(number==3?4.79:number==5?4.91:4.88,o.rider.rating,.001);
                assertEquals(Integer.valueOf(number==3?16:number==5?167:19),o.rider.count);
                assertFalse(o.rider.ambiguous); assertEquals(number>=4,o.exclusive);
                assertEquals(number==3?5.31:number==5?8.25:number==2?13.63:13.95,o.displayedRate,.001);
                assertTrue(ZoneRule.norm(o.pickupAddress).contains(number==3?"volcan":number==5?"escondida":"lerdo"));
                assertTrue(ZoneRule.norm(o.destinationAddress).contains(number==3?"arenales tapatios":number==5?"huizachera":"calle guadalajara"));
                assertFalse(o.pickupAddress.contains("1 destino"));
                assertFalse(o.destinationAddress.contains("45+"));
            } finally { b.recycle(); }
        } } finally { reader.close(); }
    }
}
