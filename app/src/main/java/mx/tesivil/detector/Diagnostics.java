package mx.tesivil.detector;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Main-thread UI state. Endpoint text exists only in RAM; the journal is numeric. */
final class Diagnostics {
    static boolean running;
    static String message="Listo para evaluar",source="Sin lectura";
    static OfferParser.Offer offer;
    static ScoreEngine.Evaluation evaluation;
    static DriverConfig config=new DriverConfig();
    static List<ZoneRule> zones=new ArrayList<>();
    static long frames,validFrames,failedFrames,lastOcrMs,lastDecisionMs;
    static final List<Runnable> listeners=new ArrayList<>();
    private static final OfferConfirmation confirmation=new OfferConfirmation();
    private static Context context;
    private static String lastHistoryKey,session=UUID.randomUUID().toString();
    private static long lastValidAt;
    static void init(Context c) { context=c.getApplicationContext();HistoryStore.init(context);reloadConfig(); }
    static void reloadConfig() { if(context!=null){zones=ConfigStore.zones(context);config=ConfigStore.load(context);} }
    static void notifyUi() { for(Runnable r:new ArrayList<>(listeners))r.run(); }
    static void status(String s){message=s;notifyUi();}
    static void clear(String s){offer=null;evaluation=null;confirmation.reset();message=s;notifyUi();}
    static void begin(){
        reloadConfig();running=true;frames=validFrames=failedFrames=lastOcrMs=lastDecisionMs=0;
        offer=null;evaluation=null;confirmation.reset();lastHistoryKey=null;lastValidAt=0;session=UUID.randomUUID().toString();
        source="Captura en vivo";status("Iniciando captura…");
    }
    static void reading(OfferParser.Result result,long ms,String from){
        long now=SystemClock.elapsedRealtime();lastOcrMs=ms;source=from;frames++;offer=result.offer;evaluation=null;
        if(offer==null){failedFrames++;confirmation.reset();message=result.reason;notifyUi();return;}
        validFrames++;
        if(now-lastValidAt>1800)lastHistoryKey=null;
        lastValidAt=now;
        boolean confirmed=!from.equals("Captura en vivo") || confirmation.observe(offer,now);
        if(!confirmed){message="Confirmando datos de la oferta…";notifyUi();return;}
        long started=SystemClock.elapsedRealtime();
        evaluation=ScoreEngine.evaluate(offer,config,zones,LocalDate.now(),LocalTime.now().getHour());
        lastDecisionMs=SystemClock.elapsedRealtime()-started;message=evaluation.label()+" · "+evaluation.score+"/100";
        String key=from+"|"+config.revision+"|"+offer.key()+"|"+ZoneRule.norm(offer.pickupAddress)+"|"+ZoneRule.norm(offer.destinationAddress);
        if(!key.equals(lastHistoryKey) && context!=null){
            lastHistoryKey=key;
            try{
                JSONObject row=offerJson(offer);row.put("local_id",UUID.randomUUID().toString());row.put("session",session);
                row.put("observed_at_ms",System.currentTimeMillis());row.put("source",from);row.put("outcome","unknown");
                row.put("ocr_ms",ms);row.put("decision_ms",lastDecisionMs);row.put("config",ConfigStore.json(config));
                row.put("zone_rules",ConfigStore.zonesJson(zones));
                row.put("evaluation",evaluationJson(evaluation));HistoryStore.add(row);
            }catch(JSONException e){HistoryStore.storageError="No se pudo registrar la evaluación";}
        }
        notifyUi();
    }
    static JSONObject offerJson(OfferParser.Offer o)throws JSONException{
        JSONObject j=new JSONObject();j.put("fare_cents",o.cents);j.put("currency_assumed","MXN");
        j.put("reader_version","0.3.3");j.put("service_type",o.serviceType.name());j.put("service_label",o.typeLabel());j.put("exclusive",o.exclusive);
        j.put("pickup_minutes",o.pickupMinutes);j.put("pickup_km",o.pickupKm);j.put("trip_minutes",o.tripMinutes);j.put("trip_km",o.tripKm);
        if(o.displayedRate!=null)j.put("uber_displayed_rate",o.displayedRate);
        JSONObject rider=new JSONObject();rider.put("rating",o.rider.rating==null?JSONObject.NULL:o.rider.rating);
        rider.put("visible_count",o.rider.count==null?JSONObject.NULL:o.rider.count);rider.put("explicit_new",o.rider.explicitNew);
        rider.put("ambiguous",o.rider.ambiguous);rider.put("count_source",o.rider.countSource);j.put("rider",rider);return j;
    }
    static JSONObject evaluationJson(ScoreEngine.Evaluation e)throws JSONException{
        JSONObject j=new JSONObject();j.put("color",e.color.name());j.put("score",e.score);j.put("economy_score",e.economyScore);
        j.put("reasons",new JSONArray(e.reasons));j.put("km",e.km);j.put("minutes",e.minutes);j.put("revenue",e.revenue);
        j.put("energy_cost",e.energyCost);j.put("upkeep_cost",e.upkeepCost);j.put("allocated_fixed_cost",e.fixedCost);j.put("extras",e.extraCost);
        j.put("estimated_remaining",e.margin);j.put("hourly",e.hourly);j.put("per_km",e.perKm);j.put("conservative_hourly",e.conservativeHourly);
        j.put("hourly_points",e.timePoints);j.put("km_points",e.kmPoints);j.put("pickup_points",e.pickupPoints);
        j.put("rider_blocked",e.riderBlocked);j.put("rider_caution",e.riderCaution);j.put("rider_unknown",e.riderUnknown);
        if(e.rider!=null)j.put("rider_summary",e.rider.summary());
        if(e.zones!=null){
            j.put("zone_blocked",e.zones.blocked);j.put("zone_caution",e.zones.caution);j.put("zone_unknown",e.zones.unknown);
            j.put("pickup_zone_status",e.zones.pickupStatus);j.put("destination_zone_status",e.zones.destinationStatus);
        }
        return j;
    }
    static String export(Context c)throws JSONException{
        JSONObject j=new JSONObject();j.put("schema",5);j.put("app_version","0.3.3");j.put("model",Build.MANUFACTURER+" "+Build.MODEL);
        j.put("android",Build.VERSION.RELEASE);j.put("sdk",Build.VERSION.SDK_INT);
        try{PackageInfo p=c.getPackageManager().getPackageInfo("com.ubercab.driver",0);j.put("uber_version",p.versionName);j.put("uber_version_code",p.getLongVersionCode());}
        catch(Exception e){j.put("uber_version","not_available");}
        j.put("ocr_frames",frames);j.put("complete_frames",validFrames);j.put("incomplete_frames",failedFrames);j.put("last_ocr_ms",lastOcrMs);
        j.put("configuration",ConfigStore.json(config));j.put("history",HistoryStore.snapshot());j.put("storage_error",HistoryStore.storageError);
        j.put("note","Estimated offer scores, not realized income or crime probabilities. Local offer identity is heuristic. No raw OCR or endpoint addresses. OCR timing is not end-to-end detection latency.");
        return j.toString(2);
    }
    private Diagnostics(){}
}
