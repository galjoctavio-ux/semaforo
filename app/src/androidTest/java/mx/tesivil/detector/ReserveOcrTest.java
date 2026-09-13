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

/** Private sample of a dark Reserve card; absent per-km rate is not invented. */
@RunWith(AndroidJUnit4.class)
public class ReserveOcrTest {
    @Test public void darkReserveFullImage() throws Exception { verify(false); }
    @Test public void darkReserveLiveRegion() throws Exception { verify(true); }
    private void verify(boolean liveRegion) throws Exception {
        Bitmap b;
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-035-reserve.jpg")) { b=BitmapFactory.decodeStream(in); }
        if(liveRegion) { int top=b.getHeight()/4; Bitmap crop=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top); if(crop!=b)b.recycle(); b=crop; }
        TextRecognizer reader=Ocr.create();
        try {
            OfferParser.Result r=Tasks.await(Ocr.read(reader,b),45,TimeUnit.SECONDS);
            assertNotNull(r.reason,r.offer);
            OfferParser.Offer o=r.offer;
            assertEquals(26562,o.cents);assertEquals(24,o.pickupMinutes);assertEquals(14.4,o.pickupKm,.001);
            assertEquals(29,o.tripMinutes);assertEquals(21.4,o.tripKm,.001);assertNull(o.displayedRate);
            assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);assertFalse(o.exclusive);assertTrue(o.reserved);
            assertEquals(4.91,o.rider.rating,.001);assertEquals(Integer.valueOf(74),o.rider.count);assertFalse(o.rider.ambiguous);
            assertTrue(ZoneRule.norm(o.pickupAddress).contains("hacienda san edgar"));
            assertTrue(ZoneRule.norm(o.destinationAddress).contains("barranquitas"));
            assertFalse(o.destinationAddress.contains("reserva"));
        } finally { reader.close();b.recycle(); }
    }
}
