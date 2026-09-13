package mx.tesivil.detector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Only for the isolated emulator: resets this app's test settings. */
@RunWith(AndroidJUnit4.class)
public class ZoneCatalogFlowTest {
    @Test public void oldEmptyCatalogStaysEmptyAfterSupplementUpgrade(){context.getSharedPreferences("driver_config_v2",0).edit().putString("zones","[]").putBoolean("zmg_historical_catalog_installed",true).commit();assertTrue(ConfigStore.zones(context).isEmpty());assertTrue(ConfigStore.zones(context).isEmpty());}
    private Context context;
    @Before public void reset(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();}
    @Test public void bundledFactsAndDefaultActionAreLoadedOnce(){
        List<ZoneRule> rules=ConfigStore.zones(context);assertEquals(90,rules.size());
        assertEquals(9,rules.stream().map(r->r.municipality).distinct().count());
        ZoneRule centro=rules.stream().filter(r->r.neighborhood.equals("Centro") && r.municipality.equals("Guadalajara")).findFirst().get();
        assertTrue(centro.officialEvidence.contains("Robo a persona: 321"));assertTrue(centro.officialEvidence.contains("Robo a vehículo particular: 30"));
        assertEquals("2021-10-31",centro.reviewedOn);assertTrue(centro.current(LocalDate.now()));
        for(ZoneRule r:rules){assertEquals(ZoneRule.Action.PRECAUCION,r.action);assertTrue(r.noExpiry);assertFalse(r.catalogId.isEmpty());}
        assertEquals(1,rules.stream().filter(r->r.referenceKind().equals("Reporte comunitario sin corroborar")).count());
        assertTrue(rules.stream().filter(r->r.neighborhood.equals("Loma Dorada")).findFirst().get().supplementEvidence.contains("EI-2"));
        long revision=ConfigStore.load(context).revision;assertEquals(90,ConfigStore.zones(context).size());assertEquals(revision,ConfigStore.load(context).revision);
    }
    @Test public void upgradeKeepsExistingRulesCostsAndDisabledFilter() throws Exception {
        ZoneRule own=new ZoneRule();own.neighborhood="Arboledas";own.municipality="Zapopan";own.action=ZoneRule.Action.EVITAR;
        ZoneRule extra=new ZoneRule();extra.neighborhood="Mi colonia de prueba";
        DriverConfig profile=new DriverConfig();profile.zoneFilter=false;profile.fuelPrice=28;profile.revision=17;
        // Exact old-format keys: no catalog installation marker, like an update from 0.2.0.
        context.getSharedPreferences("driver_config_v2",0).edit().putString("zones",ConfigStore.zonesJson(List.of(own,extra)).toString())
            .putString("profile",ConfigStore.json(profile).toString()).commit();
        List<ZoneRule> loaded=ConfigStore.zones(context);assertEquals(91,loaded.size());
        assertEquals(1,loaded.stream().filter(r->r.neighborhood.equals("Arboledas")).count());
        assertEquals(ZoneRule.Action.EVITAR,loaded.get(0).action);assertTrue(loaded.get(0).catalogId.isEmpty());
        assertFalse(loaded.get(0).supplementEvidence.isEmpty());
        assertEquals(28,ConfigStore.load(context).fuelPrice,0);assertFalse(ConfigStore.load(context).zoneFilter);assertEquals(18,ConfigStore.load(context).revision);
    }
    @Test public void deletedDefaultsAndEmptyListDoNotReappear(){
        List<ZoneRule> rules=ConfigStore.zones(context);String id=rules.remove(0).catalogId;ConfigStore.saveZones(context,rules);
        assertEquals(89,ConfigStore.zones(context).size());assertTrue(ConfigStore.zones(context).stream().noneMatch(r->r.catalogId.equals(id)));
        ConfigStore.saveZones(context,List.of());assertTrue(ConfigStore.zones(context).isEmpty());assertTrue(ConfigStore.zones(context).isEmpty());
    }
    @Test public void userLevelOverridesHistoryAndKeepsProvenance(){
        List<ZoneRule> rules=ConfigStore.zones(context);ZoneRule r=rules.get(0);String evidence=r.officialEvidence;
        r.action=ZoneRule.Action.EVITAR;ConfigStore.saveZones(context,rules);r=ConfigStore.zones(context).get(0);
        OfferParser.Offer o=new OfferParser.Offer(100000,1,1,10,5,null,r.neighborhood+", "+r.municipality,r.neighborhood+", "+r.municipality);
        DriverConfig c=new DriverConfig();c.calibrated=true;c.vehicleConfirmed=c.energyReviewed=c.costsReviewed=c.goalsReviewed=true;c.riderFilter=false;
        assertEquals(ScoreEngine.Color.ROJO,ScoreEngine.evaluate(o,c,List.of(r),LocalDate.now(),12).color);
        r.action=ZoneRule.Action.REVISADA;ConfigStore.saveZones(context,List.of(r));r=ConfigStore.zones(context).get(0);
        assertEquals(evidence,r.officialEvidence);assertEquals(ScoreEngine.Color.VERDE,ScoreEngine.evaluate(o,c,List.of(r),LocalDate.now(),12).color);
        assertTrue(ConfigStore.zonesJson(List.of(r)).toString().contains("2021"));
    }
    @Test public void supplementUpgradeKeepsDeletedOriginalsAndUserAdjustedLevels() throws Exception {
        List<ZoneRule> old=new ArrayList<>(ZoneCatalog.load(context));
        old.removeIf(r->r.neighborhood.equals("Arboledas"));ZoneRule calm=old.stream().filter(r->r.neighborhood.equals("La Calma")).findFirst().get();
        calm.action=ZoneRule.Action.REVISADA;calm.noExpiry=false;calm.reviewedOn="2021-01-01";calm.startHour=22;calm.endHour=6;
        DriverConfig profile=new DriverConfig();profile.fuelPrice=28;profile.revision=17;
        context.getSharedPreferences("driver_config_v2",0).edit().putString("zones",ConfigStore.zonesJson(old).toString()).putString("profile",ConfigStore.json(profile).toString()).putBoolean("zmg_historical_catalog_installed",true).commit();
        List<ZoneRule> current=ConfigStore.zones(context);assertEquals(89,current.size());assertTrue(current.stream().noneMatch(r->r.neighborhood.equals("Arboledas")));
        ZoneRule kept=current.stream().filter(r->r.neighborhood.equals("La Calma")).findFirst().get();assertEquals(ZoneRule.Action.REVISADA,kept.action);assertEquals(22,kept.startHour);assertFalse(kept.noExpiry);assertEquals("2021-01-01",kept.reviewedOn);assertFalse(kept.supplementEvidence.isEmpty());
        assertEquals(28,ConfigStore.load(context).fuelPrice,0);assertEquals(18,ConfigStore.load(context).revision);assertTrue(ConfigStore.load(context).riderFilter);
    }
    @Test public void malformedExistingDataIsNotOverwrittenByMigration(){
        context.getSharedPreferences("driver_config_v2",0).edit().putString("zones","broken-json").commit();
        assertTrue(ConfigStore.zones(context).isEmpty());assertFalse(ConfigStore.zonesError.isEmpty());
        assertEquals("broken-json",context.getSharedPreferences("driver_config_v2",0).getString("zones",""));
    }
    private View find(View root,String text){
        if(root instanceof TextView && text.equalsIgnoreCase(((TextView)root).getText().toString()))return root;
        if(text.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View found=find(((ViewGroup)root).getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    @Test public void editorSavesLevelWithoutErasingOfficialEvidence(){
        Activity a=InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,SettingsActivity.class).putExtra("section","zones").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->find(a.getWindow().getDecorView(),"Centro · Medio · Precaución\nGuadalajara").performClick());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            View dialog=null;
            for(View root:android.view.inspector.WindowInspector.getGlobalWindowViews())if(find(root,"Nivel y acción")!=null)dialog=root;
            assertNotNull(dialog);Spinner spinner=findSpinner(dialog);assertNotNull(spinner);spinner.setSelection(0);find(dialog,"Guardar").performClick();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();ZoneRule saved=ConfigStore.zones(context).get(0);
        assertEquals(ZoneRule.Action.EVITAR,saved.action);assertFalse(saved.officialEvidence.isEmpty());assertTrue(saved.noExpiry);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(a::finish);
    }
    private Spinner findSpinner(View root){if(root instanceof Spinner)return (Spinner)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){Spinner s=findSpinner(((ViewGroup)root).getChildAt(i));if(s!=null)return s;}return null;}
}
