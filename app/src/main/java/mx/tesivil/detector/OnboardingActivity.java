package mx.tesivil.detector;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

/** Short, local setup. A draft is written only after the user reviews and accepts it. */
public final class OnboardingActivity extends Activity {
    private int step;
    private OnboardingProfiles.Vehicle vehicle=OnboardingProfiles.Vehicle.SEDAN;
    private OnboardingProfiles.Ownership ownership=OnboardingProfiles.Ownership.OWNED;
    private OnboardingProfiles.Charging charging=OnboardingProfiles.Charging.HOME;
    private OnboardingProfiles.Strategy strategy=OnboardingProfiles.Strategy.BALANCED;
    private OnboardingProfiles.Use use;
    private String payment="",weeklyHours="40",remainingKm="";
    private String uberPercent="";
    private boolean rentalIncludesUpkeep;
    private DriverConfig previous;
    private long expectedRevision;
    private EditText paymentInput,hoursInput,rangeInput,useInput;
    private Spinner chargingInput;
    private CheckBox includedInput;
    private final OnBackInvokedCallback backCallback=this::back;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(Diagnostics.running){finish();return;}
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,backCallback);
        previous=ConfigStore.load(this);expectedRevision=previous.revision;
        if(state!=null){
            step=state.getInt("step");vehicle=OnboardingProfiles.Vehicle.valueOf(state.getString("vehicle",vehicle.name()));
            ownership=OnboardingProfiles.Ownership.valueOf(state.getString("ownership",ownership.name()));
            charging=OnboardingProfiles.Charging.valueOf(state.getString("charging",charging.name()));
            strategy=OnboardingProfiles.Strategy.valueOf(state.getString("strategy",strategy.name()));
            payment=state.getString("payment","");weeklyHours=state.getString("hours","40");remainingKm=state.getString("range","");
            rentalIncludesUpkeep=state.getBoolean("included");
            String savedUse=state.getString("use","");use=savedUse.isEmpty()?null:OnboardingProfiles.Use.valueOf(savedUse);
            uberPercent=state.getString("uberPercent","");
            expectedRevision=state.getLong("revision",expectedRevision);
        }
        render();
    }
    private int goalStep(){return vehicle.needsCharge()?4:3;}
    private int summaryStep(){return goalStep()+1;}
    private LinearLayout page(String title,String help){
        paymentInput=null;hoursInput=null;rangeInput=null;useInput=null;chargingInput=null;includedInput=null;
        LinearLayout p=FormUi.page(this,title,help);
        FormUi.text(p,"Paso "+(step+1)+" de "+(summaryStep()+1),12,true).setTextColor(Ui.TEAL);
        return p;
    }
    private void render(){
        if(step==0){
            LinearLayout p=page("¿Qué carro manejas?","Elige el tipo más parecido. Cargaremos un perfil completo para que no tengas que llenar todos los costos.");
            for(OnboardingProfiles.Vehicle choice:OnboardingProfiles.Vehicle.values()){
                choice(p,choice.title,choice.examples,false,()->{vehicle=choice;remainingKm="";advance();});
            }
            FormUi.text(p,"Los ejemplos indican el tipo de carro. Sus costos y rendimientos son estimaciones iniciales, no la ficha técnica exacta de esos modelos.",13,false);
            FormUi.button(p,"Ahora no",this::cancel);
            return;
        }
        if(step==1){
            LinearLayout p=page("¿Cómo pagas tu carro?","Esto evita omitir la renta o mensualidad y descontar el valor del carro dos veces.");
            ownershipChoice(p,"Ya está pagado",OnboardingProfiles.Ownership.OWNED);
            ownershipChoice(p,"Lo estoy pagando",OnboardingProfiles.Ownership.FINANCED);
            ownershipChoice(p,"Lo rento",OnboardingProfiles.Ownership.RENTED);
            if(ownership!=OnboardingProfiles.Ownership.OWNED){
                paymentInput=FormUi.input(p,ownership==OnboardingProfiles.Ownership.RENTED?"Renta por semana, MXN":"Mensualidad del carro, MXN",payment,true);
                paymentInput.setHint("Escribe el importe que pagas");
                if(ownership==OnboardingProfiles.Ownership.RENTED)includedInput=FormUi.check(p,"La renta incluye mantenimiento y llantas",rentalIncludesUpkeep);
                FormUi.text(p,"Verás el aporte de cada viaje y el saldo después de gastos fijos y pagos. La mensualidad completa sigue siendo una obligación.",13,false);
            }
            navigation(p);return;
        }
        if(step==2){
            LinearLayout p=page("Tu jornada y el uso del carro","Cuenta las horas de Uber por semana, incluyendo el tiempo que esperas ofertas. El uso personal del carro se pregunta por separado.");
            hoursInput=FormUi.input(p,"Horas de trabajo por semana",weeklyHours,true);
            LinearLayout shortcuts=new LinearLayout(this);
            for(int h:new int[]{10,20,40,60}){
                Button b=new Button(this);b.setText(h+" h");b.setAllCaps(false);b.setTextColor(Ui.INK);
                shortcuts.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
                b.setOnClickListener(v->hoursInput.setText(Integer.toString(h)));
            }
            p.addView(shortcuts);
            FormUi.text(p,"¿Este carro también lo usas fuera de Uber?",18,true);
            choice(p,"Solo para Uber","Asignar 100% de los gastos compartidos",use==OnboardingProfiles.Use.EXCLUSIVE,()->{captureDraft();use=OnboardingProfiles.Use.EXCLUSIVE;uberPercent="100";render();});
            choice(p,"También uso personal u otro trabajo","Repartir los gastos fijos compartidos",use==OnboardingProfiles.Use.MIXED,()->{captureDraft();if(use!=OnboardingProfiles.Use.MIXED)uberPercent="";use=OnboardingProfiles.Use.MIXED;render();});
            if(use==OnboardingProfiles.Use.MIXED){
                useInput=FormUi.input(p,"Porcentaje aproximado de km para Uber",uberPercent,true);
                LinearLayout shares=new LinearLayout(this);
                for(int percent:new int[]{25,50,75}){
                    Button b=new Button(this);b.setText(percent+"%");b.setAllCaps(false);b.setTextColor(Ui.INK);
                    shares.addView(b,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
                    b.setOnClickListener(v->useInput.setText(Integer.toString(percent)));
                }
                p.addView(shares);
                FormUi.text(p,"Es una aproximación: km de Uber ÷ todos los km del carro. No reducimos mantenimiento ni llantas por ese porcentaje; ya se cobran por los km del viaje.",13,false);
            }
            FormUi.text(p,"Las horas ayudan a revisar el presupuesto mensual. Trabajar poco no hace malo por sí solo un viaje que aporta dinero.",13,false);
            navigation(p);return;
        }
        if(vehicle.needsCharge() && step==3){
            LinearLayout p=page("Carga y autonomía","Para calcular cuándo empiezas a pagar electricidad o gasolina necesitamos dos datos sencillos.");
            String[] labels=java.util.Arrays.stream(OnboardingProfiles.Charging.values()).map(c->c.label).toArray(String[]::new);
            chargingInput=FormUi.spinner(p,"¿Cómo cargas tu carro?",labels,charging.ordinal());
            rangeInput=FormUi.input(p,"Autonomía eléctrica que queda ahora, km",remainingKm,true);
            rangeInput.setHint("Ejemplo: "+(int)vehicle.exampleRange()+"; usa lo que muestra tu carro");
            FormUi.text(p,"Si cargas con paneles y no pagas electricidad, elige $0. Los precios de las otras opciones son estimaciones editables.",13,false);
            FormUi.text(p,vehicle.energyMode.equals("PHEV")?"Al agotar los km eléctricos restantes, el cálculo usa gasolina. Actualiza esos km conforme manejes; no se leen del carro.":"Actualiza los km restantes conforme manejes. Una oferta que supera esa autonomía no se recomienda.",13,false);
            navigation(p);return;
        }
        if(step==goalStep()){
            LinearLayout p=page("¿Qué tan exigente quieres ser?","Las metas comparan el aporte de cada viaje después de energía y reservas por kilometraje, antes de gastos fijos e impuestos. El saldo tras fijos se muestra aparte. No son promedios ni promesas de ganancias.");
            for(OnboardingProfiles.Strategy choice:OnboardingProfiles.Strategy.values()){
                choice(p,choice.title,choice.description+"\nPor km: mínimo $"+(int)choice.minPerKm+" · meta $"+(int)choice.targetPerKm,
                        choice==strategy,()->{strategy=choice;advance();});
            }
            FormUi.text(p,"Rojo: incumple un mínimo o una regla. Ámbar: requiere revisión. Verde: cumple las metas y reglas evaluadas. La recogida, el pasajero y las zonas también cuentan.",13,false);
            FormUi.button(p,"Atrás",this::back);return;
        }
        summary();
    }
    private void ownershipChoice(LinearLayout p,String label,OnboardingProfiles.Ownership choice){
        choice(p,label,"",choice==ownership,()->{captureDraft();ownership=choice;render();});
    }
    private void choice(LinearLayout p,String title,String subtitle,boolean selected,Runnable action){
        Button b=FormUi.button(p,(selected?"✓ ":"")+title+(subtitle.isEmpty()?"":"\n"+subtitle),action);
        b.setContentDescription(title);b.setTextSize(16);b.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));
        b.setBackground(Ui.shape(selected?Color.rgb(221,243,246):Color.WHITE,12,this));
    }
    private void navigation(LinearLayout p){
        Button next=FormUi.button(p,"Continuar",()->{
            captureDraft();
            try{
                if(step==1 && ownership!=OnboardingProfiles.Ownership.OWNED){double amount=number(payment,"Escribe cuánto pagas por el carro");if(amount<=0 || amount>100000)throw new IllegalArgumentException("Revisa el pago del carro (mayor que 0, máximo $100,000)");}
                if(step==2){
                    double hours=number(weeklyHours,"Escribe tus horas por semana");if(hours<1 || hours>168)throw new IllegalArgumentException("Escribe de 1 a 168 horas por semana");
                    if(use==null)throw new IllegalArgumentException("Elige si el carro también se usa fuera de Uber");
                    double percent=number(uberPercent,"Elige o escribe qué porcentaje de los km es para Uber");if(percent<1 || percent>100)throw new IllegalArgumentException("El porcentaje debe estar entre 1 y 100");
                }
                if(step==3 && vehicle.needsCharge()){double km=number(remainingKm,"Escribe los km eléctricos que quedan ahora");if(km<0 || km>1000)throw new IllegalArgumentException("Revisa los km restantes (0 a 1000)");}
                advance();
            }catch(IllegalArgumentException e){FormUi.error(this,e.getMessage());}
        });
        next.setTextColor(Color.WHITE);next.setBackground(Ui.shape(Ui.TEAL,12,this));
        FormUi.button(p,"Atrás",this::back);
    }
    private static double number(String value,String missing){
        if(value.isBlank())throw new IllegalArgumentException(missing);
        try{double n=Double.parseDouble(value.trim().replace(',','.'));if(!Double.isFinite(n))throw new NumberFormatException();return n;}
        catch(NumberFormatException e){throw new IllegalArgumentException("Escribe un número válido");}
    }
    private DriverConfig build(){
        return OnboardingProfiles.build(vehicle,ownership,ownership==OnboardingProfiles.Ownership.OWNED?0:number(payment,"Escribe tu pago"),
                number(weeklyHours,"Escribe tus horas"),rentalIncludesUpkeep,charging,vehicle.needsCharge()?number(remainingKm,"Escribe la autonomía restante"):0,
                strategy,use,number(uberPercent,"Elige el uso del carro"),previous,System.currentTimeMillis());
    }
    private void summary(){
        final DriverConfig c;
        try{c=build();}catch(IllegalArgumentException e){step=0;render();FormUi.error(this,e.getMessage());return;}
        LinearLayout p=page("Tu perfil está listo","Revisa lo esencial. Los detalles ya están cargados y los puedes afinar después.");
        FormUi.text(p,vehicle.title,21,true);
        String ownershipLabel=ownership==OnboardingProfiles.Ownership.OWNED?"Carro pagado":ownership==OnboardingProfiles.Ownership.FINANCED?"Mensualidad: "+Ui.amount(c.carPaymentMonthly):"Renta semanal: "+Ui.amount(number(payment,"Revisa la renta"));
        FormUi.text(p,ownershipLabel+"\nTrabajo previsto: "+weeklyHours+" h por semana"
                +"\n"+(vehicle.needsCharge()?"Carga: "+Ui.amount(c.electricityPrice)+"/kWh · quedan "+FormUi.format(c.remainingElectricKm)+" km eléctricos":"Gasolina estimada: "+Ui.amount(c.fuelPrice)+"/L")
                +"\nPerfil "+strategy.title.toLowerCase(java.util.Locale.ROOT)+": mínimo "+Ui.amount(c.minHourly)+"/h · meta "+Ui.amount(c.targetHourly)+"/h",16,false);
        FormUi.text(p,"Uso para Uber: "+FormUi.format(c.uberUsePercent)+"% de los km"
                +"\nGastos fijos asignados a Uber: "+Ui.amount(c.allocatedFixedMonthly())+"/mes",16,true);
        FormUi.text(p,"Reservamos "+Ui.amount(c.fixedMonthly-c.carPaymentMonthly)+" al mes para seguro, teléfono y otros gastos, además de mantenimiento y llantas. Es una estimación inicial que conviene ajustar a tus gastos.",14,false);
        FormUi.text(p,"Semáforo del viaje: aporte antes de fijos. También verás el saldo tras fijos. Abre Plan del mes para comprobar si las horas previstas alcanzan; aquí no se estiman ingresos futuros.",14,true);
        if(ownership!=OnboardingProfiles.Ownership.OWNED)FormUi.text(p,"El saldo tras fijos incluye la parte del pago asignada a Uber. El pago completo sigue pendiente: "+Ui.amount(c.carPaymentMonthly)+"/mes. Con financiamiento, el aporte del viaje reserva desgaste; el saldo tras pagos usa la mensualidad en su lugar y no es utilidad contable.",14,false);
        if(vehicle.needsCharge())FormUi.text(p,"Recuerda actualizar la autonomía restante conforme manejes.",14,true);
        FormUi.button(p,"Ver lo que incluye el perfil",()->showDetails(c));
        if(!previous.vehicle.equals(new DriverConfig().vehicle))FormUi.text(p,"Al empezar, este perfil reemplaza tu vehículo, costos y metas actuales. Revisa los importes antes de continuar.",14,true);
        Button save=FormUi.button(p,"Empezar con costos estimados",()->{
            try{ConfigStore.saveOnboarding(this,build(),expectedRevision);setResult(RESULT_OK);finish();}
            catch(IllegalArgumentException | IllegalStateException e){FormUi.error(this,e.getMessage());}
        });
        save.setTextColor(Color.WHITE);save.setBackground(Ui.shape(Ui.TEAL,12,this));
        FormUi.button(p,"Atrás",this::back);
        FormUi.button(p,"Cancelar",this::cancel);
    }
    private void showDetails(DriverConfig c){
        String details="Supuestos del perfil, versión "+c.presetVersion+". No son precios cotizados ni mediciones de tu carro."
                +"\n\nGasolina: "+Ui.amount(c.fuelPrice)+"/L · rendimiento estimado "+FormUi.format(c.kmPerLiter)+" km/L"
                +(vehicle.needsCharge()?"\nElectricidad: "+Ui.amount(c.electricityPrice)+"/kWh · consumo estimado "+FormUi.format(c.kwhPer100Km)+" kWh/100 km":"")
                +"\nServicio: "+Ui.amount(c.maintenanceCost)+" cada "+FormUi.format(c.maintenanceIntervalKm)+" km"
                +"\nLlantas: "+Ui.amount(c.tiresCost)+" cada "+FormUi.format(c.tiresLifeKm)+" km"
                +"\nDesgaste / depreciación por uso: "+Ui.amount(c.wearPerKm)+"/km"
                +"\nReserva de desgaste para comparar viajes financiados: "+Ui.amount(c.financedWearRate())+"/km"
                +"\nGastos fijos compartidos, incluyendo pago del carro: "+Ui.amount(c.fixedMonthly)+"/mes"
                +"\nGastos exclusivos de Uber: "+Ui.amount(c.uberOnlyFixedMonthly)+"/mes"
                +"\nParte asignada a Uber: "+Ui.amount(c.allocatedFixedMonthly())+"/mes"
                +"\nPor km: mínimo "+Ui.amount(c.minPerKm)+" · meta "+Ui.amount(c.targetPerKm)
                +"\nRecogida máxima: "+FormUi.format(c.maxPickupMin)+" min o "+FormUi.format(c.maxPickupKm)+" km"
                +"\nEspera estimada: "+FormUi.format(c.passengerWaitMin)+" min · escenario extra: "+FormUi.format(c.conservativeExtraMin)+" min"
                +"\nDescuento adicional: "+FormUi.format(c.additionalFeePercent)+"% · extras: "+Ui.amount(c.extrasPerTrip)
                +"\nRegreso: "+(c.returnScenarioEnabled?FormUi.format(c.returnScenarioKm)+" km / "+FormUi.format(c.returnScenarioMin)+" min":c.repositionKm==0 && c.repositionMin==0?"sin considerar":FormUi.format(c.repositionKm)+" km / "+FormUi.format(c.repositionMin)+" min")
                +"\nFiltro de pasajero: "+(c.riderFilter?"activo":"desactivado")+" · zonas: "+(c.zoneFilter?"activo":"desactivado")
                +"\n\nPuedes modificar todos los valores en Ajustes avanzados. Verde no garantiza ingresos ni seguridad.";
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);
        android.widget.TextView text=Ui.text(this,details,15,false);int pad=Ui.dp(this,20);text.setPadding(pad,pad,pad,pad);scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("Costos y reglas precargados").setView(scroll).setPositiveButton("Entendido",null).show();
    }
    private void captureDraft(){
        if(paymentInput!=null)payment=paymentInput.getText().toString();if(hoursInput!=null)weeklyHours=hoursInput.getText().toString();
        if(rangeInput!=null)remainingKm=rangeInput.getText().toString();if(chargingInput!=null)charging=OnboardingProfiles.Charging.values()[chargingInput.getSelectedItemPosition()];
        if(includedInput!=null)rentalIncludesUpkeep=includedInput.isChecked();
        if(useInput!=null)uberPercent=useInput.getText().toString();
    }
    private void advance(){captureDraft();step++;render();}
    private void back(){captureDraft();if(step>0){step--;render();}else cancel();}
    @Override protected void onDestroy(){getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);super.onDestroy();}
    private void cancel(){setResult(RESULT_CANCELED);finish();}
    @Override protected void onSaveInstanceState(Bundle out){
        captureDraft();out.putInt("step",step);out.putString("vehicle",vehicle.name());out.putString("ownership",ownership.name());
        out.putString("charging",charging.name());out.putString("strategy",strategy.name());out.putString("payment",payment);
        out.putString("hours",weeklyHours);out.putString("range",remainingKm);out.putBoolean("included",rentalIncludesUpkeep);
        out.putString("use",use==null?"":use.name());out.putString("uberPercent",uberPercent);
        out.putLong("revision",expectedRevision);super.onSaveInstanceState(out);
    }
}
