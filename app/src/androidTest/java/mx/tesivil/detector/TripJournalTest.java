package mx.tesivil.detector;

import android.app.Activity;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;

/** Only for the owned emulator: resets local history and profile. */
@RunWith(AndroidJUnit4.class)
public class TripJournalTest {
    private Context context;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void reset(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();main(()->{Diagnostics.init(context);Diagnostics.begin();HistoryStore.clear();Diagnostics.running=false;});}
    private JSONObject row(String id,String group,long fare,double min,double km)throws Exception{
        JSONObject r=new JSONObject();r.put("local_id",id);r.put("episode_id",group);r.put("fare_cents",fare);r.put("offer_version",1);r.put("observed_at_ms",System.currentTimeMillis());r.put("outcome","unknown");r.put("source","test");r.put("config",ConfigStore.json(new DriverConfig()));
        JSONObject e=new JSONObject();e.put("color","AMBAR");e.put("score",60);e.put("reasons",new JSONArray().put("Ejemplo"));e.put("revenue",fare/100d);e.put("minutes",min);e.put("km",km);e.put("hourly",150);r.put("evaluation",e);return r;
    }
    private TripOutcome completed(){TripOutcome r=new TripOutcome();r.state=TripOutcome.State.COMPLETED;r.settledCents=18000L;r.totalMinutes=40d;r.totalKm=12d;return r;}
    @Test public void resultRoundTripPreservesUnknownCosts()throws Exception{TripOutcome r=TripJournal.parse(TripJournal.json(completed()));assertNull(r.energyCents);assertNull(r.actualMargin());assertEquals(18000L,(long)r.settledCents);}
    @Test public void corruptFractionalCentCostsAreRejected()throws Exception{JSONObject j=TripJournal.json(completed());j.put("energy_cents",1.5);try{TripJournal.parse(j);fail();}catch(JSONException expected){}j.put("energy_cents","0");try{TripJournal.parse(j);fail();}catch(JSONException expected){}}
    @Test public void versionsProduceOneActualResultComparedToItsExactAnchor()throws Exception{
        HistoryStore.add(row("older","same",20000,30,10));HistoryStore.add(row("newer","same",15000,35,11));HistoryStore.add(row("other","different",10000,20,8));
        HistoryStore.saveOutcome("older",completed());JSONArray rows=HistoryStore.snapshot();assertTrue(rows.getJSONObject(0).has("actual_result"));assertFalse(rows.getJSONObject(1).has("actual_result"));assertFalse(rows.getJSONObject(2).has("actual_result"));
        TripJournal.Stats s=TripJournal.stats(rows);assertEquals(2,s.groups);assertEquals(1,s.completed);assertEquals(20,s.revenueAbsoluteError,0);assertEquals(10,s.minutesAbsoluteError,0);assertEquals(2,s.kmAbsoluteError,0);assertEquals(0,s.fullyCosted);
        HistoryStore.saveOutcome("newer",completed());s=TripJournal.stats(HistoryStore.snapshot());assertEquals(1,s.completed);assertEquals(30,s.revenueAbsoluteError,0);assertFalse(HistoryStore.snapshot().getJSONObject(0).has("actual_result"));
    }
    @Test public void unknownCostsAreExcludedRatherThanCountedAsFreeTrips()throws Exception{JSONObject a=row("a","a",20000,30,10),b=row("b","b",20000,30,10);HistoryStore.add(a);HistoryStore.add(b);HistoryStore.saveOutcome("a",completed());TripOutcome r=completed();r.energyCents=2000L;r.upkeepCents=1000L;r.fixedCents=r.extrasCents=0L;HistoryStore.saveOutcome("b",r);TripJournal.Stats s=TripJournal.stats(HistoryStore.snapshot());assertEquals(2,s.completed);assertEquals(1,s.fullyCosted);assertEquals(150,s.marginSum,0);assertEquals(40,s.minutesWithCosts,0);}
    @Test public void acceptedAndCancelledStatesDoNotGenerateCompletedIncome()throws Exception{HistoryStore.add(row("a","a",20000,30,10));TripOutcome r=new TripOutcome();r.state=TripOutcome.State.ACCEPTED;HistoryStore.saveOutcome("a",r);assertEquals(0,TripJournal.stats(HistoryStore.snapshot()).completed);r.state=TripOutcome.State.CANCELLED;HistoryStore.saveOutcome("a",r);assertEquals(0,TripJournal.stats(HistoryStore.snapshot()).completed);r.state=TripOutcome.State.UNKNOWN;HistoryStore.saveOutcome("a",r);assertFalse(HistoryStore.snapshot().getJSONObject(0).has("actual_result"));}
    @Test public void saveCommitsToDiskAndCannotAttachToAnUnrelatedLatestOffer()throws Exception{HistoryStore.add(row("selected","g1",20000,30,10));HistoryStore.add(row("latest","g2",15000,30,10));HistoryStore.saveOutcome("selected",completed());JSONArray disk=new JSONArray(new String(Files.readAllBytes(new File(context.getFilesDir(),"offer-history-v2.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));assertTrue(disk.getJSONObject(0).has("actual_result"));assertFalse(disk.getJSONObject(1).has("actual_result"));String before=HistoryStore.snapshot().toString();try{HistoryStore.saveOutcome("removed",completed());fail();}catch(IllegalArgumentException expected){}assertEquals(before,HistoryStore.snapshot().toString());}
    @Test public void boundProtectsRecordedResultsBeforeUnrecordedSnapshots()throws Exception{HistoryStore.add(row("recorded","one",20000,30,10));HistoryStore.saveOutcome("recorded",completed());for(int i=0;i<210;i++)HistoryStore.add(row("new"+i,"g"+i,15000,30,10));JSONArray rows=HistoryStore.snapshot();assertEquals(200,rows.length());assertEquals("recorded",rows.getJSONObject(0).getString("local_id"));assertEquals(1,TripJournal.stats(rows).completed);}
    @Test public void oldPhevProfileKeepsAllValuesButRequiresSpecificReviews()throws Exception{DriverConfig c=new DriverConfig();c.vehicle="Captiva PHEV";c.energyMode="PHEV";c.electricityPrice=0;c.fuelPrice=28;c.remainingElectricKm=74;JSONObject old=ConfigStore.json(c);for(String key:new String[]{"vehicleConfirmed","energyReviewed","costsReviewed","goalsReviewed","rangeUpdatedAtMs","returnScenarioEnabled","returnScenarioKm","returnScenarioMin","pickupShareAlertPercent"})old.remove(key);old.put("calibrated",true);context.getSharedPreferences("driver_config_v2",0).edit().putString("profile",old.toString()).commit();DriverConfig loaded=ConfigStore.load(context);assertEquals("PHEV",loaded.energyMode);assertEquals("Captiva PHEV",loaded.vehicle);assertEquals(28,loaded.fuelPrice,0);assertEquals(74,loaded.remainingElectricKm,0);assertEquals(0,loaded.electricityPrice,0);assertFalse(loaded.costsReviewed);assertTrue(loaded.rangeStale(System.currentTimeMillis()));assertEquals(4,loaded.pendingProfile().size());}
    private View find(View v,String text){if(v instanceof TextView && text.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),text);if(found!=null)return found;}return null;}
    private <T extends View> T described(View v,String label,Class<T> type){if(type.isInstance(v) && label.contentEquals(v.getContentDescription()))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T found=described(((ViewGroup)v).getChildAt(i),label,type);if(found!=null)return found;}return null;}
    @Test public void actualResultFormStartsBlankAndSavesOnlySelectedVersion()throws Exception{
        HistoryStore.add(row("a","g",20000,30,10));
        Activity a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,ReviewActivity.class).putExtra("section","results").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        main(()->{find(a.getWindow().getDecorView(),"Ver versiones y registrar resultado").performClick();find(a.getWindow().getDecorView(),"Registrar resultado de esta versión").performClick();});
        main(()->{View root=a.getWindow().getDecorView();assertEquals("",described(root,"Recibido real, MXN (obligatorio)",EditText.class).getText().toString());described(root,"Resultado informado",Spinner.class).setSelection(TripOutcome.State.COMPLETED.ordinal());});
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        main(()->{View root=a.getWindow().getDecorView();described(root,"Recibido real, MXN (obligatorio)",EditText.class).setText("180.00");described(root,"Tiempo total real, min (obligatorio)",EditText.class).setText("40");described(root,"Distancia total real, km (obligatorio)",EditText.class).setText("12");find(root,"Guardar resultado informado").performClick();});
        for(int i=0;i<80 && !HistoryStore.snapshot().getJSONObject(0).has("actual_result");i++)Thread.sleep(25);
        TripOutcome r=TripJournal.parse(HistoryStore.snapshot().getJSONObject(0).getJSONObject("actual_result"));assertEquals(18000L,(long)r.settledCents);assertNull(r.actualMargin());main(a::finish);
    }
}
