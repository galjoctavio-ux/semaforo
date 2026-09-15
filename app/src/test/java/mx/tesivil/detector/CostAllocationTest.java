package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;
import static mx.tesivil.detector.OnboardingProfiles.*;

public class CostAllocationTest {
    private DriverConfig driver(double weeklyHours,double share){
        DriverConfig c=OnboardingProfiles.build(Vehicle.COMPACT,Ownership.FINANCED,5000,weeklyHours,false,Charging.HOME,0,
                Strategy.BALANCED,share==100?Use.EXCLUSIVE:Use.MIXED,share,new DriverConfig(),System.currentTimeMillis());
        c.zoneFilter=false;c.riderFilter=false;return c;
    }
    private OfferParser.Offer offer(long cents){return new OfferParser.Offer(cents,2,1,25,10,null,"A","B",new RiderProfile(4.95,100,false,false,"TEST"));}
    private ScoreEngine.Evaluation evaluate(DriverConfig c,long cents){return ScoreEngine.evaluate(offer(cents),c,List.of(),LocalDate.now(),12);}
    @Test public void usageShareNeverDiscountsUberMileageCostsAgain(){
        DriverConfig c=driver(10,100);ScoreEngine.Evaluation all=evaluate(c,13000);
        c.carUse="MIXED";c.uberUsePercent=50;ScoreEngine.Evaluation half=evaluate(c,13000);
        assertEquals(all.fixedCost/2,half.fixedCost,1e-9);assertEquals(all.energyCost,half.energyCost,0);
        assertEquals(all.upkeepCost,half.upkeepCost,0);assertEquals(all.financedWearReserve,half.financedWearReserve,0);
        assertEquals(all.contribution,half.contribution,0);assertEquals(all.margin+all.fixedCost/2,half.margin,1e-9);
    }
    @Test public void exclusiveUberExpensesAlwaysRemainFullyAssigned(){
        DriverConfig c=driver(10,25);c.uberOnlyFixedMonthly=1000;
        assertEquals(3000,c.allocatedFixedMonthly(),0);assertEquals(3000/c.hoursMonthly*.5,evaluate(c,13000).fixedCost,1e-9);
        assertEquals(5000,c.carPaymentMonthly,0);
    }
    @Test public void aGoodTripCanPassWhileTheMonthlyBudgetIsRed(){
        DriverConfig c=driver(10,100);ScoreEngine.Evaluation trip=evaluate(c,13000);
        assertEquals(ScoreEngine.Color.VERDE,trip.color);assertEquals(94.87333333,trip.contribution,1e-7);
        assertEquals(189.74666667,trip.contributionHourly,1e-7);assertTrue(trip.hourly<120);
        c.evaluationBasis="TOTAL";assertEquals(ScoreEngine.Color.ROJO,evaluate(c,13000).color);
        c.monthlyPlanConfigured=true;c.plannedContributionHourly=150;
        MonthlyPlan.Result month=MonthlyPlan.evaluate(c);assertEquals(ScoreEngine.Color.ROJO,month.color);
        assertEquals(6500,month.monthlyContribution,1e-8);assertEquals(-1500,month.afterFixed,1e-8);
        assertEquals(12.3076923077,month.breakEvenWeeklyHours,1e-8);
    }
    @Test public void aPositiveTripWithAnAllocatedFixedDeficitRequiresReviewInsteadOfAutomaticRed(){
        DriverConfig c=driver(5,100);ScoreEngine.Evaluation e=evaluate(c,13000);
        assertTrue(e.contribution>0);assertTrue(e.margin<0);assertFalse(e.belowFloor);assertTrue(e.fixedShortfall);
        assertEquals(ScoreEngine.Color.AMBAR,e.color);assertTrue(e.reasons.stream().anyMatch(r->r.contains("no cubre los gastos fijos")));
    }
    @Test public void losingTripsRemainRedRegardlessOfTheLoan(){
        DriverConfig c=driver(5,100);ScoreEngine.Evaluation e=evaluate(c,1000);
        assertTrue(e.contribution<0);assertTrue(e.belowFloor);assertEquals(ScoreEngine.Color.ROJO,e.color);
    }
    @Test public void financialWarningsNeverOverrideAPersonalPassengerBlock(){
        DriverConfig c=driver(5,100);c.riderFilter=true;c.minRiderRating=5;c.goodRiderRating=5;
        ScoreEngine.Evaluation e=evaluate(c,13000);assertTrue(e.riderBlocked);assertTrue(e.fixedShortfall);
        assertEquals(ScoreEngine.Color.ROJO,e.color);
    }
    @Test public void financedVehicleWearRemainsACostOfChoosingAnotherTrip(){
        DriverConfig c=driver(10,100);ScoreEngine.Evaluation e=evaluate(c,13000);
        assertEquals(7.7,e.financedWearReserve,1e-9);assertEquals(e.contribution+7.7-e.fixedCost,e.margin,1e-9);
        int score=e.economyScore;c.financedWearPerKm+=5;assertTrue(evaluate(c,13000).economyScore<score);
        c.evaluationBasis="TOTAL";assertEquals(e.margin,evaluate(c,13000).margin,0);
    }
    @Test public void lowHoursDoNotSilentlyLowerIncomeTargets(){
        DriverConfig few=driver(5,100),many=driver(40,100);
        assertEquals(few.minHourly,many.minHourly,0);assertEquals(few.targetHourly,many.targetHourly,0);
        assertEquals(evaluate(few,13000).contribution,evaluate(many,13000).contribution,0);
    }
    @Test public void tripMinimumFareAndWaitToleranceUseTheChosenBasisWithFees(){
        DriverConfig c=driver(10,50);c.additionalFeePercent=17;
        ScoreEngine.Evaluation e=evaluate(c,30000);long min=Math.round(e.minimumFare*100);
        assertFalse(evaluate(c,min).belowFloor);assertTrue(evaluate(c,min-1).belowFloor);
        c.passengerWaitMin+=e.extraWaitToleranceMin-.00001;assertFalse(evaluate(c,30000).belowFloor);
        c.passengerWaitMin+=.00002;assertTrue(evaluate(c,30000).belowFloor);
    }
    @Test public void returnScenarioUsesContributionAndDoesNotCountRepositionTwice(){
        DriverConfig c=driver(10,50);c.repositionKm=20;c.repositionMin=30;c.returnScenarioEnabled=true;c.returnScenarioKm=4;c.returnScenarioMin=7;
        ScoreEngine.Evaluation e=evaluate(c,30000);
        DriverConfig alternative=driver(10,50);alternative.repositionKm=4;alternative.repositionMin=7;
        ScoreEngine.Evaluation a=evaluate(alternative,30000);
        assertEquals(a.contribution,e.returnContribution,1e-9);assertEquals(a.decisionHourly,e.returnDecisionHourly,1e-9);
        assertEquals(a.margin,e.returnMargin,1e-9);
    }
    @Test public void absentIncomeAssumptionsNeverBecomeZeroOrAForecastFromOneOffer(){
        DriverConfig c=driver(10,100);evaluate(c,100000);
        MonthlyPlan.Result r=MonthlyPlan.evaluate(c);assertFalse(r.known);assertEquals(ScoreEngine.Color.GRIS,r.color);
        assertNull(r.breakEvenHours);assertEquals(184.615384615,r.requiredHourly,1e-8);
    }
    @Test public void sharedAllocationDoesNotPretendTheRemainingCashObligationDisappeared(){
        DriverConfig c=driver(10,50);c.monthlyPlanConfigured=true;c.plannedContributionHourly=150;
        MonthlyPlan.Result r=MonthlyPlan.evaluate(c);assertEquals(2500,r.afterFixed,1e-8);
        assertEquals(4000,r.outsideUber,0);assertEquals(-1500,r.afterAllCarFixed,1e-8);assertEquals(ScoreEngine.Color.AMBAR,r.color);
        assertEquals(5000,c.carPaymentMonthly,0);
    }
    @Test public void zeroKnownContributionDoesNotDivideByZero(){
        DriverConfig c=driver(10,100);c.monthlyPlanConfigured=true;c.plannedContributionHourly=0;
        MonthlyPlan.Result r=MonthlyPlan.evaluate(c);assertEquals(ScoreEngine.Color.ROJO,r.color);assertNull(r.breakEvenHours);
        c.fixedMonthly=0;r=MonthlyPlan.evaluate(c);assertEquals(ScoreEngine.Color.AMBAR,r.color);assertEquals(0,r.breakEvenHours,0);
    }
    @Test public void exerciseScenariosAreReproducible(){
        for(double h:new double[]{5,10,20}){
            DriverConfig c=driver(h,100);c.monthlyPlanConfigured=true;c.plannedContributionHourly=150;
            MonthlyPlan.Result r=MonthlyPlan.evaluate(c);assertEquals(h*52/12*150-8000,r.afterFixed,1e-8);
            System.out.printf(java.util.Locale.US,"EXERCISE weekly=%.0f monthly_hours=%.4f contribution=%.2f fixed=%.2f balance=%.2f budget=%s%n",h,r.hours,r.monthlyContribution,r.fixedMonthly,r.afterFixed,r.color);
        }
    }
    @Test public void invalidAllocationCannotBeAccepted(){
        DriverConfig c=driver(10,100);c.uberUsePercent=50;assertNotNull(c.validate());
        c.carUse="MIXED";c.uberUsePercent=101;assertNotNull(c.validate());c.uberUsePercent=0;assertNotNull(c.validate());
        c.uberUsePercent=50;c.evaluationBasis="MYSTERY";assertNotNull(c.validate());
    }
    @Test public void switchingAnOldFinancedProfileToTripComparisonDoesNotCertifyMissingWearCosts(){
        DriverConfig c=driver(10,100);c.financedWearPerKm=0;
        assertTrue(c.pendingProfile().contains("reserva de desgaste del carro financiado"));
        assertNotEquals(ScoreEngine.Color.VERDE,evaluate(c,13000).color);
        c.evaluationBasis="TOTAL";assertTrue(c.pendingProfile().isEmpty());
    }
}
