package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class ScoreEngineTest {
    private static final LocalDate DAY=LocalDate.of(2026,9,12);
    private OfferParser.Offer sample(long cents){return new OfferParser.Offer(cents,14,6.3,18,4,null,"Calle Uno, Colonia Ejemplo, Zapopan","Calle Dos, Colonia Destino, Zapopan");}
    private DriverConfig config(){DriverConfig c=new DriverConfig();c.calibrated=true;c.zoneFilter=false;c.riderFilter=false;return c;}
    private ScoreEngine.Evaluation evaluate(DriverConfig c){return ScoreEngine.evaluate(sample(9831),c,List.of(),DAY,12);}
    private ZoneRule rule(String name,ZoneRule.Action action){ZoneRule r=new ZoneRule();r.neighborhood=name;r.municipality="Zapopan";r.action=action;r.reviewedOn=DAY.toString();return r;}
    @Test public void defaultCostsAreExplicitAndArithmeticMatches(){
        DriverConfig c=new DriverConfig();assertNull(c.validate());ScoreEngine.Evaluation e=evaluate(c);
        assertEquals(10.3,e.km,.0001);assertEquals(35,e.minutes,.0001);assertEquals(0,e.energyCost,.0001);
        assertEquals(15.45,e.upkeepCost,.0001);assertEquals(10.9375,e.fixedCost,.0001);
        assertEquals(71.9225,e.margin,.0001);assertEquals(123.295714,e.hourly,.0001);
        assertEquals(105.54,e.conservativeHourly,.001);assertNotEquals(ScoreEngine.Color.VERDE,e.color);
    }
    @Test public void depletedPhevUsesOnlyRemainingElectricKm(){
        DriverConfig c=config();c.remainingElectricKm=4;c.electricityPrice=2;
        ScoreEngine.Evaluation e=evaluate(c);assertEquals(4,e.electricKm,0);assertEquals(6.3,e.fuelKm,.0001);
        assertEquals(4*.2*2+6.3/12*25,e.energyCost,.0001);
    }
    @Test public void rejectsNanAndZeroDenominators(){DriverConfig c=config();c.kmPerLiter=0;assertNotNull(c.validate());c.kmPerLiter=12;c.minHourly=Double.NaN;assertEquals(ScoreEngine.Color.GRIS,evaluate(c).color);}
    @Test public void onlyElectricDoesNotInventGasoline(){DriverConfig c=config();c.energyMode="ELECTRICO";c.remainingElectricKm=2;ScoreEngine.Evaluation e=evaluate(c);assertEquals(0,e.fuelKm,0);assertEquals(ScoreEngine.Color.ROJO,e.color);assertTrue(e.reasons.contains("Autonomía eléctrica insuficiente"));}
    @Test public void higherCostsNeverImproveEconomicScore(){DriverConfig c=config();double before=evaluate(c).economyScore;c.wearPerKm+=2;assertTrue(evaluate(c).economyScore<=before);}
    @Test public void pickupAndRepositionIncludedInBothCostsAndTime(){DriverConfig c=config();c.repositionKm=3;c.repositionMin=7;ScoreEngine.Evaluation e=evaluate(c);assertEquals(13.3,e.km,.0001);assertEquals(42,e.minutes,.0001);assertEquals(19.95,e.upkeepCost,.0001);}
    @Test public void explicitFeeIsNotAppliedTwice(){DriverConfig c=config();c.additionalFeePercent=10;assertEquals(88.479,evaluate(c).revenue,.0001);}
    @Test public void missingZoneDataPreventsGreenEvenAtHighFare(){DriverConfig c=config();c.zoneFilter=true;ScoreEngine.Evaluation e=ScoreEngine.evaluate(sample(50000),c,List.of(),DAY,12);assertEquals(ScoreEngine.Color.AMBAR,e.color);assertTrue(e.score<c.greenScore);assertTrue(e.zones.unknown);}
    @Test public void highFareCannotCompensateAvoidedDestination(){DriverConfig c=config();c.zoneFilter=true;ZoneRule r=rule("Colonia Destino",ZoneRule.Action.EVITAR);ScoreEngine.Evaluation e=ScoreEngine.evaluate(sample(50000),c,List.of(r),DAY,12);assertEquals(ScoreEngine.Color.ROJO,e.color);assertTrue(e.score<c.amberScore);assertTrue(e.zones.blocked);}
    @Test public void bothEndpointsReviewedCanAllowGreen(){DriverConfig c=config();c.zoneFilter=true;ScoreEngine.Evaluation e=ScoreEngine.evaluate(sample(50000),c,List.of(rule("Colonia Ejemplo",ZoneRule.Action.REVISADA),rule("Colonia Destino",ZoneRule.Action.REVISADA)),DAY,12);assertEquals(ScoreEngine.Color.VERDE,e.color);assertFalse(e.zones.unknown);}
    @Test public void defaultsNotReviewedCannotGenerateGreen(){DriverConfig c=config();c.calibrated=false;ScoreEngine.Evaluation e=ScoreEngine.evaluate(sample(50000),c,List.of(),DAY,12);assertEquals(ScoreEngine.Color.AMBAR,e.color);}
    @Test public void hardMinimumAppliesEvenWhenWeightIsZero(){DriverConfig c=config();c.weightHourly=0;c.weightKm=100;c.weightPickup=0;c.minHourly=1000;c.targetHourly=1500;assertEquals(ScoreEngine.Color.ROJO,evaluate(c).color);}
    @Test public void pickupHardLimitAppliesDespiteHighScore(){DriverConfig c=config();c.maxPickupMin=10;assertEquals(ScoreEngine.Color.ROJO,ScoreEngine.evaluate(sample(50000),c,List.of(),DAY,12).color);}
    @Test public void cautionReducesScoreAndCapsGreen(){DriverConfig c=config();c.zoneFilter=true;ScoreEngine.Evaluation e=ScoreEngine.evaluate(sample(50000),c,List.of(rule("Colonia Destino",ZoneRule.Action.PRECAUCION),rule("Colonia Ejemplo",ZoneRule.Action.REVISADA)),DAY,12);assertNotEquals(ScoreEngine.Color.VERDE,e.color);assertTrue(e.score<e.economyScore);}
    @Test public void thresholdAndWeightsValidation(){DriverConfig c=config();c.greenScore=c.amberScore;assertNotNull(c.validate());c.greenScore=75;c.weightHourly=c.weightKm=c.weightPickup=0;assertNotNull(c.validate());}
    @Test public void extremeDirectInputsFailClosed(){DriverConfig c=config();OfferParser.Offer huge=new OfferParser.Offer(10000,1,1,10,Double.MAX_VALUE,null);assertEquals(ScoreEngine.Color.GRIS,ScoreEngine.evaluate(huge,c,List.of(),DAY,12).color);c.hoursMonthly=Double.MIN_VALUE;assertNotNull(c.validate());}
}
