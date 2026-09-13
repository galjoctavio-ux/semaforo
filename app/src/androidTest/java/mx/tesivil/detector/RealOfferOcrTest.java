package mx.tesivil.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RealOfferOcrTest {
    @Test public void readsHomeScreenOfferWithRider()throws Exception{verifyCurrent("user-offer-home.jpg",6210,4.92,149);}
    @Test public void readsOfferWithOwnOverlayAndRider()throws Exception{verifyCurrent("user-offer-overlay.jpg",9642,4.87,391);}
    private void verifyCurrent(String asset,long cents,double rating,int count)throws Exception {
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(asset)){b=BitmapFactory.decodeStream(in);}
        TextRecognizer r=Ocr.create();
        try{Text t=Tasks.await(r.process(Ocr.image(b)),45,TimeUnit.SECONDS);OfferParser.Offer o=OfferParser.parse(Ocr.orderedText(t)).offer;assertNotNull(o);assertEquals(cents,o.cents);assertNotNull("Rating missing",o.rider.rating);assertEquals(rating,o.rider.rating,.001);assertEquals(Integer.valueOf(count),o.rider.count);assertFalse(o.rider.ambiguous);}
        finally{r.close();b.recycle();}
    }
    @Test public void bundledOcrReadsUserScreenshot() throws Exception { verify(false); }
    @Test public void bundledOcrReadsLiveCaptureRegion() throws Exception { verify(true); }
    @Test public void readsNewVivoOfferAndSeparateEndpoints() throws Exception {
        Bitmap bitmap;
        try(InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-offer-vivo.jpg")){bitmap=BitmapFactory.decodeStream(input);}
        TextRecognizer recognizer=Ocr.create();
        try{
            Text text=Tasks.await(recognizer.process(Ocr.image(bitmap)),45,TimeUnit.SECONDS);
            OfferParser.Offer o=OfferParser.parse(Ocr.orderedText(text)).offer;
            assertNotNull(o);assertEquals(9831,o.cents);assertEquals(14,o.pickupMinutes);assertEquals(6.3,o.pickupKm,.001);assertEquals(18,o.tripMinutes);assertEquals(4,o.tripKm,.001);
            assertTrue(ZoneRule.norm(o.pickupAddress).contains("molinos"));assertTrue(ZoneRule.norm(o.destinationAddress).contains("palomas"));
            assertEquals(4.83,o.rider.rating,.001);assertEquals(Integer.valueOf(10),o.rider.count);
        }finally{recognizer.close();bitmap.recycle();}
    }
    private void verify(boolean crop) throws Exception {
        Bitmap bitmap;
        try (InputStream input = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-offer.jpg")) {
            bitmap = BitmapFactory.decodeStream(input);
        }
        assertNotNull(bitmap);
        if (crop) {
            int top = bitmap.getHeight() / 4;
            Bitmap region = Bitmap.createBitmap(bitmap, 0, top, bitmap.getWidth(), bitmap.getHeight() - top);
            bitmap.recycle(); bitmap = region;
        }
        TextRecognizer recognizer = Ocr.create();
        try {
            Text text = Tasks.await(recognizer.process(Ocr.image(bitmap)), 45, TimeUnit.SECONDS);
            OfferParser.Result parsed = OfferParser.parse(Ocr.orderedText(text));
            assertNotNull("Missing fields: " + parsed.reason, parsed.offer);
            assertEquals(6956, parsed.offer.cents);
            assertEquals(5, parsed.offer.pickupMinutes);
            assertEquals(1.7, parsed.offer.pickupKm, 0.001);
            assertEquals(18, parsed.offer.tripMinutes);
            assertEquals(4.7, parsed.offer.tripKm, 0.001);
            assertEquals(4.83,parsed.offer.rider.rating,.001);assertEquals(Integer.valueOf(370),parsed.offer.rider.count);
        } finally { recognizer.close(); bitmap.recycle(); }
    }
}
