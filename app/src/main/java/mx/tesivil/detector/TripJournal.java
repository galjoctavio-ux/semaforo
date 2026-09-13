package mx.tesivil.detector;

import org.json.*;
import java.util.*;

/** Summaries use explicit manual results, grouped by local offer episode, never OCR event counts. */
final class TripJournal {
    static JSONObject json(TripOutcome r) throws JSONException {
        JSONObject j=new JSONObject();j.put("state",r.state.name());j.put("reason",r.reason.name());
        j.put("reported_at_ms",System.currentTimeMillis());j.put("source","driver_reported");
        j.put("settled_cents",r.settledCents);j.put("total_minutes",r.totalMinutes);j.put("total_km",r.totalKm);
        j.put("wait_minutes",r.waitMinutes);j.put("reposition_minutes",r.repositionMinutes);j.put("reposition_km",r.repositionKm);
        j.put("energy_cents",r.energyCents);j.put("upkeep_cents",r.upkeepCents);j.put("fixed_cents",r.fixedCents);j.put("extras_cents",r.extrasCents);return j;
    }
    static TripOutcome parse(JSONObject j) throws Exception {
        TripOutcome r=new TripOutcome();r.state=TripOutcome.State.valueOf(j.getString("state"));r.reason=TripOutcome.Reason.valueOf(j.getString("reason"));
        r.settledCents=money(j,"settled_cents");r.energyCents=money(j,"energy_cents");r.upkeepCents=money(j,"upkeep_cents");r.fixedCents=money(j,"fixed_cents");r.extrasCents=money(j,"extras_cents");
        r.totalMinutes=number(j,"total_minutes");r.totalKm=number(j,"total_km");r.waitMinutes=number(j,"wait_minutes");r.repositionMinutes=number(j,"reposition_minutes");r.repositionKm=number(j,"reposition_km");
        String error=r.validate();if(error!=null)throw new IllegalArgumentException(error);return r;
    }
    private static Long money(JSONObject j,String key)throws JSONException{
        if(j.isNull(key))return null;Object value=j.get(key);
        if(!(value instanceof Number))throw new JSONException("Costo no numérico");
        try{return new java.math.BigDecimal(value.toString()).longValueExact();}catch(ArithmeticException e){throw new JSONException("Centavos inválidos");}
    }
    private static Double number(JSONObject j,String key)throws JSONException{return j.isNull(key)?null:j.getDouble(key);}
    static final class Group {
        final List<JSONObject> rows=new ArrayList<>();JSONObject latest,anchor;
        void add(JSONObject row){rows.add(row);latest=row;if(row.has("actual_result"))anchor=row;}
    }
    static List<Group> groups(JSONArray rows) {
        Map<String,Group> map=new LinkedHashMap<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);if(row==null)continue;
            String key=row.optString("episode_id",row.optString("local_id","legacy-"+i));
            map.computeIfAbsent(key,k->new Group()).add(row);
        }
        return new ArrayList<>(map.values());
    }
    static final class Stats {
        int groups,completed,accepted,declined,cancelled,fullyCosted,compared,invalid;
        double revenueAbsoluteError,minutesAbsoluteError,kmAbsoluteError,marginSum,minutesWithCosts;
    }
    static Stats stats(JSONArray rows) {
        Stats s=new Stats();List<Group> groups=groups(rows);s.groups=groups.size();
        for(Group g:groups)if(g.anchor!=null)try{
            TripOutcome r=parse(g.anchor.getJSONObject("actual_result"));
            if(r.state==TripOutcome.State.ACCEPTED)s.accepted++;if(r.state==TripOutcome.State.DECLINED)s.declined++;if(r.state==TripOutcome.State.CANCELLED)s.cancelled++;
            if(r.state!=TripOutcome.State.COMPLETED)continue;
            s.completed++;JSONObject forecast=g.anchor.optJSONObject("evaluation");
            if(forecast!=null && !"GRIS".equals(forecast.optString("color")) && forecast.has("revenue") && forecast.has("minutes") && forecast.has("km")){
                s.compared++;s.revenueAbsoluteError+=Math.abs(r.settledCents/100d-forecast.getDouble("revenue"));
                s.minutesAbsoluteError+=Math.abs(r.totalMinutes-forecast.getDouble("minutes"));s.kmAbsoluteError+=Math.abs(r.totalKm-forecast.getDouble("km"));
            }
            Double margin=r.actualMargin();if(margin!=null){s.fullyCosted++;s.marginSum+=margin;s.minutesWithCosts+=r.totalMinutes;}
        }catch(Exception e){s.invalid++;}
        return s;
    }
    static String summary(JSONArray rows){
        Stats s=stats(rows);
        return s.groups+" grupos de ofertas · "+s.completed+" viajes completados informados\n"
                +s.accepted+" aceptados pendientes · "+s.declined+" rechazados · "+s.cancelled+" cancelados informados\n"
                +(s.compared==0?"Sin resultados comparables todavía":"Diferencia absoluta media contra estimación ("+s.compared+" casos): "+Ui.amount(s.revenueAbsoluteError/s.compared)+" de importe · "+Ui.km(s.minutesAbsoluteError/s.compared)+" min · "+Ui.km(s.kmAbsoluteError/s.compared)+" km")
                +"\n"+(s.fullyCosted==0?"Saldo real pendiente: faltan costos informados":Ui.amount(s.marginSum)+" de saldo informado · "+Ui.amount(s.marginSum*60/s.minutesWithCosts)+"/h en "+s.fullyCosted+" viajes con todos los costos")
                +"\nCompara versiones y perfiles registrados. No mide toda la jornada ni verifica pagos o tu tasa de aceptación en Uber. Excluye versiones repetidas y costos desconocidos del saldo real."
                +(s.invalid>0?"\n"+s.invalid+" resultados inválidos excluidos":"");
    }
    static String resultDetail(JSONObject row){
        if(!row.has("actual_result"))return "Resultado sin registrar";
        try{
            TripOutcome r=parse(row.getJSONObject("actual_result"));
            String title="Resultado informado por ti: "+stateLabel(r.state)+"\nMotivo: "+reasonLabel(r.reason);
            if(r.state!=TripOutcome.State.COMPLETED)return title;
            JSONObject e=row.optJSONObject("evaluation");Double margin=r.actualMargin();
            return title+"\nRecibido: "+Ui.money(r.settledCents)+"\nTotal real informado: "+Ui.km(r.totalMinutes)+" min · "+Ui.km(r.totalKm)+" km"
                    +(e==null || "GRIS".equals(e.optString("color"))?"":"\nDiferencia contra estimación de esta versión: "+Ui.amount(r.settledCents/100d-e.optDouble("revenue"))+" · "+Ui.km(r.totalMinutes-e.optDouble("minutes"))+" min · "+Ui.km(r.totalKm-e.optDouble("km"))+" km")
                    +"\nEnergía: "+cost(r.energyCents)+" · mantenimiento/desgaste: "+cost(r.upkeepCents)+"\nFijos: "+cost(r.fixedCents)+" · otros gastos: "+cost(r.extrasCents)
                    +"\n"+(margin==null?"Saldo real desconocido: completa los cuatro costos, incluido 0 si corresponde": "Saldo informado: "+Ui.amount(margin)+" · "+Ui.amount(r.actualHourly())+"/h antes de impuestos")
                    +"\nEspera y regreso son partes del total, no se suman otra vez. No confirma por qué cambió el importe ni valida el pago con Uber.";
        }catch(Exception e){return "Resultado inválido: edítalo antes de compararlo";}
    }
    private static String cost(Long cents){return cents==null?"sin informar":Ui.money(cents);}
    static String stateLabel(TripOutcome.State state){return new String[]{"Sin registrar","Rechazado","Aceptado, aún sin terminar","Completado","Cancelado"}[state.ordinal()];}
    static String reasonLabel(TripOutcome.Reason r){return new String[]{"Sin especificar","Rentabilidad","Recogida","Zona","Regla de pasajero","Otro"}[r.ordinal()];}
}
