package mx.tesivil.detector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalTime;

public final class ReviewActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);HistoryStore.init(this);if("history".equals(getIntent().getStringExtra("section")))history();else simulator();}
    private void simulator(){
        LinearLayout p=FormUi.page(this,"Simulador de viajes","Datos de ejemplo editables. Usa tu perfil actual; no es una solicitud real ni se guarda como un viaje realizado.");
        EditText fare=FormUi.input(p,"Importe de la oferta, MXN","98.31",true);
        Spinner service=FormUi.spinner(p,"Tipo de oferta (sin puntos adicionales)",new String[]{"UberX","UberXL","Priority"},0);
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
                OfferParser.ServiceType type=service.getSelectedItemPosition()==2?OfferParser.ServiceType.PRIORITY:service.getSelectedItemPosition()==1?OfferParser.ServiceType.UBER_XL:OfferParser.ServiceType.UBER_X;
                OfferParser.Offer o=new OfferParser.Offer(Math.round(price*100),pMin,pKm,tMin,tKm,null,pa.getText().toString(),da.getText().toString(),rider,type,false);
                java.util.List<ZoneRule> rules=ConfigStore.zones(this);DriverConfig c=ConfigStore.load(this);ScoreEngine.Evaluation e=ScoreEngine.evaluate(o,c,rules,LocalDate.now(),LocalTime.now().getHour());
                output.setText("Tipo: "+o.typeLabel()+"\n\n"+Ui.details(e,c));output.setTextColor(Ui.signalColor(e));
            }catch(Exception e){FormUi.error(this,"Revisa las cifras y el pasajero. Minutos y contador deben ser enteros; calificación de 1 a 5.");}
        });
        FormUi.button(p,"Volver",this::finish);
    }
    private void history(){
        JSONArray rows=HistoryStore.snapshot();
        LinearLayout p=FormUi.page(this,"Historial local",rows.length()+" evaluaciones · máximo 200. Son ofertas observadas, no viajes aceptados ni ingresos realizados. Se conserva perfil, cálculo y motivo sin direcciones de pasajeros.");
        FormUi.button(p,"Borrar historial",()->new AlertDialog.Builder(this).setTitle("Borrar historial local").setMessage("Se eliminarán las evaluaciones guardadas en este teléfono.").setNegativeButton("Cancelar",null).setPositiveButton("Borrar",(d,w)->{HistoryStore.clear();history();}).show());
        FormUi.button(p,"Volver",this::finish);
        for(int i=rows.length()-1;i>=0;i--){
            try{
                JSONObject r=rows.getJSONObject(i),e=r.getJSONObject("evaluation");
                String time=java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,java.text.DateFormat.SHORT).format(new java.util.Date(r.getLong("observed_at_ms")));
                FormUi.text(p,time+" · "+r.optString("source"),12,false);
                FormUi.text(p,Ui.money(r.getLong("fare_cents"))+" · "+e.getString("color")+" "+e.getInt("score")+"/100",18,true);
                FormUi.text(p,Ui.amount(e.getDouble("hourly"))+"/h estimados\n"+e.getJSONArray("reasons").join(" · ").replace("\"","")+"\nPerfil revisión "+r.getJSONObject("config").getLong("revision"),14,false);
                FormUi.button(p,"Ver desglose",()->historyDetail(r));
            }catch(Exception ignored){}
        }
    }
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
                    +"\nPor hora: "+Ui.amount(e.getDouble("hourly"))+"\nPor km: "+Ui.amount(e.getDouble("per_km"))
                    +"\nCon espera extra: "+Ui.amount(e.getDouble("conservative_hourly"))+"/h"
                    +"\n\nRecogida: "+e.optString("pickup_zone_status")+"\nDestino: "+e.optString("destination_zone_status")
                    +"\n\nPasajero registrado: "+e.optString("rider_summary","Sin datos en esta versión del registro")
                    +"\nFiltro: "+(c.optBoolean("riderFilter",false)?"activado":"desactivado / registro anterior")
                    +"\nUmbral configurado de usuario nuevo: contador visible < "+c.optInt("newRiderMinCount",5)
                    +"\n\nPerfil registrado: "+c.getString("vehicle")+" · revisión "+c.getLong("revision")
                    +"\nEs una estimación de la oferta, no un cobro confirmado ni un análisis de toda la ruta.";
            ScrollView scroll=new ScrollView(this);TextView text=Ui.text(this,details,15,false);int pad=Ui.dp(this,20);text.setPadding(pad,pad,pad,pad);scroll.addView(text);
            new AlertDialog.Builder(this).setTitle("Cálculo registrado").setView(scroll).setPositiveButton("Entendido",null).show();
        }catch(Exception e){FormUi.error(this,"No se pudo abrir este registro");}
    }
}
