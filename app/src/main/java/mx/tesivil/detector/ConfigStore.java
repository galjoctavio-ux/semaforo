package mx.tesivil.detector;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

final class ConfigStore {
    static String zonesError="";
    private static final String CATALOG_INSTALLED="zmg_historical_catalog_installed";
    private static final String SUPPLEMENT_INSTALLED="zmg_supplement_030_installed";
    private static android.content.SharedPreferences prefs(Context c) { return c.getSharedPreferences("driver_config_v2", Context.MODE_PRIVATE); }
    static boolean needsOnboarding(Context c) {
        return !prefs(c).contains("profile") && !prefs(c).getBoolean("onboarding_entry_seen",false);
    }
    static void markOnboardingEntry(Context c) {
        if(!prefs(c).edit().putBoolean("onboarding_entry_seen",true).commit())throw new IllegalStateException("No se guardó el inicio del asistente");
    }
    static synchronized void saveOnboarding(Context c,DriverConfig config,long expectedRevision) {
        if(load(c).revision!=expectedRevision)throw new IllegalStateException("Tu configuración cambió mientras completabas el asistente. Ciérralo y ábrelo de nuevo para conservar esos cambios.");
        if(!config.onboardingCompleted || !config.presetAccepted)throw new IllegalArgumentException("Confirma el perfil antes de empezar");
        save(c,config);
    }
    static DriverConfig load(Context c) {
        DriverConfig config = new DriverConfig();
        try {
            JSONObject json = new JSONObject(prefs(c).getString("profile", "{}"));
            for (Field f : DriverConfig.class.getFields()) if (json.has(f.getName())) {
                if (f.getType() == double.class) f.setDouble(config, json.getDouble(f.getName()));
                else if (f.getType() == boolean.class) f.setBoolean(config, json.getBoolean(f.getName()));
                else if (f.getType() == long.class) f.setLong(config, json.getLong(f.getName()));
                else f.set(config, json.getString(f.getName()));
            }
            if (config.validate() != null) return new DriverConfig();
        } catch (Exception e) { return new DriverConfig(); }
        return config;
    }
    static JSONObject json(DriverConfig c) {
        JSONObject j = new JSONObject();
        try { for (Field f : DriverConfig.class.getFields()) j.put(f.getName(), f.get(c)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        return j;
    }
    static void save(Context c, DriverConfig config) {
        String error = config.validate(); if (error != null) throw new IllegalArgumentException(error);
        config.revision = load(c).revision + 1;
        if (!prefs(c).edit().putString("profile", json(config).toString()).commit()) throw new IllegalStateException("No se guardó el perfil");
    }
    static List<ZoneRule> zones(Context c) {
        try {
            ensureCatalog(c);List<ZoneRule> result=readZones(prefs(c).getString("zones","[]"));zonesError="";return result;
        } catch(Exception e) {zonesError="No se pudieron cargar las zonas; no se asumirá seguridad";return new ArrayList<>();}
    }
    private static synchronized void ensureCatalog(Context c) throws Exception {
        if(prefs(c).getBoolean(CATALOG_INSTALLED,false) && prefs(c).getBoolean(SUPPLEMENT_INSTALLED,false))return;
        List<ZoneRule> existing=readZones(prefs(c).getString("zones","[]"));
        List<ZoneRule> defaults=ZoneCatalog.load(c);
        List<ZoneRule> merged=prefs(c).getBoolean(CATALOG_INSTALLED,false)?existing:ZoneCatalog.mergeKeepingUserRules(existing,defaults);
        if(!prefs(c).getBoolean(SUPPLEMENT_INSTALLED,false) && !(prefs(c).getBoolean(CATALOG_INSTALLED,false) && existing.isEmpty()))
            merged=ZoneCatalog.mergeSupplement(merged,ZoneCatalog.supplement(c),defaults);
        DriverConfig profile=load(c);profile.revision++;
        // One atomic migration: never reload removed defaults on later launches.
        if(!prefs(c).edit().putString("zones",zonesJson(merged).toString()).putString("profile",json(profile).toString())
                .putBoolean(CATALOG_INSTALLED,true).putBoolean(SUPPLEMENT_INSTALLED,true).commit())throw new IllegalStateException("No se instaló la base histórica");
    }
    private static List<ZoneRule> readZones(String raw) throws Exception {
        List<ZoneRule> out = new ArrayList<>();
            JSONArray list = new JSONArray(raw);
            if(list.length()>300)throw new IllegalArgumentException("Máximo 300 reglas locales");
            for (int i=0;i<list.length();i++) {
                JSONObject j = list.getJSONObject(i); ZoneRule r = new ZoneRule();
                r.neighborhood = j.getString("neighborhood"); r.municipality = j.optString("municipality");
                r.source = j.getString("source"); r.reviewedOn = j.getString("reviewedOn");
                r.action = ZoneRule.Action.valueOf(j.getString("action"));
                r.pickup = j.getBoolean("pickup"); r.destination = j.getBoolean("destination");
                r.startHour = j.getInt("startHour"); r.endHour = j.getInt("endHour"); r.validDays = j.getInt("validDays");
                r.noExpiry=j.optBoolean("noExpiry",false);r.catalogId=j.optString("catalogId");
                r.officialEvidence=j.optString("officialEvidence");r.officialUrl=j.optString("officialUrl");
                r.supplementEvidence=j.optString("supplementEvidence");
                if(r.validate()!=null)throw new IllegalArgumentException(r.validate());out.add(r);
            }
        return out;
    }
    static JSONArray zonesJson(List<ZoneRule> rules) {
        if (rules.size() > 300) throw new IllegalArgumentException("Máximo 300 reglas locales");
        JSONArray array = new JSONArray();
        try {
            for (ZoneRule r : rules) {
                if (r.validate() != null) throw new IllegalArgumentException(r.validate());
                JSONObject j = new JSONObject();
                j.put("neighborhood",r.neighborhood); j.put("municipality",r.municipality); j.put("source",r.source);
                j.put("reviewedOn",r.reviewedOn); j.put("action",r.action.name());
                j.put("pickup",r.pickup); j.put("destination",r.destination);
                j.put("startHour",r.startHour); j.put("endHour",r.endHour); j.put("validDays",r.validDays);
                j.put("noExpiry",r.noExpiry);j.put("catalogId",r.catalogId);j.put("officialEvidence",r.officialEvidence);j.put("officialUrl",r.officialUrl);j.put("supplementEvidence",r.supplementEvidence);array.put(j);
            }
        } catch (org.json.JSONException e) { throw new IllegalStateException(e); }
        return array;
    }
    static void saveZones(Context c, List<ZoneRule> rules) {
        JSONArray array=zonesJson(rules);DriverConfig profile=load(c);profile.revision++;
        if (!prefs(c).edit().putString("zones",array.toString()).putString("profile",json(profile).toString()).putBoolean(CATALOG_INSTALLED,true).putBoolean(SUPPLEMENT_INSTALLED,true).commit())
            throw new IllegalStateException("No se guardaron las zonas");
    }
}
