package mx.tesivil.detector;

import org.junit.Test;
import static org.junit.Assert.*;

public class DarkFormatParserTest {
    private final String sample=OfferParserTest.SAMPLE;
    @Test public void mxnFareAndRateAreSeparateObservedValues() {
        OfferParser.Offer o=OfferParser.parse(sample.replace("$","MXN")).offer;
        assertNotNull(o); assertEquals(6956,o.cents); assertEquals(11.04,o.displayedRate,0);
        assertNull(OfferParser.parse(sample.replace("$","MXN").replace("69.56","6956.00")).offer);
        assertNull(OfferParser.parse(sample.replace("$","MXN").replace("69.56","6956")).offer);
    }
    @Test public void comfortBadgeAndExclusiveHaveTheirOwnCategory() {
        for(String badge:new String[]{"Comfort","Comfort Exclusivo","Comfort\nExclusivo","Exclusivo\nComfort"}) {
            OfferParser.Offer o=OfferParser.parse(sample.replace("UberX",badge)).offer;
            assertNotNull(badge,o); assertEquals("COMFORT",o.serviceType.name());
            assertEquals(badge.contains("Exclusivo"),o.exclusive); assertEquals(6956,o.cents);
        }
        for(String badge:new String[]{"Comfort Electric","Comfort Black","Uber Moto","Uber Eats"})
            assertNull(badge,OfferParser.parse(sample.replace("UberX",badge)).offer);
    }
    @Test public void hourDurationIsConvertedWithoutBorrowingTheLongTripChip() {
        for(String duration:new String[]{"1 h 3 min","1h3min","1 hora 03 minutos"}) {
            OfferParser.Offer o=OfferParser.parse(sample.replace("18 min",duration).replace("Destino de ejemplo","Destino de ejemplo\nViaje largo (45+ min)")).offer;
            assertNotNull(duration,o); assertEquals(63,o.tripMinutes); assertEquals(68,o.totalMinutes());
            assertEquals("destino de ejemplo",o.destinationAddress);
        }
        assertEquals(60,OfferParser.parse(sample.replace("18 min","1 h")).offer.tripMinutes);
        assertEquals(125,OfferParser.parse(sample.replace("5 min","2 h 5 min")).offer.pickupMinutes);
    }
    @Test public void ambiguousOrExcessiveHourDurationsStayIncomplete() {
        for(String duration:new String[]{"1 h 60 min","11 h","1 h min","h 3 min","1 3 min","0 h"})
            assertNull(duration,OfferParser.parse(sample.replace("18 min",duration)).offer);
        assertNull(OfferParser.parse(sample.replace("5 min","5 h")).offer);
    }
    @Test public void endpointIconGlyphsAreConfinedToAnchoredCompleteLegs() {
        OfferParser.Offer o=OfferParser.parse(sample.replace("A 5 min","° All min").replace("Viaje: 18 min","Ở Viaje: lh3 min")).offer;
        assertNotNull(o); assertEquals(11,o.pickupMinutes); assertEquals(63,o.tripMinutes);
        assertNull(OfferParser.parse(sample.replace("A 5 min","Otro A 5 min")).offer);
        assertNull(OfferParser.parse(sample.replace("Viaje: 18 min","Texto Viaje: 18 min")).offer);
        assertNull(OfferParser.parse(sample.replace("A 5 min (1.7 km)","9 A 5 min")).offer);
    }
    @Test public void oneDestinationAnnotationIsRemovedButMultipleStopsAreNotSupported() {
        OfferParser.Offer o=OfferParser.parse(sample.replace("Calle de ejemplo","Calle de ejemplo\n1 destino")).offer;
        assertNotNull(o); assertEquals("calle de ejemplo",o.pickupAddress);
        assertEquals(1,o.destinationNoticeCount);
        for(String annotation:new String[]{"2 destinos","3 destinos","0 destinos","1 parada","2 paradas","1 destino\n1 destino"})
            assertNull(annotation,OfferParser.parse(sample.replace("Calle de ejemplo","Calle de ejemplo\n"+annotation)).offer);
    }
    @Test public void labeledPriorityBonusIsNotASecondFare() {
        OfferParser.Offer o=OfferParser.parse(sample.replace("UberX","Uber Priority Exclusivo").replace("A 5 min","+$10.34 por inicio de viaje prioritario\nA 5 min")).offer;
        assertNotNull(o); assertEquals(6956,o.cents); assertEquals(11.04,o.displayedRate,0);
        assertNull(OfferParser.parse(sample.replace("$69.56","$69.56\nMXN10.34")).offer);
    }
    @Test public void croppedCardDoesNotReceiveAColorEvenWithReadableCurrency() {
        OfferParser.Result r=OfferParser.parse(sample.replace("$","MXN").replace("Viaje disponible",""));
        assertNull(r.offer); assertTrue(r.reason.contains("incompleta"));
    }
    @Test public void comfortAlsoRequiresSeparateConfirmation() {
        OfferConfirmation confirmation=new OfferConfirmation();
        OfferParser.Offer x=OfferParser.parse(sample).offer,comfort=OfferParser.parse(sample.replace("UberX","Comfort")).offer;
        assertNotNull(comfort); assertFalse(confirmation.observe(x,100)); assertTrue(confirmation.observe(x,200));
        assertFalse(confirmation.observe(comfort,300)); assertTrue(confirmation.observe(comfort,400));
    }
    @Test public void destinationNoticeCannotBecomeGreenOrCancelAnExistingRed() {
        String generous=sample.replace("$69.56","$500.00").replace("$11.04/km (estimado)\n","");
        DriverConfig c=new DriverConfig();c.calibrated=true;c.vehicleConfirmed=true;c.energyReviewed=true;c.costsReviewed=true;c.goalsReviewed=true;c.zoneFilter=false;c.riderFilter=false;
        OfferParser.Offer plain=OfferParser.parse(generous).offer;
        OfferParser.Offer notice=OfferParser.parse(generous.replace("Calle de ejemplo","Calle de ejemplo\n1 destino")).offer;
        assertEquals(ScoreEngine.Color.VERDE,ScoreEngine.evaluate(plain,c,java.util.List.of(),java.time.LocalDate.now(),12).color);
        ScoreEngine.Evaluation e=ScoreEngine.evaluate(notice,c,java.util.List.of(),java.time.LocalDate.now(),12);
        assertEquals(ScoreEngine.Color.AMBAR,e.color);assertTrue(String.join(" ",e.reasons).contains("Aviso de destinos"));
        assertEquals(500,e.revenue,0);assertEquals(plain.totalMinutes()+c.passengerWaitMin,e.minutes,0);
        c.riderFilter=true;
        OfferParser.Offer newRider=OfferParser.parse(generous.replace("Calle de ejemplo","Calle de ejemplo\n1 destino").replace("4.83 (370)","5.00 (3)")).offer;
        assertEquals(ScoreEngine.Color.ROJO,ScoreEngine.evaluate(newRider,c,java.util.List.of(),java.time.LocalDate.now(),12).color);
        OfferConfirmation confirmation=new OfferConfirmation();assertFalse(confirmation.observe(plain,100));assertTrue(confirmation.observe(plain,200));
        assertFalse(confirmation.observe(notice,300));assertTrue(confirmation.observe(notice,400));
    }
}
