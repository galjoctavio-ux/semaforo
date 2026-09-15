package mx.tesivil.detector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;
import static mx.tesivil.detector.OnboardingProfiles.*;

@RunWith(AndroidJUnit4.class)
public class MonthlyPlanFlowTest {
    private Context context;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void reset(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();
        main(()->{Diagnostics.running=false;Diagnostics.init(context);});
        DriverConfig c=OnboardingProfiles.build(Vehicle.COMPACT,Ownership.FINANCED,5000,10,false,Charging.HOME,0,
                Strategy.BALANCED,Use.EXCLUSIVE,100,ConfigStore.load(context),System.currentTimeMillis());
        c.zoneFilter=false;c.riderFilter=false;ConfigStore.save(context,c);
    }
    private Activity open(){return InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,MonthlyPlanActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private View find(View v,String label){
        if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof TextView && label.contentEquals(((TextView)v).getText()))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}
        return null;
    }
    private <T extends View> T input(View v,String label,Class<T> type){
        if(type.isInstance(v) && label.contentEquals(v.getContentDescription()))return type.cast(v);
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T f=input(((ViewGroup)v).getChildAt(i),label,type);if(f!=null)return f;}
        return null;
    }
    private void enter(Activity a,String label,String value){main(()->{EditText v=input(a.getWindow().getDecorView(),label,EditText.class);assertNotNull(label,v);v.setText(value);});}
    private void select(Activity a,String label,int index){main(()->{Spinner v=input(a.getWindow().getDecorView(),label,Spinner.class);assertNotNull(label,v);v.setSelection(index);});}
    private void save(Activity a){main(()->find(a.getWindow().getDecorView(),"Guardar y calcular plan").performClick());InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    @Test public void aLowHoursBudgetIsRedWithoutChangingTheTripTargets(){
        Activity a=open();enter(a,"Aporte para pagos por hora de jornada, MXN","150");save(a);
        DriverConfig c=ConfigStore.load(context);assertTrue(c.monthlyPlanConfigured);assertEquals(150,c.plannedContributionHourly,0);
        assertEquals(-1500,MonthlyPlan.evaluate(c).afterFixed,1e-8);assertEquals(120,c.minHourly,0);assertEquals(180,c.targetHourly,0);
        assertTrue(c.tripBasis());main(a::finish);
    }
    @Test public void editingSharedUsePreservesThePaymentAndPerKmReserves(){
        DriverConfig before=ConfigStore.load(context);Activity a=open();select(a,"Uso del carro",2);
        enter(a,"Porcentaje de los km para Uber","50");enter(a,"Aporte para pagos por hora de jornada, MXN","150");save(a);
        DriverConfig c=ConfigStore.load(context);assertEquals(4000,c.allocatedFixedMonthly(),0);assertEquals(5000,c.carPaymentMonthly,0);
        assertEquals(before.maintenanceCost,c.maintenanceCost,0);assertEquals(before.financedWearPerKm,c.financedWearPerKm,0);
        assertEquals(2500,MonthlyPlan.evaluate(c).afterFixed,1e-8);assertEquals(-1500,MonthlyPlan.evaluate(c).afterAllCarFixed,1e-8);
        assertEquals(ScoreEngine.Color.AMBAR,MonthlyPlan.evaluate(c).color);main(a::finish);
    }
    @Test public void aBlankIncomeAssumptionStaysUnknownWhileAnExplicitZeroIsKnown(){
        Activity a=open();save(a);assertFalse(ConfigStore.load(context).monthlyPlanConfigured);
        enter(a,"Aporte para pagos por hora de jornada, MXN","0");save(a);
        assertTrue(ConfigStore.load(context).monthlyPlanConfigured);assertEquals(ScoreEngine.Color.ROJO,MonthlyPlan.evaluate(ConfigStore.load(context)).color);main(a::finish);
    }
    @Test public void invalidAllocationCannotOverwriteTheSavedProfile(){
        String before=ConfigStore.json(ConfigStore.load(context)).toString();Activity a=open();select(a,"Uso del carro",2);
        enter(a,"Porcentaje de los km para Uber","101");save(a);
        assertEquals(before,ConfigStore.json(ConfigStore.load(context)).toString());main(a::finish);
    }
    @Test public void diagnosticsRecordBothBasesAndKeepTheFullPaymentCalculation() throws Exception{
        DriverConfig c=ConfigStore.load(context);
        ScoreEngine.Evaluation e=ScoreEngine.evaluate(new OfferParser.Offer(13000,2,1,25,10,null),c,List.of(),LocalDate.now(),12);
        JSONObject j=Diagnostics.evaluationJson(e);assertEquals("TRIP",j.getString("evaluation_basis"));
        assertEquals(e.margin,j.getDouble("estimated_remaining"),0);assertEquals(e.contribution,j.getDouble("trip_contribution"),0);
        assertEquals("Est. $"+Math.round(e.contributionHourly)+"/h",Ui.compact(e));
        c.evaluationBasis="TOTAL";
        ScoreEngine.Evaluation afterFixed=ScoreEngine.evaluate(new OfferParser.Offer(13000,2,1,25,10,null),c,List.of(),LocalDate.now(),12);
        assertEquals("Est. $"+Math.round(afterFixed.hourly)+"/h",Ui.compact(afterFixed));
        assertTrue(Ui.details(e,c).contains("Pago completo del carro"));
        assertTrue(Ui.monthlySummary(c).contains("ingresos por estimar"));
    }
}
