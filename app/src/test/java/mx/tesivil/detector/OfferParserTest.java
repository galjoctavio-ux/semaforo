package mx.tesivil.detector;

import org.junit.Test;
import static org.junit.Assert.*;

public class OfferParserTest {
    @Test public void pairedBadgeSurvivesOcrStarGlyphOrJoinedIdentityText(){for(String s:new String[]{"t 4.92 (149)","Identidad verificada ● 4.92 (149)","4.92 (149) Identidad verificada"}){RiderProfile r=OfferParser.parse(SAMPLE.replace("4.83 (370)",s)).offer.rider;assertEquals(4.92,r.rating,0);assertEquals(Integer.valueOf(149),r.count);assertFalse(r.ambiguous);}}
    @Test public void readsRatingAndVisibleCounter(){OfferParser.Offer o=OfferParser.parse(SAMPLE).offer;assertEquals(4.83,o.rider.rating,.001);assertEquals(Integer.valueOf(370),o.rider.count);assertEquals("ADJACENT_TO_RATING",o.rider.countSource);}
    @Test public void readsCurrentScreensAndSeparatedBadgeLines(){for(String s:new String[]{"★ 4.92 (149)","4,87\n(391)","Identidad verificada ★ 4.92 (149)"}){RiderProfile r=OfferParser.parse(SAMPLE.replace("4.83 (370)",s)).offer.rider;assertNotNull(r.rating);assertNotNull(r.count);assertFalse(r.ambiguous);}}
    @Test public void readsExplicitNewButNeverDestinationNamedNuevo(){assertTrue(OfferParser.parse(SAMPLE.replace("4.83 (370)","Nuevo")).offer.rider.explicitNew);assertFalse(OfferParser.parse(SAMPLE.replace("4.83 (370)","").replace("Destino de ejemplo","Nuevo")).offer.rider.explicitNew);}
    @Test public void missingCountOrRatingDoesNotBecomeZero(){RiderProfile r=OfferParser.parse(SAMPLE.replace("4.83 (370)","5.00")).offer.rider;assertEquals(5,r.rating,0);assertNull(r.count);assertFalse(r.explicitNew);assertNull(OfferParser.parse(SAMPLE.replace("4.83 (370)","")).offer.rider.rating);}
    @Test public void invalidAndConflictingBadgesAreAmbiguous(){for(String s:new String[]{"8.92 (149)","4.92 (149)\n4.87 (391)","Nuevo\n4.92 (149)"})assertTrue(OfferParser.parse(SAMPLE.replace("4.83 (370)",s)).offer.rider.ambiguous);}
    @Test public void labeledTripsCanBeReadSeparately(){RiderProfile r=OfferParser.parse(SAMPLE.replace("4.83 (370)","4.92\n149 viajes")).offer.rider;assertEquals(4.92,r.rating,0);assertEquals(Integer.valueOf(149),r.count);assertEquals("LABELED_TRIPS",r.countSource);}
    @Test public void ratingDoesNotComeFromEarlierAppOrEndpoint(){RiderProfile r=OfferParser.parse("5.00 (800)\n"+SAMPLE.replace("4.83 (370)","").replace("Destino de ejemplo","4.92 (149)")).offer.rider;assertNull(r.rating);assertNull(r.count);}
    @Test public void extractsBothAddressesSeparatelyWithoutFareOrCta(){
        OfferParser.Offer o=OfferParser.parse(SAMPLE).offer;
        assertEquals("calle de ejemplo",o.pickupAddress);assertEquals("destino de ejemplo",o.destinationAddress);
    }
    public static final String SAMPLE = "UberX\n$69.56\n$11.04/km (estimado)\nIdentidad verificada\n4.83 (370)\n"
            + "A 5 min (1.7 km)\nCalle de ejemplo\nViaje: 18 min (4.7 km)\nDestino de ejemplo\nViaje disponible";
    @Test public void readsSampleWithoutUsingPerKmRateAsFare() {
        OfferParser.Offer offer = OfferParser.parse(SAMPLE).offer;
        assertNotNull(offer); assertEquals(6956, offer.cents);
        assertEquals(5, offer.pickupMinutes); assertEquals(1.7, offer.pickupKm, 0.001);
        assertEquals(18, offer.tripMinutes); assertEquals(4.7, offer.tripKm, 0.001);
        assertEquals(6.4, offer.totalKm(), 0.001); assertEquals(23, offer.totalMinutes());
        assertEquals(11.04, offer.displayedRate, 0.001);
    }
    @Test public void handlesCommaDecimalsAndMeters() {
        OfferParser.Offer offer = OfferParser.parse(SAMPLE.replace("69.56", "69,56").replace("1.7 km", "700 m")).offer;
        assertNotNull(offer); assertEquals(6956, offer.cents); assertEquals(.7, offer.pickupKm, .001);
    }
    @Test public void missingPickupDoesNotBorrowTripDistance() {
        assertNull(OfferParser.parse(SAMPLE.replace("A 5 min (1.7 km)", "A 5 min")).offer);
    }
    @Test public void missingTripDoesNotBorrowPickup() {
        assertNull(OfferParser.parse(SAMPLE.replace("Viaje: 18 min (4.7 km)", "Viaje: 18 min")).offer);
    }
    @Test public void missingFareDoesNotTakeRateOrRating() {
        assertNull(OfferParser.parse(SAMPLE.replace("$69.56\n", "")).offer);
    }
    @Test public void receiptAndPreviousScreenAreNotOffers() {
        assertNull(OfferParser.parse(SAMPLE.replace("Viaje disponible", "Viaje terminado")).offer);
    }
    @Test public void duplicateCardsFailClosed() {
        assertNull(OfferParser.parse(SAMPLE + "\n" + SAMPLE.replace("69.56", "200.00")).offer);
    }
    @Test public void zeroDurationOrImpossibleDistanceFails() {
        assertNull(OfferParser.parse(SAMPLE.replace("18 min", "0 min")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("4.7 km", "4700 km")).offer);
    }
    @Test public void rejectsOtherCategoriesUntilSupported() {
        assertNull(OfferParser.parse(SAMPLE.replace("UberX", "Uber Moto")).offer);
    }
    @Test public void handlesMoneySeparatorsUnambiguously() {
        assertEquals(123456, OfferParser.parseMoney("1,234.56"));
        assertEquals(123456, OfferParser.parseMoney("1.234,56"));
        assertEquals(12000, OfferParser.parseMoney("120"));
    }
    @Test public void blankInputIsNotAnOffer() { assertNull(OfferParser.parse("").offer); assertNull(OfferParser.parse(null).offer); }
    @Test public void parserDoesNotKeepDataFromPreviousOffer() {
        assertNotNull(OfferParser.parse(SAMPLE).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("$69.56", "")).offer);
    }
}
