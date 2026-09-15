package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;
import static mx.tesivil.detector.OnboardingProfiles.*;

public class OnboardingProfilesTest {
    private DriverConfig build(Vehicle v,Ownership o,double payment,boolean included,Charging charge,double remaining){
        return OnboardingProfiles.build(v,o,payment,40,included,charge,remaining,Strategy.BALANCED,new DriverConfig(),System.currentTimeMillis());
    }
    private ScoreEngine.Evaluation evaluate(DriverConfig c){
        return ScoreEngine.evaluate(new OfferParser.Offer(30000,2,1,20,11,null,"",""),c,List.of(),LocalDate.now(),12);
    }
    @Test public void everyVehicleOwnershipAndStrategyBuildsACompleteValidProfile(){
        for(Vehicle v:Vehicle.values())for(Ownership o:Ownership.values())for(Strategy s:Strategy.values()){
            DriverConfig c=OnboardingProfiles.build(v,o,2000,40,false,Charging.HOME,25,s,new DriverConfig(),System.currentTimeMillis());
            assertNull(v+"/"+o+"/"+s,c.validate());assertTrue(c.pendingProfile().isEmpty());assertEquals(s.minHourly,c.minHourly,0);
            assertTrue(c.onboardingCompleted);assertTrue(c.presetAccepted);assertEquals(VERSION,c.presetVersion);
        }
    }
    @Test public void choosingAClassAloneDoesNotConfirmCostsOrElectricRange(){
        DriverConfig c=Vehicle.PHEV.profile();assertNull(c.validate());assertFalse(c.presetAccepted);
        assertFalse(c.energyReviewed);assertFalse(c.costsReviewed);assertEquals(0,c.remainingElectricKm,0);assertTrue(c.rangeStale(System.currentTimeMillis()));
    }
    @Test public void freeChargeIsExplicitAndOnlyCoversRemainingPhevRange(){
        DriverConfig c=build(Vehicle.PHEV,Ownership.OWNED,0,false,Charging.FREE,5);
        ScoreEngine.Evaluation e=evaluate(c);assertEquals(5,e.electricKm,0);assertEquals(7,e.fuelKm,0);
        assertEquals(7d/12*25,e.energyCost,1e-9);
        c.remainingElectricKm=100;assertEquals(0,evaluate(c).energyCost,0);
        c.remainingElectricKm=0;assertEquals(12d/12*25,evaluate(c).energyCost,0);
    }
    @Test public void paidChargingNeverDefaultsToFree(){
        assertTrue(Vehicle.ELECTRIC.profile().electricityPrice>0);
        DriverConfig c=build(Vehicle.ELECTRIC,Ownership.OWNED,0,false,Charging.HOME,50);
        assertEquals(12*.18*3,evaluate(c).energyCost,1e-9);
    }
    @Test public void financingCountsTheCashPaymentOnceWithoutAdditionalDepreciation(){
        DriverConfig c=build(Vehicle.SEDAN,Ownership.FINANCED,5000,false,Charging.HOME,0);
        assertEquals(8000,c.fixedMonthly,0);assertEquals(5000,c.carPaymentMonthly,0);assertEquals(0,c.wearPerKm,0);
        assertTrue(c.maintenanceCost>0);assertTrue(c.tiresCost>0);
    }
    @Test public void weeklyRentConvertsToMonthlyAndIncludedUpkeepIsNotChargedAgain(){
        DriverConfig c=build(Vehicle.SEDAN,Ownership.RENTED,2000,true,Charging.HOME,0);
        assertEquals(2000*52d/12,c.carPaymentMonthly,1e-9);assertEquals(3000+2000*52d/12,c.fixedMonthly,1e-9);
        assertEquals(0,c.wearPerKm,0);assertEquals(0,c.maintenanceCost,0);assertEquals(0,c.tiresCost,0);
        DriverConfig separate=build(Vehicle.SEDAN,Ownership.RENTED,2000,false,Charging.HOME,0);
        assertTrue(separate.maintenanceCost>0);assertTrue(separate.tiresCost>0);
    }
    @Test public void hoursAllocateFixedCostsUsingExpectedOnlineHours(){
        DriverConfig c=OnboardingProfiles.build(Vehicle.SEDAN,Ownership.OWNED,0,20,false,Charging.HOME,0,Strategy.BALANCED,new DriverConfig(),System.currentTimeMillis());
        assertEquals(20*52d/12,c.hoursMonthly,1e-9);
        assertEquals(3000/c.hoursMonthly*25d/60,evaluate(c).fixedCost,1e-9);
    }
    @Test public void acceptingEstimatesDoesNotClaimMeasuredOrReviewedCosts(){
        DriverConfig c=build(Vehicle.COMPACT,Ownership.OWNED,0,false,Charging.HOME,0);
        assertFalse(c.calibrated);assertFalse(c.energyReviewed);assertFalse(c.costsReviewed);assertTrue(c.usesPresetEstimates());
        assertTrue(c.profileStatus().contains("estimados"));assertFalse(c.profileStatus().contains("revisados por ti"));
        ScoreEngine.Evaluation e=evaluate(c);assertFalse(e.profilePending);assertTrue(e.alerts.stream().anyMatch(a->a.contains("estimaciones iniciales")));
    }
    @Test public void acceptingAPresetDoesNotCertifyUnknownZonesOrRiders(){
        DriverConfig c=build(Vehicle.COMPACT,Ownership.OWNED,0,false,Charging.HOME,0);
        assertNotEquals(ScoreEngine.Color.VERDE,evaluate(c).color);
        assertTrue(evaluate(c).riderUnknown);assertTrue(evaluate(c).zones.unknown);
    }
    @Test public void existingPersonalFiltersFeesAndReturnScenarioArePreserved(){
        DriverConfig old=new DriverConfig();old.minRiderRating=4.9;old.goodRiderRating=4.95;old.blockNewRider=false;old.newRiderMinCount=8;
        old.zoneFilter=false;old.additionalFeePercent=3;old.extrasPerTrip=10;old.repositionKm=3;
        old.returnScenarioEnabled=true;old.returnScenarioKm=17;old.returnScenarioMin=35;old.captureWholeScreen=false;
        old.maxPickupMin=8;old.maxPickupKm=4;old.greenScore=80;old.weightHourly=70;
        DriverConfig c=OnboardingProfiles.build(Vehicle.PHEV,Ownership.OWNED,0,40,false,Charging.FREE,30,Strategy.FLEXIBLE,old,System.currentTimeMillis());
        assertEquals(4.9,c.minRiderRating,0);assertFalse(c.blockNewRider);assertEquals(8,c.newRiderMinCount,0);assertFalse(c.zoneFilter);
        assertEquals(3,c.additionalFeePercent,0);assertEquals(10,c.extrasPerTrip,0);assertEquals(3,c.repositionKm,0);
        assertTrue(c.returnScenarioEnabled);assertEquals(17,c.returnScenarioKm,0);assertEquals(35,c.returnScenarioMin,0);assertFalse(c.captureWholeScreen);
        assertEquals(8,c.maxPickupMin,0);assertEquals(4,c.maxPickupKm,0);assertEquals(80,c.greenScore,0);assertEquals(70,c.weightHourly,0);
    }
    @Test public void invalidInputsCannotCreateAnAcceptedProfile(){
        for(double hours:new double[]{0,169,Double.NaN,Double.POSITIVE_INFINITY})expectFailure(()->OnboardingProfiles.build(Vehicle.SEDAN,Ownership.OWNED,0,hours,false,Charging.HOME,0,Strategy.BALANCED,new DriverConfig(),System.currentTimeMillis()));
        for(double amount:new double[]{0,-1,100001,Double.NaN})expectFailure(()->build(Vehicle.SEDAN,Ownership.RENTED,amount,false,Charging.HOME,0));
        for(double range:new double[]{-1,1001,Double.NaN})expectFailure(()->build(Vehicle.PHEV,Ownership.OWNED,0,false,Charging.FREE,range));
    }
    @Test public void legacyProfilesStillRequireTheirSpecificReviews(){
        DriverConfig c=new DriverConfig();c.calibrated=true;assertEquals(4,c.pendingProfile().size());assertFalse(c.onboardingCompleted);
        c.presetAccepted=true;assertNotNull(c.validate());assertEquals(4,c.pendingProfile().size());
    }
    private void expectFailure(Runnable r){try{r.run();fail("Invalid input accepted");}catch(IllegalArgumentException expected){}}
}
