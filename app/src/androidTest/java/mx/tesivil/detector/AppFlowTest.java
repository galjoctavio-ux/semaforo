package mx.tesivil.detector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Run only on the isolated emulator: these tests reset the app's test profile. */
@RunWith(AndroidJUnit4.class)
public class AppFlowTest {
    private Context context;
    private static final String SAMPLE="UberX\n$98.31\n4.92 (149)\nA 14 min (6.3 km)\nCalle PrivadaUnica 123, Colonia Ejemplo, Zapopan\nViaje: 18 min (4.0 km)\nCalle SecretaUnica 456, Otra Colonia, Zapopan\nViaje disponible";
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void reset(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("driver_config_v2",Context.MODE_PRIVATE).edit().clear().commit();
        main(()->{Diagnostics.init(context);HistoryStore.clear();Diagnostics.begin();Diagnostics.running=false;});
    }
    private Activity settings(){
        return InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,SettingsActivity.class).putExtra("section","vehicle").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    private View find(View v,String label){
        if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof TextView && label.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=find(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    @Test public void settingsFormSavesEditableDefaultsAndCalibration(){
        Activity a=settings();
        main(()->{
            View root=a.getWindow().getDecorView();
            View price=find(root,"Gasolina, MXN por litro");
            // The label and field share a caption; prefer the content-described input.
            EditText input=findInput(root,"Gasolina, MXN por litro");assertNotNull(input);input.setText("27.5");
            ((CheckBox)find(root,"He revisado mis costos y objetivos")).setChecked(true);
            find(root,"Guardar configuración").performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        DriverConfig loaded=ConfigStore.load(context);assertEquals(27.5,loaded.fuelPrice,0);assertTrue(loaded.calibrated);assertTrue(loaded.revision>1);
        main(a::finish);
    }
    private EditText findInput(View v,String label){
        if(v instanceof EditText && label.contentEquals(v.getContentDescription()))return (EditText)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){EditText e=findInput(((ViewGroup)v).getChildAt(i),label);if(e!=null)return e;}
        return null;
    }
    @Test public void invalidProfileCannotOverwriteSavedValues(){
        DriverConfig good=ConfigStore.load(context);good.fuelPrice=28;ConfigStore.save(context,good);
        DriverConfig bad=ConfigStore.load(context);bad.kmPerLiter=0;
        try{ConfigStore.save(context,bad);fail("Invalid profile saved");}catch(IllegalArgumentException expected){}
        assertEquals(12,ConfigStore.load(context).kmPerLiter,0);assertEquals(28,ConfigStore.load(context).fuelPrice,0);
    }
    @Test public void riderSettingsSaveAndSurviveReload(){
        Activity a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,SettingsActivity.class).putExtra("section","rider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        main(()->{View root=a.getWindow().getDecorView();findInput(root,"Nuevo si el contador es menor que (entero)").setText("10");((CheckBox)find(root,"Usuario NUEVO genera rojo automático")).setChecked(false);find(root,"Guardar configuración").performClick();});
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();DriverConfig c=ConfigStore.load(context);assertEquals(10,c.newRiderMinCount,0);assertFalse(c.blockNewRider);assertTrue(c.riderFilter);main(a::finish);
    }
    @Test public void riderChangesCreateTheirOwnHistoryAndUnknownRemainsNull()throws Exception{
        main(()->{Diagnostics.reading(OfferParser.parse(SAMPLE),140,"Captura seleccionada");Diagnostics.reading(OfferParser.parse(SAMPLE.replace("4.92 (149)","Nuevo")),140,"Captura seleccionada");});
        JSONArray rows=HistoryStore.snapshot();assertEquals(2,rows.length());assertEquals(149,rows.getJSONObject(0).getJSONObject("rider").getInt("visible_count"));assertTrue(rows.getJSONObject(1).getJSONObject("rider").isNull("visible_count"));assertEquals("ROJO",rows.getJSONObject(1).getJSONObject("evaluation").getString("color"));
        assertEquals("0.3.4",new org.json.JSONObject(Diagnostics.export(context)).getString("app_version"));
        assertEquals("UBER_X",rows.getJSONObject(0).getString("service_type"));assertFalse(rows.getJSONObject(0).getBoolean("exclusive"));
        assertEquals("0.3.4",rows.getJSONObject(0).getString("reader_version"));
    }
    @Test public void changedXlFareIsConfirmedAndJournaledSeparately()throws Exception{
        String original=SAMPLE.replace("UberX","UberXL Exclusivo").replace("98.31","284.93");
        main(()->Diagnostics.reading(OfferParser.parse(original),140,"Captura en vivo"));
        Thread.sleep(25);main(()->Diagnostics.reading(OfferParser.parse(original),140,"Captura en vivo"));
        assertNotNull(Diagnostics.evaluation);double firstMargin=Diagnostics.evaluation.margin;
        String changed=original.replace("284.93","236.82");
        main(()->Diagnostics.reading(OfferParser.parse(changed),140,"Captura en vivo"));assertNull(Diagnostics.evaluation);
        Thread.sleep(25);main(()->Diagnostics.reading(OfferParser.parse(changed),140,"Captura en vivo"));
        assertNotNull(Diagnostics.evaluation);assertEquals(48.11,firstMargin-Diagnostics.evaluation.margin,.001);
        JSONArray rows=HistoryStore.snapshot();assertEquals(2,rows.length());
        assertEquals(28493,rows.getJSONObject(0).getLong("fare_cents"));assertEquals(23682,rows.getJSONObject(1).getLong("fare_cents"));
        assertEquals("UBER_XL",rows.getJSONObject(1).getString("service_type"));assertTrue(rows.getJSONObject(1).getBoolean("exclusive"));
    }
    @Test public void comfortHoursAndDestinationNoticeAreRecordedWithoutStreetText() throws Exception {
        String card=SAMPLE.replace("UberX","Comfort Exclusivo").replace("18 min","1 h 3 min")
                .replace("Calle PrivadaUnica 123, Colonia Ejemplo, Zapopan","Calle PrivadaUnica 123, Colonia Ejemplo, Zapopan\n1 destino");
        main(()->Diagnostics.reading(OfferParser.parse(card),140,"Captura seleccionada"));
        JSONObject row=HistoryStore.snapshot().getJSONObject(0);
        assertEquals("COMFORT",row.getString("service_type"));assertTrue(row.getBoolean("exclusive"));
        assertEquals(63,row.getInt("trip_minutes"));assertEquals(1,row.getInt("destination_notice_count"));
        assertFalse(row.toString().contains("PrivadaUnica"));
        assertNotEquals("VERDE",row.getJSONObject("evaluation").getString("color"));
        assertTrue(row.getJSONObject("evaluation").getJSONArray("reasons").toString().contains("Aviso de destinos"));
    }
    @Test public void liveReadingsRequireConfirmationAndInvalidReadClearsScore()throws Exception{
        main(()->Diagnostics.reading(OfferParser.parse(SAMPLE),140,"Captura en vivo"));assertNull(Diagnostics.evaluation);
        Thread.sleep(25);main(()->Diagnostics.reading(OfferParser.parse(SAMPLE),135,"Captura en vivo"));assertNotNull(Diagnostics.evaluation);
        main(()->Diagnostics.reading(OfferParser.parse("Sin oferta"),20,"Captura en vivo"));assertNull(Diagnostics.evaluation);assertNull(Diagnostics.offer);
    }
    @Test public void historyIsPersistentBoundedToOfferAndDoesNotContainStreetText()throws Exception{
        main(()->Diagnostics.reading(OfferParser.parse(SAMPLE),140,"Captura seleccionada"));
        main(()->Diagnostics.reading(OfferParser.parse(SAMPLE),130,"Captura seleccionada"));
        JSONArray rows=HistoryStore.snapshot();assertEquals(1,rows.length());
        assertEquals(9831,rows.getJSONObject(0).getLong("fare_cents"));assertTrue(rows.getJSONObject(0).has("config"));assertTrue(rows.getJSONObject(0).has("zone_rules"));
        String json=rows.toString().toLowerCase();assertFalse(json.contains("privadaunica"));assertFalse(json.contains("secretaunica"));
        File path=new File(context.getFilesDir(),"offer-history-v2.json");String stored="";
        for(int i=0;i<60;i++){
            if(path.exists())try{stored=new String(java.nio.file.Files.readAllBytes(path.toPath()),StandardCharsets.UTF_8);if(new JSONArray(stored).length()==1)break;}catch(Exception ignored){}
            Thread.sleep(25);
        }
        assertEquals(1,new JSONArray(stored).length());assertFalse(stored.toLowerCase().contains("privadaunica"));
    }
    @Test public void zoneRulesPersistWithAtomicProfileRevision(){
        ZoneRule r=new ZoneRule();r.neighborhood="Ejemplo de prueba";r.municipality="Zapopan";
        long before=ConfigStore.load(context).revision;ConfigStore.saveZones(context,java.util.List.of(r));
        assertEquals(1,ConfigStore.zones(context).size());assertEquals("Ejemplo de prueba",ConfigStore.zones(context).get(0).neighborhood);
        assertTrue(ConfigStore.load(context).revision>before);
    }
}
