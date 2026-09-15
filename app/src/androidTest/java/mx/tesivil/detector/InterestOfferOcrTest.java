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

/** Private supplied screenshots stay ignored; only synthetic expectations enter source control. */
public class InterestOfferOcrTest {
    private static final Object[][] CASES={
            {"user-054-1.jpg",3536,6.80,3,.0+1.3,9,3.9,4.82,47,OfferParser.ServiceType.UBER_X,false},
            {"user-054-2.jpg",5544,8.66,9,2.6,13,3.9,4.83,83,OfferParser.ServiceType.UBER_X,false},
            {"user-054-3.jpg",3647,9.86,4,.8,12,2.9,4.76,231,OfferParser.ServiceType.UBER_X,false},
            // ML Kit reads the visible $11.08/km as $1l08/km. It must remain unknown, never inferred.
            {"user-054-4.jpg",4212,null,4,1.3,9,2.6,4.93,114,OfferParser.ServiceType.PRIORITY,false},
            {"user-054-5.jpg",5279,6.95,3,.6,17,6.9,4.85,469,OfferParser.ServiceType.UBER_X,true}
    };
    @Test public void image1Full()throws Exception{verify(0,false);}
    @Test public void image1CaptureRegion()throws Exception{verify(0,true);}
    @Test public void image2Full()throws Exception{verify(1,false);}
    @Test public void image2CaptureRegion()throws Exception{verify(1,true);}
    @Test public void image3Full()throws Exception{verify(2,false);}
    @Test public void image3CaptureRegion()throws Exception{verify(2,true);}
    @Test public void image4Full()throws Exception{verify(3,false);}
    @Test public void image4CaptureRegion()throws Exception{verify(3,true);}
    @Test public void image5Full()throws Exception{verify(4,false);}
    @Test public void image5CaptureRegion()throws Exception{verify(4,true);}
    private void verify(int index,boolean crop)throws Exception{
        Object[] expected=CASES[index];Bitmap bitmap;
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open((String)expected[0])){bitmap=BitmapFactory.decodeStream(in);}
        if(crop){Bitmap next=Bitmap.createBitmap(bitmap,0,bitmap.getHeight()/4,bitmap.getWidth(),bitmap.getHeight()*3/4);bitmap.recycle();bitmap=next;}
        TextRecognizer recognizer=Ocr.create();
        try{
            long started=android.os.SystemClock.elapsedRealtime();OfferParser.Result result=Tasks.await(Ocr.read(recognizer,bitmap),45,TimeUnit.SECONDS);
            android.util.Log.i("Interest054","image="+(index+1)+" crop="+crop+" reason="+result.reason+" ms="+(android.os.SystemClock.elapsedRealtime()-started));
            if(result.offer==null&&"true".equals(InstrumentationRegistry.getArguments().getString("diagnosticText")))
                android.util.Log.i("Interest054Private","image="+(index+1)+" crop="+crop+"\n"+Ocr.orderedText(Tasks.await(recognizer.process(Ocr.image(bitmap)),45,TimeUnit.SECONDS)));
            assertNotNull(result.reason,result.offer);OfferParser.Offer o=result.offer;
            assertEquals(((Number)expected[1]).longValue(),o.cents);
            if(expected[2]==null)assertNull(o.displayedRate);else assertEquals((Double)expected[2],o.displayedRate,.001);
            assertEquals(expected[3],o.pickupMinutes);assertEquals((Double)expected[4],o.pickupKm,.001);
            assertEquals(expected[5],o.tripMinutes);assertEquals((Double)expected[6],o.tripKm,.001);
            assertEquals((Double)expected[7],o.rider.rating,.001);assertEquals(Integer.valueOf((Integer)expected[8]),o.rider.count);
            assertFalse(o.rider.ambiguous);assertEquals(expected[9],o.serviceType);assertEquals(expected[10],o.exclusive);assertFalse(o.reserved);
        }finally{bitmap.recycle();recognizer.close();}
    }
}
