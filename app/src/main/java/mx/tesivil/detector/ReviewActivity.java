package mx.tesivil.detector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;

public final class ReviewActivity extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    @Override public void onCreate(Bundle state){super.onCreate(state);HistoryStore.init(this);String section=getIntent().getStringExtra("section");if("history".equals(section)||"results".equals(section))history();else simulator();}
    @Override public void onDestroy(){worker.shutdown();super.onDestroy();}
    private void simulator(){
        LinearLayout p=FormUi.page(this,"Simulador de viajes","Datos de ejemplo editables. Usa tu perfil actual; no es una solicitud real ni se guarda como un viaje realizado.");
        EditText fare=FormUi.input(p,"Importe de la oferta, MXN","98.31",true);
        Spinner service=FormUi.spinner(p,"Tipo de oferta (sin puntos adicionales)",new String[]{"UberX","UberXL","Priority","Comfort"},0);
        CheckBox reserve=FormUi.check(p,"Oferta reservada (horario y espera pendientes)",false);
        EditText pm=FormUi.input(p,"Recogida, minutos enteros","14",true),pk=FormUi.input(p,"Recogida, km","6.3",true);
        EditText tm=FormUi.input(p,"Trayecto, minutos enteros","18",true),tk=FormUi.input(p,"Trayecto, km","4",true);
        EditText pa=FormUi.input(p,"Dirección o colonia de recogida","",false),da=FormUi.input(p,"Dirección o colonia de destino","",false);
        EditText rating=FormUi.input(p,"Calificación del pasajero (vacío = sin leer)","",true);
        EditText count=FormUi.input(p,"Contador visible (vacío = sin leer)","",true);
        CheckBox newRider=FormUi.check(p,"La oferta muestra usuario NUEVO",false);
        TextView output=FormUi.text(p,"Toca Evaluar para ver el cálculo",16,true);
        FormUi.button(p,"Evaluar simulación",()->{
            try{
                double price=FormUi.number(fare);double pKm=FormUi.number(pk),tKm=FormUi.number(tk);
                int pMin=Integer.parseInt(pm.getText().toString()),tMin=Integer.parseInt(tm.getText().toString());
                if(price<=0||price>100000||pKm<0||pKm>300||tKm<=0||tKm>1000||pMin<0||pMin>240||tMin<1||tMin>600)throw new IllegalArgumentException("Importe, minutos o km fuera de rango");
                Double r=rating.getText().toString().isBlank()?null:FormUi.number(rating);
                Integer n=count.getText().toString().isBlank()?null:Integer.valueOf(count.getText().toString().trim());
                RiderProfile rider=new RiderProfile(r,n,newRider.isChecked(),newRider.isChecked() && n!=null && n>0,"SIMULATION");
                if(rider.ambiguous)throw new IllegalArgumentException("Datos de pasajero inválidos");
                OfferParser.ServiceType type=new OfferParser.ServiceType[]{OfferParser.ServiceType.UBER_X,OfferParser.ServiceType.UBER_XL,OfferParser.ServiceType.PRIORITY,OfferParser.ServiceType.COMFORT}[service.getSelectedItemPosition()];
                OfferParser.Offer o=new OfferParser.Offer(cents(fare),pMin,pKm,tMin,tKm,null,pa.getText().toString(),da.getText().toString(),rider,type,false,0,reserve.isChecked());
                java.util.List<ZoneRule> rules=ConfigStore.zones(this);DriverConfig c=ConfigStore.load(this);ScoreEngine.Evaluation e=ScoreEngine.evaluate(o,c,rules,LocalDate.now(),LocalTime.now().getHour());
                output.setText("Tipo: "+o.typeLabel()+"\n\n"+Ui.details(e,c));output.setTextColor(Ui.signalColor(e));
            }catch(Exception e){FormUi.error(this,"Revisa las cifras y el pasajero. Minutos y contador deben ser enteros; calificación de 1 a 5.");}
        });
        FormUi.button(p,"Volver",this::finish);
    }
    private void history(){
        JSONArray rows=HistoryStore.snapshot();
        LinearLayout p=FormUi.page(this,"Resultados y precisión",TripJournal.summary(rows));
        FormUi.text(p,rows.length()+" versiones guardadas · máximo 200. El agrupamiento es aproximado y local: solo agrupa lecturas consecutivas con los mismos datos visibles. Registra tú el resultado de la oferta seleccionada. Ninguna lectura acepta, rechaza ni completa un viaje.",14,false);
        FormUi.button(p,"Borrar historial",()->new AlertDialog.Builder(this).setTitle("Borrar historial local").setMessage("Se eliminarán las ofertas y los resultados informados en este teléfono.").setNegativeButton("Cancelar",null).setPositiveButton("Borrar",(d,w)->{HistoryStore.clear();history();}).show());
        FormUi.button(p,"Volver",this::finish);
        List<TripJournal.Group> groups=TripJournal.groups(rows);
        for(int i=groups.size()-1;i>=0;i--){
            try{
                TripJournal.Group g=groups.get(i);JSONObject r=g.latest,e=r.getJSONObject("evaluation");
                String time=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(r.getLong("observed_at_ms")));
                FormUi.text(p,time+" · "+r.optString("source"),12,false);
                FormUi.text(p,Ui.money(r.getLong("fare_cents"))+" · "+e.getString("color")+" "+e.getInt("score")+"/100",18,true);
                FormUi.text(p,(e.has("trip_contribution")?"Aporte: "+Ui.amount(e.getDouble("contribution_hourly"))+"/h · ":"")+"Tras fijos: "+Ui.amount(e.getDouble("hourly"))+"/h estimados\n"+e.getJSONArray("reasons").join(" · ").replace("\"","")+"\nPerfil revisión "+r.getJSONObject("config").getLong("revision"),14,false);
                FormUi.text(p,g.rows.size()+" versiones · "+(g.anchor==null?"Resultado sin registrar":TripJournal.resultDetail(g.anchor)),14,false);
                FormUi.button(p,"Ver versiones y registrar resultado",()->versions(g));
            }catch(Exception ignored){}
        }
    }
    private void versions(TripJournal.Group group){
        LinearLayout p=FormUi.page(this,"Versiones de la oferta","Selecciona el importe y perfil con los que decidiste. Registrar otra versión sustituye el resultado de este grupo; nunca suma otro viaje completado.");
        for(JSONObject row:group.rows){
            FormUi.text(p,versionLabel(row)+"\n"+TripJournal.resultDetail(row),16,true);
            FormUi.button(p,"Ver cálculo de esta versión",()->historyDetail(row));
            if(!row.optString("local_id").isBlank())FormUi.button(p,"Registrar resultado de esta versión",()->editOutcome(row));
            else FormUi.text(p,"Registro anterior sin identificador: no puede recibir un resultado nuevo.",14,false);
        }
        FormUi.button(p,"Volver a resultados",this::history);
    }
    private String versionLabel(JSONObject row){
        JSONObject config=row.optJSONObject("config");
        return Ui.money(row.optLong("fare_cents"))+" · versión "+row.optInt("offer_version",1)+" · perfil revisión "+(config==null?"sin dato":config.optLong("revision"))+"\n"+java.text.DateFormat.getDateTimeInstance().format(new java.util.Date(row.optLong("observed_at_ms")));
    }
    private void editOutcome(JSONObject row){
        TripOutcome old=new TripOutcome();try{if(row.has("actual_result"))old=TripJournal.parse(row.getJSONObject("actual_result"));}catch(Exception ignored){}
        LinearLayout p=FormUi.page(this,"Resultado de esta oferta",versionLabel(row)+"\nDatos voluntarios informados por ti. No se verifican con Uber. Los campos reales empiezan vacíos; no se rellenan con la estimación.");
        Spinner state=FormUi.spinner(p,"Resultado informado",Arrays.stream(TripOutcome.State.values()).map(TripJournal::stateLabel).toArray(String[]::new),old.state.ordinal());
        Spinner reason=FormUi.spinner(p,"Motivo",Arrays.stream(TripOutcome.Reason.values()).map(TripJournal::reasonLabel).toArray(String[]::new),old.reason.ordinal());
        LinearLayout actual=FormUi.column(this);p.addView(actual);
        FormUi.text(actual,"Importe recibido después de descuentos de la plataforma. Usa el total de recogida + viaje + espera + regreso que tú incluyas; espera y regreso son partes de ese total. Si los incluyes, interpreta la diferencia contra la estimación con esa misma cobertura.",14,false);
        EditText paid=FormUi.input(actual,"Recibido real, MXN (obligatorio)",cash(old.settledCents),true);
        EditText minutes=FormUi.input(actual,"Tiempo total real, min (obligatorio)",num(old.totalMinutes),true),km=FormUi.input(actual,"Distancia total real, km (obligatorio)",num(old.totalKm),true);
        EditText wait=FormUi.input(actual,"Espera dentro del total, min (opcional)",num(old.waitMinutes),true),returnMin=FormUi.input(actual,"Regreso dentro del total, min (opcional)",num(old.repositionMinutes),true),returnKm=FormUi.input(actual,"Regreso dentro del total, km (opcional)",num(old.repositionKm),true);
        FormUi.text(actual,"Costos atribuibles a este viaje. Vacío = desconocido; escribe 0 solo si ese costo realmente fue cero. Un costo estimado por ti sigue siendo un dato informado, no medido por la app.",14,false);
        EditText energy=FormUi.input(actual,"Energía real informada, MXN (opcional)",cash(old.energyCents),true),upkeep=FormUi.input(actual,"Mantenimiento y desgaste informados, MXN (opcional)",cash(old.upkeepCents),true),fixed=FormUi.input(actual,"Fijos asignados informados, MXN (opcional)",cash(old.fixedCents),true),extras=FormUi.input(actual,"Otros gastos informados, MXN (opcional)",cash(old.extrasCents),true);
        actual.setVisibility(old.state==TripOutcome.State.COMPLETED?View.VISIBLE:View.GONE);
        state.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> a){}public void onItemSelected(AdapterView<?> a,View v,int position,long id){actual.setVisibility(position==TripOutcome.State.COMPLETED.ordinal()?View.VISIBLE:View.GONE);}});
        Button save=new Button(this);save.setText("Guardar resultado informado");save.setAllCaps(false);p.addView(save);
        save.setOnClickListener(v->{
            try{
                TripOutcome r=new TripOutcome();r.state=TripOutcome.State.values()[state.getSelectedItemPosition()];r.reason=TripOutcome.Reason.values()[reason.getSelectedItemPosition()];
                if(r.state==TripOutcome.State.COMPLETED){r.settledCents=cents(paid);r.totalMinutes=decimal(minutes);r.totalKm=decimal(km);r.waitMinutes=decimal(wait);r.repositionMinutes=decimal(returnMin);r.repositionKm=decimal(returnKm);r.energyCents=cents(energy);r.upkeepCents=cents(upkeep);r.fixedCents=cents(fixed);r.extrasCents=cents(extras);}
                String error=r.validate();if(error!=null){FormUi.error(this,error);return;}
                save.setEnabled(false);
                worker.execute(()->{try{HistoryStore.saveOutcome(row.getString("local_id"),r);runOnUiThread(()->{if(!isDestroyed()){if(r.state==TripOutcome.State.COMPLETED || r.state==TripOutcome.State.DECLINED || r.state==TripOutcome.State.CANCELLED)Diagnostics.episodes.finish(row.optString("episode_id"));history();}});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){save.setEnabled(true);FormUi.error(this,"No se pudo guardar el resultado de esta oferta. Reabre el historial y comprueba que siga disponible.");}});}});
            }catch(Exception e){FormUi.error(this,"Revisa las cifras. Importe y costos admiten hasta dos decimales; vacío conserva un dato desconocido.");}
        });
        FormUi.button(p,"Cancelar edición",this::history);
    }
    private static String cash(Long n){return n==null?"":BigDecimal.valueOf(n,2).toPlainString();}
    private static String num(Double n){return n==null?"":FormUi.format(n);}
    private static Long cents(EditText e){String s=e.getText().toString().trim().replace(',','.');return s.isBlank()?null:new BigDecimal(s).movePointRight(2).longValueExact();}
    private static Double decimal(EditText e){return e.getText().toString().isBlank()?null:FormUi.number(e);}
    private void historyDetail(JSONObject r){
        try{
            JSONObject e=r.getJSONObject("evaluation"),c=r.getJSONObject("config");
            String details="Evaluación guardada · "+e.getString("color")+" "+e.getInt("score")+"/100\n"
                    +e.getJSONArray("reasons").join("\n").replace("\"","")
                    +"\n\nTipo: "+r.optString("service_label","Sin tipo registrado")
                    +"\nLector: "+r.optString("reader_version","Registro anterior; importe no revalidado con 0.3.1")
                    +"\nOferta: "+Ui.money(r.getLong("fare_cents"))+"\nTiempo estimado: "+Ui.km(e.getDouble("minutes"))+" min"
                    +"\nDistancia estimada: "+Ui.km(e.getDouble("km"))+" km\nEnergía: "+Ui.amount(e.getDouble("energy_cost"))
                    +"\nMantenimiento y desgaste: "+Ui.amount(e.getDouble("upkeep_cost"))+"\nFijos prorrateados: "+Ui.amount(e.getDouble("allocated_fixed_cost"))
                    +"\nExtras: "+Ui.amount(e.getDouble("extras"))+"\nDisponible estimado: "+Ui.amount(e.getDouble("estimated_remaining"))
                    +(e.has("trip_contribution")?"\nAporte antes de fijos: "+Ui.amount(e.getDouble("trip_contribution"))+" · "+Ui.amount(e.getDouble("contribution_hourly"))+"/h\nBase del semáforo: "+e.getString("evaluation_basis"):"")
                    +"\nPor hora: "+Ui.amount(e.getDouble("hourly"))+"\nPor km: "+Ui.amount(e.getDouble("per_km"))
                    +"\nCon espera extra: "+Ui.amount(e.getDouble("conservative_hourly"))+"/h"
                    +(e.has("economy_status")?"\nRentabilidad: "+e.optString("economy_status")+" · económico "+e.optInt("economy_score")+"/100":"")
                    +(e.has("minimum_economic_fare")?"\nImporte mínimo económico: "+Ui.amount(e.optDouble("minimum_economic_fare"))+"\nEspera extra tolerable, carro detenido: "+Ui.km(e.optDouble("extra_wait_tolerance_min"))+" min":"")
                    +(e.has("pending")?"\nPendientes: "+e.getJSONArray("pending").join("; ").replace("\"",""):"")
                    +(e.has("alerts")?"\nAlertas: "+e.getJSONArray("alerts").join("; ").replace("\"",""):"")
                    +(e.has("return_scenario_hourly")?"\nEscenario de regreso configurado: "+Ui.amount(e.optDouble("return_scenario_hourly"))+"/h; sustituye reposición base":"")
                    +"\n\nRecogida: "+e.optString("pickup_zone_status")+"\nDestino: "+e.optString("destination_zone_status")
                    +"\nFuente recogida: "+e.optString("pickup_zone_reference","Sin referencia guardada")+"\nFuente destino: "+e.optString("destination_zone_reference","Sin referencia guardada")
                    +"\n\nPasajero registrado: "+e.optString("rider_summary","Sin datos en esta versión del registro")
                    +"\nFiltro: "+(c.optBoolean("riderFilter",false)?"activado":"desactivado / registro anterior")
                    +"\nUmbral configurado de usuario nuevo: contador visible < "+c.optInt("newRiderMinCount",5)
                    +"\n\nPerfil registrado: "+c.getString("vehicle")+" · revisión "+c.getLong("revision")
                    +"\nEs una estimación de la oferta, no un cobro confirmado ni un análisis de toda la ruta.\n\n"+TripJournal.resultDetail(r);
            ScrollView scroll=new ScrollView(this);TextView text=Ui.text(this,details,15,false);int pad=Ui.dp(this,20);text.setPadding(pad,pad,pad,pad);scroll.addView(text);
            new AlertDialog.Builder(this).setTitle("Cálculo registrado").setView(scroll).setPositiveButton("Entendido",null).show();
        }catch(Exception e){FormUi.error(this,"No se pudo abrir este registro");}
    }
}
