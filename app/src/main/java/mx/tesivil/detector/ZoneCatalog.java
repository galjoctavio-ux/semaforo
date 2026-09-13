package mx.tesivil.detector;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Historical counts are facts from the source; the initial caution action is ours. */
final class ZoneCatalog {
    static List<ZoneRule> load(Context context) throws Exception {
        JSONObject catalog;
        try (java.io.InputStream in=context.getAssets().open("zmg_historical_zones.json")) {
            catalog=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
        JSONArray entries=catalog.getJSONArray("entries");
        List<ZoneRule> result=new ArrayList<>();
        for(int i=0;i<entries.length();i++) {
            JSONObject e=entries.getJSONObject(i);ZoneRule r=new ZoneRule();
            r.neighborhood=e.getString("neighborhood");r.municipality=e.getString("municipality");
            r.catalogId=e.getString("id");r.source="Base histórica Fiscalía / IIEG · nivel inicial de la app";
            r.action=ZoneRule.Action.PRECAUCION;r.reviewedOn=catalog.getString("periodEnd");r.noExpiry=true;
            r.officialUrl=catalog.getString("sourceUrl");
            r.officialEvidence="Fiscalía del Estado de Jalisco / IIEG\nPeriodo: enero–octubre de 2021"
                +"\nColonia en la fuente: "+e.getString("sourceNeighborhood")+"\nMunicipio: "+r.municipality
                +"\nRobo a persona: "+e.getInt("personRobberies")+"\nRobo a vehículo particular: "+e.getInt("vehicleRobberies")
                +"\nTotal de ambos delitos: "+e.getInt("total")
                +"\nSelección de la app: hasta 10 colonias por municipio con al menos 5 registros. Cantidades absolutas, sin ajustar por población o tránsito."
                +"\nEl nivel es editable; la fuente no asigna peligrosidad a conductores. Datos históricos, sin actualización automática.";
            if(r.validate()!=null)throw new IllegalArgumentException(r.validate());
            result.add(r);
        }
        return result;
    }
    static List<ZoneRule> mergeKeepingUserRules(List<ZoneRule> existing,List<ZoneRule> defaults) {
        List<ZoneRule> merged=new ArrayList<>(existing);
        for(ZoneRule r:defaults) if(merged.stream().noneMatch(old->ZoneRule.samePlace(old,r))) merged.add(r);
        return merged;
    }
    static List<ZoneRule> supplement(Context context) throws Exception {
        JSONObject catalog;
        try(java.io.InputStream in=context.getAssets().open("zmg_zone_supplement.json")) {
            catalog=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
        List<ZoneRule> out=new ArrayList<>();JSONArray entries=catalog.getJSONArray("entries");
        for(int i=0;i<entries.length();i++) {
            JSONObject j=entries.getJSONObject(i);ZoneRule r=new ZoneRule();
            r.neighborhood=j.getString("neighborhood");r.municipality=j.getString("municipality");r.catalogId=j.getString("id");
            r.source="Catálogo de antecedentes · nivel inicial de la app";r.reviewedOn=catalog.getString("consultedOn");r.noExpiry=true;r.action=ZoneRule.Action.PRECAUCION;
            JSONArray refs=j.getJSONArray("references");StringBuilder evidence=new StringBuilder();
            for(int n=0;n<refs.length();n++) {
                JSONObject ref=refs.getJSONObject(n);evidence.append(n==0?"":"\n\n").append(ref.getString("kind")).append(" · ").append(ref.getString("publisher"))
                    .append("\n").append(ref.getString("title")).append("\nPeriodo / fecha: ").append(ref.getString("period"))
                    .append("\nHallazgo: ").append(ref.getString("finding")).append("\n").append(ref.getString("url"));
            }
            evidence.append("\n\nConsultado: ").append(catalog.getString("consultedOn"))
                .append(". Cada referencia se conserva por separado; no se suman periodos. Precaución es una decisión de la app, editable. No es una probabilidad de delito ni una certificación de inseguridad actual.");
            r.supplementEvidence=evidence.toString();if(r.validate()!=null)throw new IllegalArgumentException(r.validate());out.add(r);
        }
        return out;
    }
    static List<ZoneRule> mergeSupplement(List<ZoneRule> existing,List<ZoneRule> supplement,List<ZoneRule> originalDefaults) {
        List<ZoneRule> out=new ArrayList<>(existing);
        for(ZoneRule next:supplement) {
            ZoneRule old=out.stream().filter(r->ZoneRule.samePlace(r,next)).findFirst().orElse(null);
            if(old!=null) {
                // An added reference never changes the user's level, location, hours or expiry.
                old.supplementEvidence=next.supplementEvidence;
            } else if(originalDefaults.stream().noneMatch(r->ZoneRule.samePlace(r,next))) out.add(next);
            // Missing original places are deliberate deletions, not missing defaults to restore.
        }
        return out;
    }
}
