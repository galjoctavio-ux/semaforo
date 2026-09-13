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

/** Two stages of the blue animation, their radar card, and a real Mexican Priority card. */
@RunWith(AndroidJUnit4.class)
public class ExclusiveEffectOcrTest {
    @Test public void actualResampledBlueFrame()throws Exception{
        Bitmap full;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-033-native-blue.png")){full=BitmapFactory.decodeStream(in);}
        Bitmap scaled=Bitmap.createScaledBitmap(full,full.getWidth()*1280/full.getHeight(),1280,true);full.recycle();
        Bitmap crop=Bitmap.createBitmap(scaled,0,320,scaled.getWidth(),960);scaled.recycle();
        TextRecognizer reader=Ocr.create();
        try{
            OfferParser.Result r=Tasks.await(Ocr.read(reader,crop),45,TimeUnit.SECONDS);
            assertNotNull("Resampled native blue: "+r.reason,r.offer);
            assertEquals(8017,r.offer.cents);assertEquals(1.4,r.offer.pickupKm,.001);assertEquals(6.8,r.offer.tripKm,.001);
            assertEquals(Integer.valueOf(64),r.offer.rider.count);assertTrue(r.offer.exclusive);
        }finally{reader.close();crop.recycle();}
    }
    @Test public void blueLowerHalf()throws Exception{verifyBoth(1);}
    @Test public void blueWholeCard()throws Exception{verifyBoth(2);}
    @Test public void sameRadarCard()throws Exception{verifyBoth(3);}
    @Test public void anotherRadarCard()throws Exception{verifyBoth(4);}
    @Test public void actualPrioritySurchargeNotAddedTwice()throws Exception{verifyBoth(5);}
    @Test public void animatedExclusiveAndRadarConfirmSameData()throws Exception{
        TextRecognizer reader=Ocr.create();
        try{
            OfferConfirmation confirmation=new OfferConfirmation();
            OfferParser.Offer radar=read(reader,3,true),lower=read(reader,1,true),whole=read(reader,2,true);
            assertFalse(confirmation.observe(radar,100));
            assertTrue("Blue phase reset confirmation",confirmation.observe(lower,520));
            assertTrue("Whole blue card reset confirmation",confirmation.observe(whole,940));
        }finally{reader.close();}
    }
    private void verifyBoth(int number)throws Exception{
        TextRecognizer reader=Ocr.create();try{read(reader,number,false);read(reader,number,true);}finally{reader.close();}
    }
    private OfferParser.Offer read(TextRecognizer reader,int number,boolean liveRegion)throws Exception{
        Bitmap b;try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-033-"+number+".jpg")){b=BitmapFactory.decodeStream(in);}
        if(liveRegion){int top=b.getHeight()/4;Bitmap crop=Bitmap.createBitmap(b,0,top,b.getWidth(),b.getHeight()-top);if(crop!=b)b.recycle();b=crop;}
        try{
            OfferParser.Result r=Tasks.await(Ocr.read(reader,b),45,TimeUnit.SECONDS);
            assertNotNull("Photo "+number+" crop="+liveRegion+": "+r.reason,r.offer);
            OfferParser.Offer o=r.offer;
            assertEquals(number==5?OfferParser.ServiceType.PRIORITY:OfferParser.ServiceType.UBER_X,o.serviceType);
            assertEquals(number<=2,o.exclusive);assertEquals(number<=3?8017:number==4?12934:6549,o.cents);
            assertEquals(5,o.pickupMinutes);assertEquals(number<=3?1.4:number==4?1.1:.9,o.pickupKm,.001);
            assertEquals(number<=3?16:number==4?30:13,o.tripMinutes);assertEquals(number<=3?6.8:number==4?12.2:4.8,o.tripKm,.001);
            assertEquals(number<=3?4.84:number==4?4.88:4.86,o.rider.rating,.001);
            assertEquals(Integer.valueOf(number<=3?64:number==4?151:102),o.rider.count);assertFalse(o.rider.ambiguous);
            assertTrue(ZoneRule.norm(o.pickupAddress).contains(number<=3?"regalis":number==4?"molino de trigo":"harina"));
            assertTrue(ZoneRule.norm(o.destinationAddress).contains(number<=3?"mirador del bosque":number==4?"colinas de tesistan":"copala"));
            return o;
        }finally{b.recycle();}
    }
}
