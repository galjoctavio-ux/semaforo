package mx.tesivil.detector;

import org.junit.Test;
import static org.junit.Assert.*;

public class BlackOfferParserTest {
    private static final String CARD="Black\nExclusivo\n$148.13\n$13.59/km (estimado)\nIdentidad verificada\n4.71 (191)\nA 11 min (3.3 km)\nOrigen de ejemplo\nViaje: 20 min (7.6 km)\nDestino de ejemplo\nA 59 min del destino\nAceptar";

    @Test public void blackAndUberBlackAreIndependentSupportedTypes() {
        for(String badge:new String[]{"Black\nExclusivo","Black Exclusivo","Uber Black\nExclusivo"}) {
            OfferParser.Offer o=OfferParser.parse(CARD.replace("Black\nExclusivo",badge)).offer;
            assertNotNull(badge,o);assertEquals(OfferParser.ServiceType.BLACK,o.serviceType);
            assertEquals("Black · Exclusivo",o.typeLabel());assertTrue(o.exclusive);
            assertEquals(14813,o.cents);assertEquals(13.59,o.displayedRate,0);
            assertEquals(11,o.pickupMinutes);assertEquals(3.3,o.pickupKm,0);
            assertEquals(20,o.tripMinutes);assertEquals(7.6,o.tripKm,0);
        }
    }

    @Test public void blackDoesNotReceiveCategoryPointsOrRelaxOtherRules() {
        DriverConfig config=new DriverConfig();config.calibrated=true;config.vehicleConfirmed=true;
        config.energyReviewed=true;config.costsReviewed=true;config.goalsReviewed=true;config.zoneFilter=false;
        OfferParser.Offer black=OfferParser.parse(CARD).offer;
        OfferParser.Offer x=OfferParser.parse(CARD.replace("Black\nExclusivo","UberX\nExclusivo")).offer;
        ScoreEngine.Evaluation blackResult=ScoreEngine.evaluate(black,config,java.util.List.of(),java.time.LocalDate.now(),12);
        ScoreEngine.Evaluation xResult=ScoreEngine.evaluate(x,config,java.util.List.of(),java.time.LocalDate.now(),12);
        assertEquals(xResult.color,blackResult.color);assertEquals(xResult.score,blackResult.score);
        assertEquals(xResult.decisionMargin,blackResult.decisionMargin,0);
    }

    @Test public void similarUnsupportedProductsStayRejected() {
        for(String badge:new String[]{"Black SUV","Comfort Black","Black VIP","Uber Moto"})
            assertNull(badge,OfferParser.parse(CARD.replace("Black\nExclusivo",badge)).offer);
    }
}
