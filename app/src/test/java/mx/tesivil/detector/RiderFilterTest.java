package mx.tesivil.detector;
import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class RiderFilterTest {
    @Test public void visibleNewBadgeBlocksEvenIfCounterConflicts(){RiderProfile r=new RiderProfile(4.92,149,true,true,"ADJACENT_TO_RATING");ScoreEngine.Evaluation e=evaluate(r,config());assertEquals(ScoreEngine.Color.ROJO,e.color);assertTrue(e.reason().contains("NUEVO"));assertTrue(e.riderUnknown);}
    private ScoreEngine.Evaluation evaluate(RiderProfile r,DriverConfig c) {
        return ScoreEngine.evaluate(new OfferParser.Offer(100000,1,1,10,4,null,"","",r),c,List.of(),LocalDate.now(),12);
    }
    private DriverConfig config(){DriverConfig c=new DriverConfig();c.zoneFilter=false;c.calibrated=true;c.vehicleConfirmed=true;c.energyReviewed=true;c.costsReviewed=true;c.goalsReviewed=true;return c;}
    private RiderProfile profile(Double rating,Integer count){return new RiderProfile(rating,count,false,false,"ADJACENT_TO_RATING");}
    @Test public void explicitNewOverridesHighFareAndReviewedCosts(){ScoreEngine.Evaluation e=evaluate(new RiderProfile(null,null,true,false,"NOT_VISIBLE"),config());assertEquals(ScoreEngine.Color.ROJO,e.color);assertTrue(e.riderBlocked);assertTrue(e.reason().contains("NUEVO"));assertTrue(e.score<45);}
    @Test public void fiveStarsCannotOverrideNewByCount(){for(int n:new int[]{0,1,4})assertEquals(ScoreEngine.Color.ROJO,evaluate(profile(5d,n),config()).color);}
    @Test public void unknownCountIsNotZeroOrNew(){ScoreEngine.Evaluation e=evaluate(profile(5d,null),config());assertEquals(ScoreEngine.Color.AMBAR,e.color);assertFalse(e.riderBlocked);assertTrue(e.riderUnknown);}
    @Test public void absentAndInvalidDataPreventGreen(){for(RiderProfile r:new RiderProfile[]{RiderProfile.unknown(),profile(null,149),profile(8d,149),profile(Double.NaN,149),profile(4.9,-1)}){ScoreEngine.Evaluation e=evaluate(r,config());assertEquals(ScoreEngine.Color.AMBAR,e.color);assertTrue(e.riderUnknown);assertFalse(e.riderBlocked);}}
    @Test public void lowRatingBlocksDespiteLargeCounter(){assertEquals(ScoreEngine.Color.ROJO,evaluate(profile(4.69,391),config()).color);}
    @Test public void exactMinimumIsCautionAndExactTargetsPermitGreen(){assertEquals(ScoreEngine.Color.AMBAR,evaluate(profile(4.7,20),config()).color);assertEquals(ScoreEngine.Color.VERDE,evaluate(profile(4.85,20),config()).color);}
    @Test public void countBoundaryFiveIsNotNewAndTwentyEstablished(){ScoreEngine.Evaluation e=evaluate(profile(4.92,5),config());assertEquals(ScoreEngine.Color.AMBAR,e.color);assertFalse(e.riderBlocked);assertTrue(e.riderCaution);assertEquals(ScoreEngine.Color.VERDE,evaluate(profile(4.92,20),config()).color);}
    @Test public void userCanChangeNewThresholdOrDisableBlock(){DriverConfig c=config();c.newRiderMinCount=10;assertEquals(ScoreEngine.Color.ROJO,evaluate(profile(4.92,5),c).color);c.blockNewRider=false;assertEquals(ScoreEngine.Color.AMBAR,evaluate(profile(4.92,5),c).color);assertEquals(ScoreEngine.Color.AMBAR,evaluate(new RiderProfile(null,null,true,false,"NOT_VISIBLE"),c).color);}
    @Test public void disabledFilterDoesNotEnforcePassengerRules(){DriverConfig c=config();c.riderFilter=false;assertEquals(ScoreEngine.Color.VERDE,evaluate(new RiderProfile(null,null,true,false,"NOT_VISIBLE"),c).color);}
    @Test public void riderPenaltyAppliesOnceAndDoesNotChangeEconomicArithmetic(){DriverConfig c=config();c.greenScore=99;ScoreEngine.Evaluation good=evaluate(profile(4.92,149),c),caution=evaluate(profile(4.8,10),c);assertEquals(good.economyScore,caution.economyScore);assertEquals(good.margin,caution.margin,0);assertEquals(good.economyScore-10,caution.score);assertEquals(ScoreEngine.Color.AMBAR,caution.color);}
    @Test public void newBlockSurvivesOtherCautionsAndUnknownZones(){DriverConfig c=config();c.zoneFilter=true;c.calibrated=false;ScoreEngine.Evaluation e=evaluate(profile(4.8,1),c);assertEquals(ScoreEngine.Color.ROJO,e.color);assertTrue(e.reason().contains("Usuario nuevo"));}
    @Test public void profileRejectsContradictoryThresholds(){DriverConfig c=config();c.establishedRiderCount=4;assertNotNull(c.validate());c.establishedRiderCount=20;c.newRiderMinCount=1.5;assertNotNull(c.validate());c.newRiderMinCount=5;c.minRiderRating=5;c.goodRiderRating=4.85;assertNotNull(c.validate());}
}
