package mx.tesivil.detector;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingsActivity extends Activity {
    private DriverConfig config;
    private final Map<String,EditText> fields = new LinkedHashMap<>();
    private CheckBox calibrated, zoneFilter, riderFilter, blockNewRider;
    private CheckBox vehicleConfirmed,energyReviewed,costsReviewed,goalsReviewed,rangeUpdated,returnEnabled;
    private Spinner mode;
    private EditText model;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); config=ConfigStore.load(this);
        String section=getIntent().getStringExtra("section");
        if ("zones".equals(section)) { zones(); return; }
        if ("rider".equals(section)) { riderSettings(); return; }
        boolean vehicle=!"goals".equals(section);
        LinearLayout p=FormUi.page(this,vehicle?"Vehículo y costos":"Objetivos y calificación",
                "Valores iniciales de ejemplo. Ajusta con tus registros; no son una ficha técnica ni precios actuales. Todo se guarda en este teléfono.");
        if (vehicle) {
            model=FormUi.input(p,"Modelo del vehículo",config.vehicle,false);
            mode=FormUi.spinner(p,"Energía",new String[]{"PHEV: eléctrico y gasolina","Gasolina o híbrido no enchufable","Solo eléctrico"},config.energyMode.equals("PHEV")?0:config.energyMode.equals("GASOLINA")?1:2);
            field(p,"modelYear","Año (0 = sin registrar)"); field(p,"odometerKm","Odómetro, km (informativo)");
            FormUi.text(p,"Energía",20,true);
            field(p,"fuelPrice","Gasolina, MXN por litro"); field(p,"kmPerLiter","Rendimiento con gasolina, km/L");
            field(p,"electricityPrice","Electricidad, MXN por kWh"); field(p,"kwhPer100Km","Consumo eléctrico, kWh por 100 km");
            field(p,"electricRangeKm","Autonomía eléctrica cargado, km"); field(p,"remainingElectricKm","Autonomía eléctrica restante ahora, km");
            rangeUpdated=FormUi.check(p,"Actualicé la autonomía restante para hoy (si uso electricidad)",false);
            FormUi.text(p,"Actualiza la autonomía al iniciar tu jornada y cuando cambie. No se lee del carro ni se descuenta por ofertas que no aceptas.",13,false);
            FormUi.text(p,"Mantenimiento y desgaste",20,true);
            field(p,"maintenanceCost","Costo de cada servicio, MXN"); field(p,"maintenanceIntervalKm","Intervalo de servicio, km");
            field(p,"tiresCost","Costo del juego de llantas, MXN"); field(p,"tiresLifeKm","Duración estimada de llantas, km");
            field(p,"wearPerKm","Otro desgaste / depreciación por uso, MXN/km");
            field(p,"fixedMonthly","Gastos fijos mensuales asignados, MXN"); field(p,"hoursMonthly","Horas trabajadas al mes");
            FormUi.text(p,"El odómetro no cambia costos automáticamente. Calibra los intervalos con tu mantenimiento. Evita contar dos veces depreciación y financiamiento. El saldo estimado es antes de impuestos y depende de las horas configuradas.",13,false);
            vehicleConfirmed=FormUi.check(p,"Confirmo mi vehículo y el tipo de energía",config.vehicleConfirmed);
            energyReviewed=FormUi.check(p,"Revisé precio y rendimiento de energía con mis registros",config.energyReviewed);
            costsReviewed=FormUi.check(p,"Revisé mantenimiento, desgaste, llantas y gastos fijos",config.costsReviewed);
            FormUi.text(p,"Electricidad a $0 solo corresponde si de verdad no la pagas. Confirmar casillas registra tu revisión; no verifica el consumo del carro. Los perfiles anteriores se conservan, pero requieren estas revisiones específicas.",13,false);
        } else {
            field(p,"minHourly","Mínimo disponible estimado, MXN/h"); field(p,"targetHourly","Meta disponible estimado, MXN/h");
            field(p,"minPerKm","Mínimo disponible estimado, MXN/km"); field(p,"targetPerKm","Meta disponible estimado, MXN/km");
            field(p,"maxPickupMin","Máximo de recogida, minutos"); field(p,"maxPickupKm","Máximo de recogida, km");
            field(p,"pickupShareAlertPercent","Alertar si la recogida consume al menos este % de km o tiempo");
            field(p,"passengerWaitMin","Espera estimada del pasajero, minutos"); field(p,"conservativeExtraMin","Espera extra del escenario conservador, minutos");
            field(p,"repositionKm","Reposicionamiento adicional estimado, km"); field(p,"repositionMin","Reposicionamiento adicional estimado, minutos");
            field(p,"extrasPerTrip","Extras no reembolsados por viaje, MXN"); field(p,"additionalFeePercent","Descuento adicional no incluido en la oferta, %");
            FormUi.text(p,"Deja el descuento adicional en 0 si la oferta ya descuenta la comisión. No se suman propinas o bonos hipotéticos. Reposición en 0 significa que este escenario no considera regreso.",13,false);
            FormUi.text(p,"Escenario alternativo de regreso",20,true);
            returnEnabled=FormUi.check(p,"Comparar también con mi escenario de regreso",config.returnScenarioEnabled);
            field(p,"returnScenarioKm","Regreso alternativo, km (ejemplo editable)");field(p,"returnScenarioMin","Regreso alternativo, minutos (ejemplo editable)");
            FormUi.text(p,"Es una hipótesis tuya, no una predicción de demanda. Este escenario reemplaza la reposición anterior; no suma dos veces el regreso. Si queda bajo tus mínimos, limita a ámbar y conserva cualquier rojo.",13,false);
            FormUi.text(p,"Pesos del score",20,true);
            FormUi.text(p,"Se normalizan entre sí. Son preferencias, no probabilidades. Costos y rendimientos entran en el margen; no vuelven a penalizarse por separado.",13,false);
            field(p,"weightHourly","Peso del margen por hora"); field(p,"weightKm","Peso del margen por km"); field(p,"weightPickup","Peso del esfuerzo de recogida");
            field(p,"greenScore","Score mínimo verde, 0–100"); field(p,"amberScore","Score mínimo ámbar, 0–100"); field(p,"cautionPenalty","Descuento de score por zona de precaución");
            zoneFilter=FormUi.check(p,"Evaluar zonas (sin información no habrá verde)",config.zoneFilter);
            goalsReviewed=FormUi.check(p,"Revisé metas, recogida, esperas y regreso",config.goalsReviewed);
        }
        calibrated=FormUi.check(p,"He revisado mis costos y objetivos",config.calibrated);
        FormUi.text(p,"Mientras uses valores sin revisar, la recomendación no será verde. Puedes analizar escenarios desde Simulador.",13,false);
        FormUi.button(p,"Guardar configuración",()->save());
        FormUi.button(p,"Volver",this::finish);
    }
    private void field(LinearLayout p,String key,String label) {
        try { fields.put(key,FormUi.input(p,label,FormUi.format(DriverConfig.class.getField(key).getDouble(config)),true)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private void save() {
        try {
            double oldRange=config.remainingElectricKm;
            for (Map.Entry<String,EditText> entry:fields.entrySet()) DriverConfig.class.getField(entry.getKey()).setDouble(config,FormUi.number(entry.getValue()));
            if (model!=null) { config.vehicle=model.getText().toString().trim(); config.energyMode=new String[]{"PHEV","GASOLINA","ELECTRICO"}[mode.getSelectedItemPosition()]; }
            if(zoneFilter!=null)config.zoneFilter=zoneFilter.isChecked();
            if(riderFilter!=null)config.riderFilter=riderFilter.isChecked();
            if(blockNewRider!=null)config.blockNewRider=blockNewRider.isChecked();
            if(vehicleConfirmed!=null)config.vehicleConfirmed=vehicleConfirmed.isChecked();
            if(energyReviewed!=null)config.energyReviewed=energyReviewed.isChecked();
            if(costsReviewed!=null)config.costsReviewed=costsReviewed.isChecked();
            if(goalsReviewed!=null)config.goalsReviewed=goalsReviewed.isChecked();
            if(returnEnabled!=null)config.returnScenarioEnabled=returnEnabled.isChecked();
            if(rangeUpdated!=null && (rangeUpdated.isChecked() || oldRange!=config.remainingElectricKm))config.rangeUpdatedAtMs=System.currentTimeMillis();
            if(calibrated!=null)config.calibrated=calibrated.isChecked(); ConfigStore.save(this,config); finish();
        } catch(Exception e) { FormUi.error(this,e instanceof NumberFormatException?"Completa los campos numéricos con valores válidos":e.getMessage()); }
    }
    private void riderSettings() {
        LinearLayout p=FormUi.page(this,"Pasajero y filtros","Reglas de aceptación editables. Una cuenta nueva o una calificación baja no prueban peligrosidad. Solo se usan los datos visibles en la oferta; no se consulta la cuenta del pasajero.");
        riderFilter=FormUi.check(p,"Evaluar calificación y contador del pasajero",config.riderFilter);
        blockNewRider=FormUi.check(p,"Usuario NUEVO genera rojo automático",config.blockNewRider);
        field(p,"newRiderMinCount","Nuevo si el contador es menor que (entero)");
        field(p,"establishedRiderCount","Contador mínimo para permitir verde (entero)");
        field(p,"minRiderRating","Calificación mínima; debajo genera rojo (1–5)");
        field(p,"goodRiderRating","Calificación para permitir verde (1–5)");
        field(p,"riderCautionPenalty","Descuento de score por precaución del pasajero");
        FormUi.text(p,"NUEVO explícito o contador menor al umbral: rojo si activas esa regla, aunque el viaje pague bien. Entre tus mínimos y metas: precaución. Si faltan datos o son ambiguos, no habrá verde; un dato ausente nunca se convierte en cero. Identidad verificada no elimina estas reglas.",14,false);
        FormUi.text(p,"Se lee el número junto a la estrella como contador visible. Su significado exacto en esta interfaz de México no está confirmado; no se presenta como total certificado de viajes. Los umbrales iniciales son preferencias de la app, no estadísticas de riesgo ni reglas oficiales de Uber.",13,false);
        FormUi.button(p,"Guardar configuración",this::save);FormUi.button(p,"Volver",this::finish);
    }
    private void zones() {
        LinearLayout p=FormUi.page(this,"Colonias y reglas de zona",
                "Base ZMG con antecedentes de Fiscalía / IIEG y documentos municipales. Cada referencia conserva su periodo. Los niveles son editables y no constituyen una clasificación oficial de peligrosidad.");
        List<ZoneRule> list=ConfigStore.zones(this);
        config=ConfigStore.load(this);
        if(!ConfigStore.zonesError.isEmpty())FormUi.text(p,ConfigStore.zonesError,16,true);
        CheckBox enabled=FormUi.check(p,"Evaluar zonas (sin información no habrá verde)",config.zoneFilter);
        enabled.setOnCheckedChangeListener((v,b)-> { config.zoneFilter=b; try { ConfigStore.save(this,config); } catch(Exception e){FormUi.error(this,e.getMessage());} });
        FormUi.text(p,"Alto: evitar. Medio: precaución. Bajo: revisada por ti. Los niveles son criterios editables de la app o tuyos. No certifican seguridad. La base empieza en Medio y sigue activa sin vencimiento automático. Se comparan recogida y destino, no la ruta.",14,false);
        FormUi.button(p,"Añadir colonia",()->editZone(list,-1));
        FormUi.button(p,"Vaciar lista",()->new AlertDialog.Builder(this).setTitle("Vaciar lista de colonias")
                .setMessage("Se eliminarán todas tus reglas, incluidas las precargadas. No reaparecerán al reiniciar. Las zonas quedarán sin verificar.")
                .setNegativeButton("Cancelar",null).setPositiveButton("Vaciar",(d,w)->{try{ConfigStore.saveZones(this,java.util.List.of());zones();}catch(Exception e){FormUi.error(this,e.getMessage());}}).show());
        FormUi.button(p,"Volver",this::finish);
        EditText search=FormUi.input(p,"Buscar colonia o municipio","",false);
        TextView count=FormUi.text(p,list.size()+" reglas guardadas",16,true);
        LinearLayout results=FormUi.column(this);p.addView(results);
        Runnable render=()->renderZones(results,count,list,search.getText().toString());
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int n,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int n){render.run();}
            public void afterTextChanged(android.text.Editable e){}
        });
        render.run();
    }
    private void renderZones(LinearLayout p,TextView count,List<ZoneRule> list,String query) {
        p.removeAllViews();String q=ZoneRule.norm(query);int shown=0;
        for(int i=0;i<list.size();i++) {
            final int index=i; ZoneRule r=list.get(i);
            if(!ZoneRule.norm(r.neighborhood+" "+r.municipality).contains(q))continue;shown++;
            FormUi.button(p,r.neighborhood+" · "+r.levelLabel()+"\n"+(r.municipality.isBlank()?"Cualquier municipio":r.municipality),()->editZone(list,index));
            FormUi.text(p,(r.catalogId.isEmpty() && r.supplementEvidence.isEmpty()?"Regla personal":r.referenceKind())+" · "+(r.noExpiry?"sin vencimiento automático":"revisión "+r.reviewedOn+" · "+r.validDays+" días"),12,false);
        }
        count.setText(shown+" de "+list.size()+" reglas · toca una para editar");
        if(shown==0)FormUi.text(p,list.isEmpty()?"Lista vacía · zonas sin verificar":"Sin coincidencias en tu lista",16,false);
    }
    private void editZone(List<ZoneRule> rules,int index) {
        ZoneRule original=index<0?new ZoneRule():rules.get(index);
        LinearLayout form=FormUi.column(this); int pad=Ui.dp(this,18); form.setPadding(pad,pad,pad,pad);
        ScrollView scroll=new ScrollView(this); scroll.addView(form);
        if(!original.catalogId.isEmpty() || !original.supplementEvidence.isEmpty()) {
            FormUi.text(form,original.referenceKind()+" · nivel editable",15,true);
            FormUi.button(form,"Ver fuente y conteos",()->sourceDetails(original));
            FormUi.text(form,"Cambiar el nivel conserva la fuente. Cambiar colonia o municipio crea una regla personal.",13,false);
        }
        EditText name=FormUi.input(form,"Colonia (nombre completo)",original.neighborhood,false);
        EditText muni=FormUi.input(form,"Municipio (vacío = cualquiera)",original.municipality,false);
        Spinner action=FormUi.spinner(form,"Nivel y acción",new String[]{"Alto · Evitar","Medio · Precaución","Bajo · Revisada por mí"},original.action.ordinal());
        CheckBox pickup=FormUi.check(form,"Aplicar a recogida",original.pickup), destination=FormUi.check(form,"Aplicar a destino",original.destination);
        EditText start=FormUi.input(form,"Desde hora 0–23 (igual a hasta = todo el día)",Integer.toString(original.startHour),true);
        EditText end=FormUi.input(form,"Hasta hora 0–23",Integer.toString(original.endHour),true);
        EditText source=FormUi.input(form,"Fuente o motivo (no se verifica automáticamente)",original.source,false);
        EditText date=FormUi.input(form,"Fecha de referencia o revisión, AAAA-MM-DD",original.reviewedOn,false);
        CheckBox noExpiry=FormUi.check(form,"Sin vencimiento automático",original.noExpiry);
        EditText days=FormUi.input(form,"Vigencia de tu revisión, días (1–365)",Integer.toString(original.validDays),true);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(index<0?"Nueva regla":"Editar regla").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null);
        if(index>=0)builder.setNeutralButton("Eliminar",(d,w)-> { List<ZoneRule> updated=new java.util.ArrayList<>(rules);updated.remove(index);try {ConfigStore.saveZones(this,updated);zones();}catch(Exception e){FormUi.error(this,e.getMessage());} });
        AlertDialog dialog=builder.create(); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v-> {
            try {
                ZoneRule r=new ZoneRule(); r.neighborhood=name.getText().toString().trim(); r.municipality=muni.getText().toString().trim();
                r.action=ZoneRule.Action.values()[action.getSelectedItemPosition()];r.pickup=pickup.isChecked();r.destination=destination.isChecked();
                r.startHour=Integer.parseInt(start.getText().toString()); r.endHour=Integer.parseInt(end.getText().toString()); r.validDays=Integer.parseInt(days.getText().toString());
                r.source=source.getText().toString().trim();r.reviewedOn=date.getText().toString().trim();
                r.noExpiry=noExpiry.isChecked();
                if(ZoneRule.samePlace(original,r)) {r.catalogId=original.catalogId;r.officialEvidence=original.officialEvidence;r.officialUrl=original.officialUrl;r.supplementEvidence=original.supplementEvidence;}
                else if(!original.catalogId.isEmpty() && r.source.equals(original.source))r.source="Preferencia personal · colonia modificada";
                String error=r.validate();if(error!=null)throw new IllegalArgumentException(error);
                List<ZoneRule> updated=new java.util.ArrayList<>(rules);
                if(index<0)updated.add(r);else updated.set(index,r);ConfigStore.saveZones(this,updated);dialog.dismiss();zones();
            }catch(Exception e){FormUi.error(this,e instanceof NumberFormatException?"Las horas y días deben ser enteros":e.getMessage());}
        });
    }
    private void sourceDetails(ZoneRule rule) {
        LinearLayout p=FormUi.column(this);int pad=Ui.dp(this,20);p.setPadding(pad,pad,pad,pad);
        FormUi.text(p,rule.officialEvidence,14,false);
        TextView link=FormUi.text(p,rule.officialUrl,12,false);android.text.util.Linkify.addLinks(link,android.text.util.Linkify.WEB_URLS);
        if(!rule.supplementEvidence.isEmpty()){TextView refs=FormUi.text(p,rule.supplementEvidence,14,false);android.text.util.Linkify.addLinks(refs,android.text.util.Linkify.WEB_URLS);}
        ScrollView scroll=new ScrollView(this);scroll.addView(p);
        new AlertDialog.Builder(this).setTitle("Fuente y periodo original").setView(scroll).setPositiveButton("Entendido",null).show();
    }
}
