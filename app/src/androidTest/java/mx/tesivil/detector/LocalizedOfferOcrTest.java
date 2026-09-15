package mx.tesivil.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.text.TextRecognizer;
import org.junit.Test;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class LocalizedOfferOcrTest {
    @Test public void fullImage()throws Exception{verify(false);}
    @Test public void liveCaptureRegion()throws Exception{verify(true);}
    private void verify(boolean crop)throws Exception{
        Bitmap bitmap;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-052-1.jpg")){bitmap=BitmapFactory.decodeStream(in);}
        if(crop){Bitmap next=Bitmap.createBitmap(bitmap,0,bitmap.getHeight()/4,bitmap.getWidth(),bitmap.getHeight()*3/4);if(next!=bitmap)bitmap.recycle();bitmap=next;}
        TextRecognizer r=Ocr.create();
        try{
            long start=android.os.SystemClock.elapsedRealtime();OfferParser.Result result=Tasks.await(Ocr.read(r,bitmap),45,TimeUnit.SECONDS);
            android.util.Log.i("Localized052","crop="+crop+" reason="+result.reason+" ms="+(android.os.SystemClock.elapsedRealtime()-start));
            if(result.offer==null&&"true".equals(InstrumentationRegistry.getArguments().getString("diagnosticText")))
                android.util.Log.i("Localized052Private",Ocr.orderedText(Tasks.await(r.process(Ocr.image(bitmap)),45,TimeUnit.SECONDS)));
            assertNotNull(result.reason,result.offer);OfferParser.Offer o=result.offer;
            assertEquals(15305,o.cents);assertEquals(6.05,o.displayedRate,.001);assertTrue(o.exclusive);
            assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);assertFalse(o.reserved);
            assertEquals(3,o.pickupMinutes);assertEquals(.7,o.pickupKm,.001);
            assertEquals(33,o.tripMinutes);assertEquals(24.6,o.tripKm,.001);
            assertEquals(4.87,o.rider.rating,.001);assertEquals(Integer.valueOf(15),o.rider.count);assertFalse(o.rider.ambiguous);
            assertTrue(ZoneRule.norm(o.pickupAddress).contains("castellanos"));assertTrue(ZoneRule.norm(o.pickupAddress).contains("tulipanes"));
            assertTrue(ZoneRule.norm(o.destinationAddress).contains("puente grande"));
            assertFalse(o.pickupAddress.contains("de distancia"));assertEquals(36,o.totalMinutes());assertEquals(25.3,o.totalKm(),.001);
        }finally{bitmap.recycle();r.close();}
    }
}
