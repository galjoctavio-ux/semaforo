package mx.tesivil.detector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.text.TextRecognizer;
import org.junit.Test;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class LiveOcrSequenceTest {
    private Bitmap sample(int n) throws Exception {
        try(InputStream in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("user-041-"+n+".jpg")){
            Bitmap full=BitmapFactory.decodeStream(in);
            Bitmap crop=Bitmap.createBitmap(full,0,full.getHeight()/4,full.getWidth(),full.getHeight()*3/4);
            if(full!=crop)full.recycle();return crop;
        }
    }
    private OfferParser.Offer read(TextRecognizer r,Bitmap b,long fare)throws Exception{
        long start=android.os.SystemClock.elapsedRealtime();
        OfferParser.Result result=Tasks.await(Ocr.read(r,b),45,TimeUnit.SECONDS);
        android.util.Log.i("LiveOcr041","expected="+fare+" result="+result.reason+" ms="+(android.os.SystemClock.elapsedRealtime()-start));
        assertNotNull(result.reason,result.offer);assertEquals(fare,result.offer.cents);return result.offer;
    }
    @Test public void sameDarkCardHasStableFieldsAndConfirmation()throws Exception{
        TextRecognizer r=Ocr.create();Bitmap b=sample(4);
        try{
            OfferParser.Offer first=read(r,b,11803);OfferConfirmation c=new OfferConfirmation();assertFalse(c.observe(first,100));
            for(int i=1;i<=5;i++){
                OfferParser.Offer next=read(r,b,11803);
                assertEquals(ZoneRule.norm(first.pickupAddress),ZoneRule.norm(next.pickupAddress));
                assertEquals(ZoneRule.norm(first.destinationAddress),ZoneRule.norm(next.destinationAddress));
                assertTrue("Stable card lost confirmation at frame "+i,c.observe(next,100+i*500));
            }
        }finally{b.recycle();r.close();}
    }
    @Test public void differentCardsAndTheirDisappearanceNeverReuseAnOldOffer()throws Exception{
        TextRecognizer r=Ocr.create();
        try{
            for(int n:new int[]{1,4,2,3,1}){
                Bitmap b=sample(n);
                try{read(r,b,new long[]{11433,9292,6106,11803}[n-1]);}finally{b.recycle();}
            }
            Bitmap blank=Bitmap.createBitmap(578,960,Bitmap.Config.ARGB_8888);blank.eraseColor(Color.DKGRAY);
            try{assertNull(Tasks.await(Ocr.read(r,blank),45,TimeUnit.SECONDS).offer);}finally{blank.recycle();}
            Bitmap again=sample(1);try{read(r,again,11433);}finally{again.recycle();}
        }finally{r.close();}
    }
    @Test public void movedCardAndResizedCaptureAreReadFromTheCurrentPixels()throws Exception{
        TextRecognizer r=Ocr.create();Bitmap first=sample(4),next=sample(1);
        Bitmap moved=Bitmap.createBitmap(578,1280,Bitmap.Config.ARGB_8888);
        moved.eraseColor(Color.DKGRAY);new Canvas(moved).drawBitmap(next,0,-170,null);
        try{
            read(r,first,11803);read(r,moved,11433);
            // Same dimensions, with a different position: no geometry or result from the old frame.
            moved.eraseColor(Color.DKGRAY);new Canvas(moved).drawBitmap(next,0,210,null);
            read(r,moved,11433);
        }finally{first.recycle();next.recycle();moved.recycle();r.close();}
    }
}
