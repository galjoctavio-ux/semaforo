package mx.tesivil.detector;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import static org.junit.Assert.*;

/** Explicitly selected on the owned emulator; excluded from the normal suite. */
@RunWith(AndroidJUnit4.class)
public class ReleaseEvidenceTest {
    @Test public void recordLocalProfileAndJournal() throws Exception {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()-> {
            Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Diagnostics.init(c);
            try {
                JSONObject meta=new JSONObject(); meta.put("version",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName);
                meta.put("fuel",ConfigStore.load(c).fuelPrice);meta.put("zone_count",ConfigStore.zones(c).size());
                android.util.Log.i("ReleaseEvidence","META "+meta);
                org.json.JSONArray rows=HistoryStore.snapshot();
                for(int i=0;i<rows.length();i++) {
                    JSONObject row=rows.getJSONObject(i); row.remove("zone_rules");row.remove("config");
                    android.util.Log.i("ReleaseEvidence","ROW "+row);
                }
            }
            catch(Exception e) { throw new AssertionError(e); }
        });
    }
    @Test public void seedUpdateProfile() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("mx.tesivil.detector",c.getPackageName());
        DriverConfig config=ConfigStore.load(c); config.fuelPrice=28;
        ConfigStore.save(c,config);
        JSONObject marker=new JSONObject();
        try { marker.put("fuel",ConfigStore.load(c).fuelPrice); marker.put("profile",ConfigStore.json(ConfigStore.load(c))); marker.put("zones",ConfigStore.zonesJson(ConfigStore.zones(c))); }
        catch(Exception e) { throw new AssertionError(e); }
        // The application never writes this test-only baseline.
        assertTrue(c.getSharedPreferences("update_evidence",0).edit().putString("before",marker.toString()).commit());
    }
    @Test public void updatePreservesProfileAndZoneRules() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject before=new JSONObject(c.getSharedPreferences("update_evidence",0).getString("before","{}"));
        assertEquals(before.getDouble("fuel"),ConfigStore.load(c).fuelPrice,0);
        assertEquals(before.getJSONArray("zones").toString(),ConfigStore.zonesJson(ConfigStore.zones(c)).toString());
        if(before.has("profile")){
            JSONObject expected=before.getJSONObject("profile"),actual=ConfigStore.json(ConfigStore.load(c));
            java.util.Iterator<String> keys=expected.keys();
            while(keys.hasNext()){
                String key=keys.next();Object value=expected.get(key);
                if(value instanceof Number)assertEquals(key,((Number)value).doubleValue(),actual.getDouble(key),0);
                else assertEquals(key,value,actual.get(key));
            }
            if(!expected.has("evaluationBasis")){
                assertEquals("TOTAL",actual.getString("evaluationBasis"));assertEquals(100,actual.getDouble("uberUsePercent"),0);
            }
        }
    }
    @Test public void seedSolarOnboardingUpdateProfile() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        ConfigStore.zones(c);ConfigStore.saveZones(c,java.util.List.of());
        // Uses the v1 method signature and fields, so it executes on the old 0.5.0 APK.
        DriverConfig profile=OnboardingProfiles.build(OnboardingProfiles.Vehicle.PHEV,OnboardingProfiles.Ownership.OWNED,
                0,10,false,OnboardingProfiles.Charging.FREE,74,OnboardingProfiles.Strategy.BALANCED,ConfigStore.load(c),System.currentTimeMillis());
        profile.vehicle="Captiva PHEV · prueba de actualización";profile.fuelPrice=28;ConfigStore.save(c,profile);
        JSONObject marker=new JSONObject();marker.put("fuel",ConfigStore.load(c).fuelPrice);
        marker.put("profile",ConfigStore.json(ConfigStore.load(c)));marker.put("zones",ConfigStore.zonesJson(ConfigStore.zones(c)));
        assertTrue(c.getSharedPreferences("update_evidence",0).edit().putString("before",marker.toString()).commit());
    }
}
