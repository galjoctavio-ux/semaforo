package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class OfferReadingRegressionTest {
    private final String sample=OfferParserTest.SAMPLE;
    @Test public void damagedMoneyRequestsFreshPixelsWithoutReplacingLetters(){
        for(String money:new String[]{"$1l4.33","$ll4.33","$I14.33","$1|4.33"}){
            OfferParser.Result r=OfferParser.parse(sample.replace("$69.56",money));
            assertNull(r.offer);assertTrue(r.needsMoneyRefinement);assertEquals(1,OfferParser.numericFieldKind(money));
        }
        assertEquals(0,OfferParser.numericFieldKind("$114.33 por inicio de viaje"));
        assertEquals(0,OfferParser.numericFieldKind("$ll.33/km"));
        assertNull(OfferParser.parse(sample.replace("$69.56","$1O4.33")).offer);
        assertNull(OfferParser.parse(sample.replace("$69.56","$69.56\n$1l4.33")).offer);
        assertNull(OfferParser.parse(sample.replace("$69.56","$1l4.33\n$1l8.03")).offer);
    }
    @Test public void damagedJoinedBadgeCanRetryPixelsButIsNotAnAcceptedCategory(){
        OfferParser.Result r=OfferParser.parse(sample.replace("UberX","UberX Exdusivo"));
        assertNull(r.offer);assertTrue(r.needsCardRefinement);
        assertFalse(OfferParser.parse(sample.replace("UberX","Uber Moto")).needsCardRefinement);
        assertFalse(OfferParser.parse("TIPO: UBERX Exdusivo\n$69.56\nA 5 min (1.7 km)\nViaje: 18 min (4.7 km)").needsCardRefinement);
    }
    @Test public void unreadableActionOnlyRequestsPixelsForACompletePotentialCard(){
        OfferParser.Result r=OfferParser.parse(sample.replace("Viaje disponible","AceptarO"));
        assertNull(r.offer);assertTrue(r.needsCardRefinement);assertFalse(r.offerScreen);
        assertFalse(OfferParser.parse("UberX\n$69.56\nConfiguración").needsCardRefinement);
        assertNull(OfferParser.parse(sample.replace("Viaje disponible","Viaje terminado")).offer);
    }
    @Test public void exclusiveAndOcrIconAreNotPriority(){
        for(String badge:new String[]{"UberX Exclusivo","2 UberX\nExclusivo","2 UberX Exclusivo","Exclusivo\n2 UberX"}){
            OfferParser.Offer o=OfferParser.parse(sample.replace("UberX",badge)).offer;
            assertNotNull(o);assertTrue(o.exclusive);assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);
        }
    }
    @Test public void explicitPriorityLabelsAreSupportedWithoutInventingFare(){
        for(String badge:new String[]{"UberX Priority","Uber Priority","Priority","UberX\nPriority"}){
            OfferParser.Offer o=OfferParser.parse(sample.replace("UberX",badge)).offer;
            assertNotNull(badge,o);assertEquals(OfferParser.ServiceType.PRIORITY,o.serviceType);assertEquals(6956,o.cents);
        }
    }
    @Test public void ownOverlayAndOtherScreenAmountsAreExcluded(){
        OfferParser.Offer o=OfferParser.parse("SEMÁFORO\n$7683.17\n$835.13/km\n"+sample+"\n$9900.00").offer;
        assertNotNull(o);assertEquals(6956,o.cents);assertEquals(11.04,o.displayedRate,0);
    }
    @Test public void minuteGlyphRepairIsConfinedToTheCompleteLeg(){
        OfferParser.Offer o=OfferParser.parse(sample.replace("18 min","1l min").replace("Calle de ejemplo","Calle 1l de ejemplo")).offer;
        assertNotNull(o);assertEquals(11,o.tripMinutes);assertTrue(o.pickupAddress.contains("1l"));
    }
    @Test public void missingCentsRequirePixelsAndNeverBecomeWholePesos(){
        for(String money:new String[]{"7706","77 06","120"}){
            OfferParser.Result r=OfferParser.parse(sample.replace("69.56",money));
            assertNull(r.offer);assertTrue(r.needsMoneyRefinement);
            assertNull(OfferParser.parse(sample.replace("69.56",money).replace("$11.04/km (estimado)","")).offer);
        }
    }
    @Test public void explicitButIncoherentHugeFareIsRejectedRatherThanRescaled(){
        OfferParser.Result r=OfferParser.parse(sample.replace("69.56","6956.00"));
        assertNull(r.offer);assertFalse(r.needsMoneyRefinement);assertTrue(r.reason.contains("incoherentes"));
    }
    @Test public void roundingAllowanceDoesNotOverwriteObservedFare(){
        OfferParser.Offer o=OfferParser.parse(sample.replace("69.56","77.06").replace("11.04/km","8.29/km").replace("1.7 km","1.0 km").replace("4.7 km","8.2 km")).offer;
        assertNotNull(o);assertEquals(7706,o.cents);
    }
    @Test public void prioritySurchargeAsASecondUnlabeledAmountRemainsAmbiguous(){
        assertNull(OfferParser.parse(sample.replace("UberX","UberX Priority").replace("$69.56","$69.56\n$5.00")).offer);
    }
    private ScoreEngine.Evaluation evaluate(String badge,String rider){
        DriverConfig c=new DriverConfig();c.calibrated=true;c.vehicleConfirmed=true;c.energyReviewed=true;c.costsReviewed=true;c.goalsReviewed=true;c.zoneFilter=false;
        OfferParser.Offer o=OfferParser.parse(sample.replace("UberX",badge).replace("4.83 (370)",rider)).offer;
        assertNotNull(o);return ScoreEngine.evaluate(o,c,List.of(),LocalDate.now(),12);
    }
    @Test public void categoryAndExclusiveDoNotDoubleCountHigherFareOrChangeScore(){
        ScoreEngine.Evaluation standard=evaluate("UberX","4.92 (149)");
        for(String badge:new String[]{"UberX Priority","UberX Exclusivo","Comfort","Comfort Exclusivo"}){
            ScoreEngine.Evaluation other=evaluate(badge,"4.92 (149)");
            assertEquals(standard.score,other.score);assertEquals(standard.color,other.color);
            assertEquals(standard.revenue,other.revenue,0);assertEquals(standard.margin,other.margin,0);
        }
    }
    @Test public void priorityCannotOverrideNewRiderRejection(){
        ScoreEngine.Evaluation e=evaluate("UberX Priority","5.00 (3)");
        assertTrue(e.riderBlocked);assertEquals(ScoreEngine.Color.ROJO,e.color);
    }
    @Test public void changingPriorityRequiresItsOwnConfirmationButExclusiveDoesNotChangeIdentity(){
        OfferParser.Offer x=OfferParser.parse(sample).offer,priority=OfferParser.parse(sample.replace("UberX","Priority")).offer;
        OfferConfirmation confirmation=new OfferConfirmation();assertFalse(confirmation.observe(x,100));assertTrue(confirmation.observe(x,200));
        assertFalse(confirmation.observe(priority,300));assertTrue(confirmation.observe(priority,400));
        assertEquals(x.key(),OfferParser.parse(sample.replace("UberX","UberX Exclusivo")).offer.key());
    }
}
