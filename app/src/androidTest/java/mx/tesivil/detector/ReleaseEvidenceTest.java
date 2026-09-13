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
        assertEquals(90,ConfigStore.zones(c).size());
        JSONObject marker=new JSONObject();
        try { marker.put("fuel",ConfigStore.load(c).fuelPrice); marker.put("zones",ConfigStore.zonesJson(ConfigStore.zones(c))); }
        catch(Exception e) { throw new AssertionError(e); }
        // The application never writes this test-only baseline.
        assertTrue(c.getSharedPreferences("update_evidence",0).edit().putString("before",marker.toString()).commit());
    }
    @Test public void updatePreservesProfileAndZoneRules() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject before=new JSONObject(c.getSharedPreferences("update_evidence",0).getString("before","{}"));
        assertEquals(before.getDouble("fuel"),ConfigStore.load(c).fuelPrice,0);
        assertEquals(before.getJSONArray("zones").toString(),ConfigStore.zonesJson(ConfigStore.zones(c)).toString());
    }
}
