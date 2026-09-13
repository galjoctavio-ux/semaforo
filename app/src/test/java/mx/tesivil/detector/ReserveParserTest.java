package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class ReserveParserTest {
    private static final String CARD="Reservar UberX\n$265.62\n4.91 (74)\n° A 24 min y (14,4 km)\nCalle Privada Ejemplo\no Viaje: 29 min (21.4 km)\nAvenida Destino Ejemplo\n9 Reserva\nViaje disponible";
    private OfferParser.Offer parse(String s){OfferParser.Result r=OfferParser.parse(s);assertNotNull(r.reason,r.offer);return r.offer;}
    private DriverConfig reviewed(){DriverConfig c=new DriverConfig();c.calibrated=true;c.vehicleConfirmed=true;c.energyReviewed=true;c.costsReviewed=true;c.goalsReviewed=true;c.zoneFilter=false;return c;}
    private ScoreEngine.Evaluation evaluate(OfferParser.Offer o,DriverConfig c){return ScoreEngine.evaluate(o,c,List.of(),LocalDate.of(2026,9,13),12);}
    @Test public void reservedCardPreservesVisibleTotalsAndAbsentRate(){
        OfferParser.Offer o=parse(CARD);assertTrue(o.reserved);assertFalse(o.exclusive);
        assertEquals(OfferParser.ServiceType.UBER_X,o.serviceType);assertEquals("UberX · Reserva",o.typeLabel());
        assertEquals(26562,o.cents);assertEquals(24,o.pickupMinutes);assertEquals(29,o.tripMinutes);
        assertEquals(53,o.totalMinutes());assertEquals(35.8,o.totalKm(),.001);assertNull(o.displayedRate);
        assertEquals(4.91,o.rider.rating,.001);assertEquals(Integer.valueOf(74),o.rider.count);
        assertEquals("avenida destino ejemplo",o.destinationAddress);
    }
    @Test public void pickupConjunctionIsOptionalAndUnitsRemainExplicit(){
        assertEquals(24,parse(CARD.replace("min y","min")).pickupMinutes);
        assertEquals(24,parse(CARD.replace("min y","minutos y")).pickupMinutes);
        assertEquals(.4,parse(CARD.replace("14,4 km","400 m")).pickupKm,.001);
        assertEquals(63,parse(CARD.replace("24 min y","1 h 3 min y")).pickupMinutes);
    }
    @Test public void unrecognizedSeparatorsAndMalformedLegsAreRejected(){
        for(String replacement:new String[]{"24 min aproximadamente (14.4 km)","24 min yy (14.4 km)","24 min y 14.4 km","1 h 60 min y (14.4 km)","24 min y (14.4 mi)"})
            assertNull(replacement,OfferParser.parse(CARD.replace("24 min y (14,4 km)",replacement)).offer);
    }
    @Test public void reservationDoesNotRelaxMoneyOrCompleteCardRequirements(){
        assertNull(OfferParser.parse(CARD.replace("Viaje disponible","")).offer);
        assertNull(OfferParser.parse(CARD.replace("265.62","26562")).offer);
        assertNull(OfferParser.parse(CARD.replace("$265.62","$265.62\n$10.00")).offer);
    }
    @Test public void unsupportedReservedCategoriesAreNotSilentlyTreatedAsX(){
        for(String category:new String[]{"Reservar UberXL","Reservar Comfort","Reservar UberX VIP","UberX Reserve"})
            assertNull(category,OfferParser.parse(CARD.replace("Reservar UberX",category)).offer);
    }
    @Test public void streetNamesDoNotCreateReservationsOrDisappear(){
        OfferParser.Offer o=parse(CARD.replace("Reservar UberX","UberX").replace("9 Reserva","Calle La Reserva, Zapopan"));
        assertFalse(o.reserved);assertTrue(o.destinationAddress.contains("calle la reserva"));
        assertTrue(parse(CARD.replace("Avenida Destino Ejemplo","Calle La Reserva, Zapopan")).destinationAddress.contains("calle la reserva"));
    }
    @Test public void switchingToReservationRequiresFreshConfirmation(){
        OfferConfirmation c=new OfferConfirmation();OfferParser.Offer immediate=parse(CARD.replace("Reservar UberX","UberX")),reserved=parse(CARD);
        assertFalse(c.observe(immediate,100));assertTrue(c.observe(immediate,520));
        assertNotEquals(immediate.key(),reserved.key());assertFalse(c.observe(reserved,940));assertTrue(c.observe(reserved,1360));
    }
    @Test public void reservationCapsGreenWithoutInventingFeesOrWaiting(){
        String profitable=CARD.replace("24 min y (14,4 km)","2 min y (1.0 km)").replace("265.62","500.00");
        DriverConfig c=reviewed();ScoreEngine.Evaluation reserve=evaluate(parse(profitable),c),immediate=evaluate(parse(profitable.replace("Reservar UberX","UberX")),c);
        assertEquals(ScoreEngine.Color.VERDE,immediate.color);assertEquals(ScoreEngine.Color.AMBAR,reserve.color);
        assertEquals(immediate.revenue,reserve.revenue,0);assertEquals(500,reserve.revenue,0);
        assertEquals(immediate.minutes,reserve.minutes,0);assertEquals(immediate.margin,reserve.margin,0);
        assertTrue(reserve.reasons.stream().anyMatch(s->s.contains("espera previa por verificar")));
    }
    @Test public void reservationKeepsLongPickupAndNewRiderRed(){
        ScoreEngine.Evaluation longPickup=evaluate(parse(CARD),reviewed());assertEquals(ScoreEngine.Color.ROJO,longPickup.color);
        assertTrue(longPickup.reasons.contains("Recogida fuera de tus límites"));
        String newer=CARD.replace("24 min y (14,4 km)","2 min y (1.0 km)").replace("265.62","500.00").replace("4.91 (74)","5.00 (3)");
        ScoreEngine.Evaluation newRider=evaluate(parse(newer),reviewed());assertTrue(newRider.riderBlocked);assertEquals(ScoreEngine.Color.ROJO,newRider.color);
    }
}
