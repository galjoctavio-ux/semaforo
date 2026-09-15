package mx.tesivil.detector;

import org.junit.Test;
import static org.junit.Assert.*;

public class LocalizedOfferParserTest {
    private static final String SAMPLE="UberX\nExclusiva\n153,05 MXN\nEst. 6,05 MXN/km\nIdentidad verificada\n★ 4,87 (15)\nA 3 min (0.7 km) de distancia\nOrigen de ejemplo\nViaje de 33 min (24.6 km)\nDestino de ejemplo\nAceptar";
    @Test public void localizedCardKeepsFareRateLegsRiderAndExclusiveSeparate(){
        OfferParser.Offer o=OfferParser.parse(SAMPLE).offer;
        assertNotNull(o);assertEquals(15305,o.cents);assertEquals(6.05,o.displayedRate,0);
        assertEquals(3,o.pickupMinutes);assertEquals(.7,o.pickupKm,0);
        assertEquals(33,o.tripMinutes);assertEquals(24.6,o.tripKm,0);
        assertEquals(4.87,o.rider.rating,0);assertEquals(Integer.valueOf(15),o.rider.count);assertFalse(o.rider.ambiguous);
        assertTrue(o.exclusive);assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);assertFalse(o.reserved);
        assertEquals("origen de ejemplo",o.pickupAddress);assertEquals("destino de ejemplo",o.destinationAddress);
    }
    @Test public void equivalentCurrencyAndBadgeSpellingsHaveEquivalentFields(){
        for(String fare:new String[]{"153,05 MXN","153.05MXN","MXN153.05","$153.05","MX $153,05","$153,05 MXN"})
            for(String rate:new String[]{"Est. 6,05 MXN/km","6.05MXN/km","MXN6.05/km","$6.05/km (estimado)"})
                for(String badge:new String[]{"UberX Exclusiva","Exclusiva\nUberX","UberX\nExclusivo"}){
                    OfferParser.Offer o=OfferParser.parse(SAMPLE.replace("153,05 MXN",fare).replace("Est. 6,05 MXN/km",rate).replace("UberX\nExclusiva",badge)).offer;
                    assertNotNull(fare+" / "+rate+" / "+badge,o);assertEquals(15305,o.cents);assertEquals(6.05,o.displayedRate,0);assertTrue(o.exclusive);
                }
    }
    @Test public void currencyIsRequiredAndAmbiguousMoneyNeverBecomesAnOffer(){
        for(String fare:new String[]{"153,05","153,05 USD","15305 MXN","153,0 MXN","153,05 MXN + 10,00 MXN","1,53,05 MXN","153,05 MXN\n$10.00","153,05 MXN\n15l,05 MXN"})
            assertNull(fare,OfferParser.parse(SAMPLE.replace("153,05 MXN",fare)).offer);
        OfferParser.Result damaged=OfferParser.parse(SAMPLE.replace("153,05 MXN","15l,05 MXN"));
        assertNull(damaged.offer);assertTrue(damaged.needsMoneyRefinement);
    }
    @Test public void suffixCurrencyParticipatesInTheSameConsistencyChecks(){
        assertNull(OfferParser.parse(SAMPLE.replace("6,05 MXN/km","0,60 MXN/km")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("Est. 6,05 MXN/km","Est. 6,05 MXN/km\n$7.00/km")).offer);
        assertEquals(1,OfferParser.numericFieldKind("153,05 MXN"));
        assertEquals(1,OfferParser.numericFieldKind("15l,05 MXN"));
        assertEquals(2,OfferParser.numericFieldKind("Est. 6,05 MXN/km"));
        assertEquals(3,OfferParser.numericFieldKind("A 3 min (0.7 km) de distancia"));
        assertEquals(4,OfferParser.numericFieldKind("Viaje de 33 min (24.6 km)"));
        assertEquals(0,OfferParser.numericFieldKind("+10,34 MXN por inicio de viaje prioritario"));
    }
    @Test public void labelsRemainAnchoredAndIncompleteCardsAreNotEvaluated(){
        assertNull(OfferParser.parse(SAMPLE.replace("Aceptar","")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("Viaje de 33 min","Viaje desde 33 min")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("de distancia","de espera")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("Viaje de 33 min (24.6 km)","Viaje de 33 min")).offer);
        assertNull(OfferParser.parse(SAMPLE.replace("UberX\nExclusiva","UberX Exclusivamente")).offer);
    }
    @Test public void localizedLegsSupportHoursAndLabeledPriorityBonusIsNotAdded(){
        OfferParser.Offer o=OfferParser.parse(SAMPLE.replace("UberX","Uber Priority").replace("A 3 min","+10,34 MXN por inicio de viaje prioritario\nA 3 min").replace("Viaje de 33 min","Viaje de 1 h 3 min").replace("0.7 km","0,7 km")).offer;
        assertNotNull(o);assertEquals(15305,o.cents);assertEquals(63,o.tripMinutes);assertEquals(.7,o.pickupKm,0);
        assertEquals(OfferParser.ServiceType.PRIORITY,o.serviceType);assertTrue(o.exclusive);
    }
    @Test public void interestButtonIsACompleteOfferActionButSimilarTextIsNot(){
        OfferParser.Offer o=OfferParser.parse(SAMPLE.replace("Aceptar","Me interesa")).offer;
        assertNotNull(o);assertEquals(15305,o.cents);
        assertTrue(OfferParser.isCardActionLine("Me interesa"));
        assertFalse(OfferParser.isCardActionLine("Quizá me interesa después"));
        assertNull(OfferParser.parse(SAMPLE.replace("Aceptar","Me interesaría")).offer);
    }
}
