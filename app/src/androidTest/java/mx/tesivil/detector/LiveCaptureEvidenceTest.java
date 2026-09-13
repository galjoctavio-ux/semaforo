package mx.tesivil.detector;

import android.content.Intent;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Explicitly selected only: a real projection authorized through Android's consent UI. */
public class LiveCaptureEvidenceTest {
    @Test public void recordProjectionStability() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        long expected=Long.parseLong(InstrumentationRegistry.getArguments().getString("fare","11803"));
        int duration=Integer.parseInt(InstrumentationRegistry.getArguments().getString("duration","16"));
        instrumentation.getTargetContext().startActivity(new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        AtomicBoolean ready=new AtomicBoolean();
        long deadline=SystemClock.elapsedRealtime()+180000;
        while(SystemClock.elapsedRealtime()<deadline&&!ready.get()) {
            instrumentation.runOnMainSync(()->ready.set(Diagnostics.running&&Diagnostics.evaluation!=null&&Diagnostics.offer!=null&&Diagnostics.offer.cents==expected));
            Thread.sleep(100);
        }
        assertTrue("No confirmed offer received from the authorized projection",ready.get());
        measure(expected,duration);
        if("true".equals(InstrumentationRegistry.getArguments().getString("exerciseTransitions"))){
            show("user-041-4.jpg",false);awaitFare(11803);measure(11803,10);
            show("user-041-2.jpg",true);
            Thread.sleep(500);
            instrumentation.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            awaitFare(9292);measure(9292,10);
            instrumentation.getTargetContext().stopService(new Intent().setComponent(new android.content.ComponentName("mx.tesivil.detector.test","mx.tesivil.detector.fixture.FloatingOfferService")));
            instrumentation.getTargetContext().startActivity(new Intent().setComponent(new android.content.ComponentName("mx.tesivil.detector.test","mx.tesivil.detector.fixture.SampleOfferActivity")).putExtra("blank",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            AtomicBoolean cleared=new AtomicBoolean();long end=SystemClock.elapsedRealtime()+4000;
            while(SystemClock.elapsedRealtime()<end&&!cleared.get()){
                instrumentation.runOnMainSync(()->cleared.set(Diagnostics.offer==null&&Diagnostics.evaluation==null));Thread.sleep(100);
            }
            assertTrue("The removed offer left a score on screen",cleared.get());
            android.util.Log.i("Live041Evidence","REMOVED offer_and_score_cleared=true");
            instrumentation.getTargetContext().startService(new Intent(instrumentation.getTargetContext(),CaptureService.class).setAction(CaptureService.STOP));
        }
    }
    private void show(String asset,boolean floating){
        InstrumentationRegistry.getInstrumentation().getTargetContext().startActivity(new Intent().setComponent(new android.content.ComponentName("mx.tesivil.detector.test","mx.tesivil.detector.fixture.SampleOfferActivity")).putExtra("asset",asset).putExtra("floating",floating).putExtra("cardTop",.438).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private void awaitFare(long expected)throws Exception{
        AtomicBoolean found=new AtomicBoolean();long until=SystemClock.elapsedRealtime()+12000;
        while(SystemClock.elapsedRealtime()<until&&!found.get()){
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->found.set(Diagnostics.evaluation!=null&&Diagnostics.offer!=null&&Diagnostics.offer.cents==expected));Thread.sleep(100);
        }
        assertTrue("Changed offer was not confirmed: "+expected,found.get());
    }
    private void measure(long expected,int duration)throws Exception{
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        int[] counts={0,0}; long[] frameCounts={0,0};
        instrumentation.runOnMainSync(()->{frameCounts[0]=Diagnostics.frames;frameCounts[1]=Diagnostics.validFrames;});
        long until=SystemClock.elapsedRealtime()+duration*1000L;
        while(SystemClock.elapsedRealtime()<until) {
            instrumentation.runOnMainSync(()->{
                counts[0]++;
                boolean shown=Diagnostics.offer!=null&&Diagnostics.evaluation!=null&&Diagnostics.offer.cents==expected;
                if(!shown)counts[1]++;
                android.util.Log.i("Live041Evidence","state="+(shown?"confirmed":Diagnostics.message)+" frames="+Diagnostics.frames+" valid="+Diagnostics.validFrames+" ocr="+Diagnostics.lastOcrMs);
            });
            Thread.sleep(100);
        }
        instrumentation.runOnMainSync(()->android.util.Log.i("Live041Evidence","SUMMARY fare="+expected+" seconds="+duration+" samples="+counts[0]+" missing="+counts[1]+" frames="+(Diagnostics.frames-frameCounts[0])+" complete="+(Diagnostics.validFrames-frameCounts[1])));
        if("true".equals(InstrumentationRegistry.getArguments().getString("assertStable")))assertEquals("A confirmed card flickered",0,counts[1]);
    }
}
