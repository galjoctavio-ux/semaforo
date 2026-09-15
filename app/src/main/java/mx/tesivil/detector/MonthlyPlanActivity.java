package mx.tesivil.detector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ScrollView;

/** Shared-car setup and a cash budget, saved only after an explicit button press. */
public final class MonthlyPlanActivity extends Activity {
    private DriverConfig config;
    private EditText hours,percent,rate;
    private Spinner use,basis;
    private TextView output,costs;
    private long expectedRevision;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(Diagnostics.running){finish();return;}
        config=ConfigStore.load(this);expectedRevision=config.revision;
        LinearLayout p=FormUi.page(this,"Plan del mes","Comprueba si las horas previstas alcanzan para los gastos asignados a Uber. Es un supuesto, no una promesa de ingresos.");
        use=FormUi.spinner(p,"Uso del carro",new String[]{"Sin indicar · conservar reparto actual","Solo para Uber","También personal u otro trabajo"},config.carUse.equals("EXCLUSIVE")?1:config.carUse.equals("MIXED")?2:0);
        percent=FormUi.input(p,"Porcentaje de los km para Uber",FormUi.format(config.uberUsePercent),true);
        FormUi.text(p,"Reparte solo fijos compartidos. Mantenimiento, llantas y desgaste siguen por km de Uber.",13,false);
        if(config.carUse.equals("UNSET") && !config.presetAccepted)FormUi.text(p,"Si tus fijos actuales ya representan solo la parte de Uber, conserva 100% o captura primero el gasto total compartido. Evita repartir el mismo gasto dos veces.",13,true);
        hours=FormUi.input(p,"Horas de Uber por semana, incluyendo espera",FormUi.format(config.hoursMonthly/OnboardingProfiles.WEEKS_PER_MONTH),true);
        basis=FormUi.spinner(p,"Cómo comparar cada viaje",new String[]{"Aporte antes de gastos fijos","Saldo después de gastos fijos y pagos"},config.tripBasis()?0:1);
        FormUi.text(p,"Los mismos mínimos se comparan con la base elegida; verás ambos importes.",13,false);
        FormUi.text(p,"Gastos fijos del perfil",19,true);
        costs=FormUi.text(p,costText(),14,false);
        FormUi.button(p,"Revisar importes de gastos",()->startActivityForResult(new Intent(this,SettingsActivity.class).putExtra("section","vehicle"),1));
        rate=FormUi.input(p,"Aporte para pagos por hora de jornada, MXN",config.monthlyPlanConfigured?FormUi.format(config.plannedContributionHourly):"",true);
        rate.setHint("Vacío = todavía no tengo una estimación");
        FormUi.text(p,"Después de costos por km y extras, antes de fijos. Incluye espera entre ofertas; no copies la tarifa de un solo viaje.",13,false);
        if(config.ownership.equals("FINANCED"))FormUi.text(p,"Aquí usa aporte sin descontar depreciación adicional: el presupuesto descuenta la mensualidad en su lugar.",13,false);
        output=FormUi.text(p,Ui.monthlyPlan(config),16,true);
        output.setTextColor(Ui.color(MonthlyPlan.evaluate(config).color));
        FormUi.button(p,"Guardar y calcular plan",()->save());
        FormUi.button(p,"Cómo se calcula",this::help);
        FormUi.button(p,"Volver",this::finish);
        if(state!=null){
            hours.setText(state.getString("hours"));percent.setText(state.getString("percent"));rate.setText(state.getString("rate"));
            use.setSelection(state.getInt("use"));basis.setSelection(state.getInt("basis"));expectedRevision=state.getLong("revision",expectedRevision);
        }
    }
    private String costText(){return "Compartidos, incluido pago del carro: "+Ui.amount(config.fixedMonthly)+"/mes"
            +"\nExclusivos de Uber: "+Ui.amount(config.uberOnlyFixedMonthly)+"/mes"
            +(config.carPaymentMonthly>0?"\nPago completo del carro: "+Ui.amount(config.carPaymentMonthly)+"/mes · sigue siendo obligación":"")
            +"\n"+config.profileStatus();}
    private void help(){
        String explanation="Un viaje puede aportar dinero aunque las horas del mes no alcancen para pagar el carro. El semáforo compara el viaje; este plan revisa el presupuesto del mes."
                +"\n\nUso para Uber = km de Uber ÷ todos los km del carro. Solo para Uber asigna 100%. Los gastos exclusivos de Uber se cargan completos. Mantenimiento, llantas y desgaste no se reducen otra vez: ya se calculan por los km trabajados."
                +"\n\nAporte para pagos: después de energía, reservas de mantenimiento, llantas, desgaste y extras, antes de fijos e impuestos. Incluye espera entre ofertas. Usa jornadas completas o un supuesto prudente; una oferta no acredita el ingreso de toda la jornada."
                +"\n\nSi estás pagando el carro, este presupuesto de efectivo usa la mensualidad y no descuenta además una reserva de depreciación. El aporte de cada viaje sí reserva desgaste para comparar ofertas. Saldo para pagos y utilidad contable son distintos."
                +"\n\nCampo vacío = desconocido. Un 0 escrito = aporte cero. Cubrir fijos no incluye sueldo objetivo ni impuestos. El dinero reservado para mantenimiento no es efectivo libre."
                +"\n\nCompartir gastos no elimina el pago completo ni demuestra que otro ingreso pagará el resto. Si Uber tiene que pagar todos los fijos del carro, revisa también esa cuenta."
                +"\n\nLos gastos precargados son estimaciones iniciales. Revisa tus importes; no bajes mínimos o porcentajes solo para conseguir verdes.";
        ScrollView scroll=new ScrollView(this);TextView text=Ui.text(this,explanation,15,false);
        int pad=Ui.dp(this,20);text.setPadding(pad,pad,pad,pad);scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("Viaje y presupuesto mensual").setView(scroll).setPositiveButton("Entendido",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==1){config=ConfigStore.load(this);expectedRevision=config.revision;costs.setText(costText());output.setText(Ui.monthlyPlan(config));output.setTextColor(Ui.color(MonthlyPlan.evaluate(config).color));}
    }
    @Override protected void onSaveInstanceState(Bundle state){
        state.putString("hours",hours.getText().toString());state.putString("percent",percent.getText().toString());state.putString("rate",rate.getText().toString());
        state.putInt("use",use.getSelectedItemPosition());state.putInt("basis",basis.getSelectedItemPosition());state.putLong("revision",expectedRevision);
        super.onSaveInstanceState(state);
    }
    private void save(){
        try{
            DriverConfig latest=ConfigStore.load(this);
            if(latest.revision!=expectedRevision)throw new IllegalStateException("Tu perfil cambió. Vuelve a abrir Plan del mes para usar los nuevos gastos.");
            double weekly=FormUi.number(hours);if(weekly<1 || weekly>168)throw new IllegalArgumentException("Escribe de 1 a 168 horas por semana");
            double share=use.getSelectedItemPosition()==1?100:FormUi.number(percent);
            if(use.getSelectedItemPosition()==0 && share!=config.uberUsePercent)throw new IllegalArgumentException("Elige el uso del carro antes de cambiar el reparto");
            config.carUse=use.getSelectedItemPosition()==1?"EXCLUSIVE":use.getSelectedItemPosition()==2?"MIXED":config.carUse;
            config.uberUsePercent=share;config.hoursMonthly=weekly*OnboardingProfiles.WEEKS_PER_MONTH;
            config.evaluationBasis=basis.getSelectedItemPosition()==0?"TRIP":"TOTAL";
            config.monthlyPlanConfigured=!rate.getText().toString().isBlank();
            config.plannedContributionHourly=config.monthlyPlanConfigured?FormUi.number(rate):0;
            String error=config.validate();if(error!=null)throw new IllegalArgumentException(error);
            ConfigStore.save(this,config);expectedRevision=config.revision;Diagnostics.reloadConfig();percent.setText(FormUi.format(share));
            output.setText(Ui.monthlyPlan(config));output.setTextColor(Ui.color(MonthlyPlan.evaluate(config).color));
        }catch(Exception e){FormUi.error(this,e instanceof NumberFormatException?"Revisa las cifras":e.getMessage());}
    }
}
