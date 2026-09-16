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

/** The supplied screenshot stays ignored; only its numeric expectations enter source control. */
public class BlackOfferOcrTest {
    @Test public void fullScreenshot() throws Exception { verify(false); }
    @Test public void liveCaptureRegion() throws Exception { verify(true); }

    private void verify(boolean crop) throws Exception {
        Bitmap bitmap;
        try (InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-055-black.jpg")) {
            bitmap=BitmapFactory.decodeStream(in);
        }
        if(crop) {
            Bitmap next=Bitmap.createBitmap(bitmap,0,bitmap.getHeight()/4,bitmap.getWidth(),bitmap.getHeight()*3/4);
            bitmap.recycle();bitmap=next;
        }
        TextRecognizer recognizer=Ocr.create();
        try {
            OfferParser.Result result=Tasks.await(Ocr.read(recognizer,bitmap),45,TimeUnit.SECONDS);
            android.util.Log.i("Black055","crop="+crop+" reason="+result.reason);
            if(result.offer==null && "true".equals(InstrumentationRegistry.getArguments().getString("diagnosticText")))
                android.util.Log.i("Black055Private","crop="+crop+"\n"+Ocr.orderedText(Tasks.await(recognizer.process(Ocr.image(bitmap)),45,TimeUnit.SECONDS)));
            assertNotNull(result.reason,result.offer);
            OfferParser.Offer o=result.offer;
            assertEquals(14813,o.cents);assertEquals(13.59,o.displayedRate,.001);
            assertEquals(11,o.pickupMinutes);assertEquals(3.3,o.pickupKm,.001);
            assertEquals(20,o.tripMinutes);assertEquals(7.6,o.tripKm,.001);
            assertEquals(4.71,o.rider.rating,.001);assertEquals(Integer.valueOf(191),o.rider.count);
            assertFalse(o.rider.ambiguous);assertEquals("BLACK",o.serviceType.name());
            assertTrue(o.exclusive);assertFalse(o.reserved);assertEquals("Black · Exclusivo",o.typeLabel());
        } finally { bitmap.recycle();recognizer.close(); }
    }
}
