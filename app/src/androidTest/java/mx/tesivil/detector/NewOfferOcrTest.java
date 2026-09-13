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

/** Actual phone screenshots, including our overlay, launcher and exclusive acceptance cards. */
@RunWith(AndroidJUnit4.class)
public class NewOfferOcrTest {
    @Test public void actualProjectionFrameRetainsDecimals()throws Exception {
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-031-actual-mp.png")){b=BitmapFactory.decodeStream(in);}
        TextRecognizer r=Ocr.create();
        try {
            Text text=Tasks.await(r.process(Ocr.image(b)),45,TimeUnit.SECONDS);System.out.println("OCR_ACTUAL_MP\n"+Ocr.orderedText(text));
            OfferParser.Result result=Tasks.await(Ocr.read(r,b),45,TimeUnit.SECONDS);
            assertNotNull(result.reason,result.offer);assertEquals(5990,result.offer.cents);
            assertEquals(1.3,result.offer.pickupKm,.001);assertEquals(5.6,result.offer.tripKm,.001);
        }finally{r.close();b.recycle();}
    }
    @Test public void liveResampledLauncherFrameRetainsDecimals()throws Exception {
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-031-native-59.png")){b=BitmapFactory.decodeStream(in);}
        Bitmap scaled=Bitmap.createScaledBitmap(b,b.getWidth()*1280/b.getHeight(),1280,true);b.recycle();
        b=Bitmap.createBitmap(scaled,0,320,scaled.getWidth(),960);scaled.recycle();TextRecognizer r=Ocr.create();
        try {
            Text text=Tasks.await(r.process(Ocr.image(b)),45,TimeUnit.SECONDS);System.out.println("OCR_RESAMPLED_59\n"+Ocr.orderedText(text));
            OfferParser.Result result=Tasks.await(Ocr.read(r,b),45,TimeUnit.SECONDS);
            assertNotNull(result.reason,result.offer);assertEquals(5990,result.offer.cents);
            assertEquals(1.3,result.offer.pickupKm,.001);assertEquals(5.6,result.offer.tripKm,.001);
        }finally{r.close();b.recycle();}
    }
    @Test public void radarFull()throws Exception{verify(1,false);}
    @Test public void radarLiveRegion()throws Exception{verify(1,true);}
    @Test public void launcherFull()throws Exception{verify(2,false);}
    @Test public void launcherLiveRegion()throws Exception{verify(2,true);}
    @Test public void exclusiveFull()throws Exception{verify(3,false);}
    @Test public void exclusiveLiveRegion()throws Exception{verify(3,true);}
    @Test public void exclusiveNewRiderFull()throws Exception{verify(4,false);}
    @Test public void exclusiveNewRiderLiveRegion()throws Exception{verify(4,true);}
    @Test public void wrongOverlayFareFull()throws Exception{verify(5,false);}
    @Test public void wrongOverlayFareLiveRegion()throws Exception{verify(5,true);}
    private void verify(int number,boolean crop)throws Exception {
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-031-"+number+".jpg")){b=BitmapFactory.decodeStream(in);}
        if(crop){int top=b.getHeight()/4;Bitmap region=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top);b.recycle();b=region;}
        TextRecognizer r=Ocr.create();
        try {
            Text text=Tasks.await(r.process(Ocr.image(b)),45,TimeUnit.SECONDS);
            String ordered=Ocr.orderedText(text);OfferParser.Result result=OfferParser.parse(ordered);
            // Test APK only; exact OCR text is never logged by the delivered app.
            if(result.offer==null || result.offer.cents!=(number<=3?5990:7706) || number==3&&crop)System.out.println("OCR_CORPUS_"+number+"_"+crop+"\n"+ordered);
            OfferParser.Result refined=Tasks.await(Ocr.read(r,b),45,TimeUnit.SECONDS);
            assertNotNull(refined.reason,refined.offer);OfferParser.Offer o=refined.offer;
            assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);assertEquals(number==3||number==4,o.exclusive);
            assertEquals(number<=3?5990:7706,o.cents);assertEquals(5,o.pickupMinutes);
            assertEquals(number<=3?1.3:1.0,o.pickupKm,.001);assertEquals(number<=3?11:15,o.tripMinutes);
            assertEquals(number<=3?5.6:8.2,o.tripKm,.001);assertNotNull(o.rider.rating);
            assertEquals(number<=3?4.73:5.0,o.rider.rating,.001);assertEquals(Integer.valueOf(number<=3?463:3),o.rider.count);
            assertFalse(o.rider.ambiguous);assertTrue(ZoneRule.norm(o.pickupAddress).contains(number<=3?"avellana":"cicas"));
            assertTrue(ZoneRule.norm(o.destinationAddress).contains(number<=3?"altaluz":"base militar"));
        }finally{r.close();b.recycle();}
    }
}
