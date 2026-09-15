package mx.tesivil.detector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Destructive test setup belongs exclusively to the owned test emulator. */
@RunWith(AndroidJUnit4.class)
public class OnboardingFlowTest {
    private Context context;
    private void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    @Before public void reset(){
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();
        main(()->{Diagnostics.running=false;Diagnostics.init(context);});
    }
    private Activity wizard(){return InstrumentationRegistry.getInstrumentation().startActivitySync(new Intent(context,OnboardingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
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
    private void click(Activity a,String label){main(()->{View v=find(a.getWindow().getDecorView(),label);assertNotNull(label,v);assertTrue(v.performClick());});InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void enter(Activity a,String label,String value){main(()->{EditText e=input(a.getWindow().getDecorView(),label,EditText.class);assertNotNull(label,e);e.setText(value);});}
    private void accept(Activity a){click(a,"Empezar con costos estimados");}
    private void toGasSummary(Activity a){click(a,"Sedán a gasolina");click(a,"Continuar");enter(a,"Horas de trabajo por semana","20");click(a,"Solo para Uber");click(a,"Continuar");click(a,"Equilibrado");}
    @Test public void gasWizardIsFiveStepsAndWritesOnlyAfterExplicitAcceptance(){
        String before=ConfigStore.json(ConfigStore.load(context)).toString();Activity a=wizard();toGasSummary(a);
        assertEquals(before,ConfigStore.json(ConfigStore.load(context)).toString());
        main(()->assertNotNull(find(a.getWindow().getDecorView(),"Paso 5 de 5")));
        accept(a);DriverConfig c=ConfigStore.load(context);assertTrue(c.onboardingCompleted);assertEquals("sedan",c.presetId);
        assertEquals(20*52d/12,c.hoursMonthly,1e-9);assertFalse(c.costsReviewed);assertFalse(c.energyReviewed);assertTrue(c.pendingProfile().isEmpty());
    }
    @Test public void solarPhevUsesEnteredRemainingRangeAndPreservesItWhenGoingBack(){
        Activity a=wizard();click(a,"PHEV: eléctrico + gasolina");click(a,"Continuar");click(a,"Solo para Uber");click(a,"Continuar");
        main(()->input(a.getWindow().getDecorView(),"¿Cómo cargas tu carro?",Spinner.class).setSelection(2));
        enter(a,"Autonomía eléctrica que queda ahora, km","30");click(a,"Continuar");click(a,"Atrás");
        main(()->{assertEquals("30",input(a.getWindow().getDecorView(),"Autonomía eléctrica que queda ahora, km",EditText.class).getText().toString());assertEquals(2,input(a.getWindow().getDecorView(),"¿Cómo cargas tu carro?",Spinner.class).getSelectedItemPosition());});
        click(a,"Continuar");click(a,"Flexible");main(()->assertNotNull(find(a.getWindow().getDecorView(),"Paso 6 de 6")));
        accept(a);DriverConfig c=ConfigStore.load(context);assertEquals("PHEV",c.energyMode);assertEquals(0,c.electricityPrice,0);
        assertEquals(30,c.remainingElectricKm,0);assertFalse(c.rangeStale(System.currentTimeMillis()));assertEquals(100,c.minHourly,0);
    }
    @Test public void rentalPaymentAndIncludedUpkeepReachTheSavedEngineProfile(){
        Activity a=wizard();click(a,"Compacto a gasolina");click(a,"Lo rento");enter(a,"Renta por semana, MXN","2000");
        main(()->((CheckBox)find(a.getWindow().getDecorView(),"La renta incluye mantenimiento y llantas")).setChecked(true));
        click(a,"Continuar");click(a,"Solo para Uber");click(a,"Continuar");click(a,"Equilibrado");accept(a);
        DriverConfig c=ConfigStore.load(context);assertEquals("RENTED",c.ownership);assertEquals(2000*52d/12,c.carPaymentMonthly,1e-9);
        assertEquals(0,c.wearPerKm,0);assertEquals(0,c.maintenanceCost,0);assertEquals(0,c.tiresCost,0);
    }
    @Test public void cancellingTheDraftPreservesAnExistingPersonalProfile(){
        DriverConfig personal=ConfigStore.load(context);personal.vehicle="Captiva PHEV";personal.energyMode="PHEV";
        personal.electricityPrice=0;personal.remainingElectricKm=74;personal.minHourly=130;ConfigStore.save(context,personal);
        String before=ConfigStore.json(ConfigStore.load(context)).toString();Activity a=wizard();toGasSummary(a);click(a,"Cancelar");
        assertEquals(before,ConfigStore.json(ConfigStore.load(context)).toString());
    }
    @Test public void aConcurrentProfileChangeCannotBeOverwrittenByAnOldDraft(){
        Activity a=wizard();toGasSummary(a);DriverConfig latest=ConfigStore.load(context);latest.fuelPrice=28;ConfigStore.save(context,latest);
        accept(a);assertEquals(28,ConfigStore.load(context).fuelPrice,0);assertFalse(ConfigStore.load(context).onboardingCompleted);main(a::finish);
    }
    @Test public void onlyANewInstallationRequestsAutomaticOnboarding(){
        context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();assertTrue(ConfigStore.needsOnboarding(context));
        ConfigStore.markOnboardingEntry(context);assertFalse(ConfigStore.needsOnboarding(context));
        context.getSharedPreferences("driver_config_v2",0).edit().clear().commit();DriverConfig c=new DriverConfig();c.vehicle="Mi perfil anterior";ConfigStore.save(context,c);
        String before=ConfigStore.json(ConfigStore.load(context)).toString();assertFalse(ConfigStore.needsOnboarding(context));assertEquals(before,ConfigStore.json(ConfigStore.load(context)).toString());
    }
    @Test public void sharedVehicleUsageReachesTheSavedCostsWithoutAnExtraPage(){
        Activity a=wizard();click(a,"Sedán a gasolina");click(a,"Continuar");
        enter(a,"Horas de trabajo por semana","10");click(a,"También uso personal u otro trabajo");
        enter(a,"Porcentaje aproximado de km para Uber","50");click(a,"Continuar");click(a,"Equilibrado");accept(a);
        DriverConfig c=ConfigStore.load(context);assertEquals("MIXED",c.carUse);assertEquals(50,c.uberUsePercent,0);
        assertEquals(1500,c.allocatedFixedMonthly(),0);assertEquals(10000,c.tiresCost,0);assertTrue(c.tripBasis());
    }
}
