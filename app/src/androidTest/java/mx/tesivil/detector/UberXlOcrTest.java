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

/** Phone screenshots live only in the separate instrumentation APK. */
@RunWith(AndroidJUnit4.class)
public class UberXlOcrTest {
    @Test public void uberXOverAnotherApp()throws Exception{verify(1,false);}
    @Test public void uberXLiveRegion()throws Exception{verify(1,true);}
    @Test public void xlRadar()throws Exception{verify(2,false);}
    @Test public void xlRadarLiveRegion()throws Exception{verify(2,true);}
    @Test public void xlExclusive()throws Exception{verify(3,false);}
    @Test public void xlExclusiveLiveRegion()throws Exception{verify(3,true);}
    @Test public void xlChangedPrice()throws Exception{verify(4,false);}
    @Test public void xlChangedPriceLiveRegion()throws Exception{verify(4,true);}
    private void verify(int number,boolean liveRegion)throws Exception{
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-032-"+number+".jpg")){b=BitmapFactory.decodeStream(in);}
        if(liveRegion){int top=b.getHeight()/4;Bitmap crop=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top);if(crop!=b)b.recycle();b=crop;}
        TextRecognizer reader=Ocr.create();
        try{
            OfferParser.Result r=Tasks.await(Ocr.read(reader,b),45,TimeUnit.SECONDS);assertNotNull(r.reason,r.offer);
            OfferParser.Offer o=r.offer;boolean x=number==1;
            assertEquals(x?OfferParser.ServiceType.UBER_X:OfferParser.ServiceType.UBER_XL,o.serviceType);
            assertEquals(number>=3,o.exclusive);assertEquals(x?12327:number==4?23682:28493,o.cents);
            assertEquals(x?6:19,o.pickupMinutes);assertEquals(x?1.8:8.5,o.pickupKm,.001);
            assertEquals(x?30:41,o.tripMinutes);assertEquals(x?13.4:17.1,o.tripKm,.001);
            assertEquals(x?4.80:4.87,o.rider.rating,.001);assertEquals(Integer.valueOf(x?393:91),o.rider.count);
            assertFalse(o.rider.ambiguous);
            assertTrue(ZoneRule.norm(o.pickupAddress).contains(x?"25 de mayo":"base militar"));
            assertTrue(ZoneRule.norm(o.destinationAddress).contains(x?"arcos de zapopan":"colonia belisario"));
        }finally{reader.close();b.recycle();}
    }
}
