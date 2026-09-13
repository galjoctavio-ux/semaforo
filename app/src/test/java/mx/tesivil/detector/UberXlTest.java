package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class UberXlTest {
    static final String XL="UberXL\n$284.93\n$11.17/km (estimado)\nIdentidad verificada\n4.87 (91)\n"
            +"A 19 min (8.5 km)\nEncino, Base Militar\nViaje: 41 min (17.1 km)\nColonia Belisario, Guadalajara\nViaje disponible";
    @Test public void normalExclusiveAndSpacedXlAreTheirOwnCategory(){
        for(String badge:new String[]{"UberXL","Uber XL","Uber X L","2 UberXL Exclusivo","Exclusivo\nUberXL"}){
            OfferParser.Offer o=OfferParser.parse(XL.replace("UberXL",badge).replace("Viaje disponible","Aceptar")).offer;
            assertNotNull(badge,o);assertEquals(OfferParser.ServiceType.UBER_XL,o.serviceType);
            assertEquals(badge.contains("Exclusivo"),o.exclusive);assertTrue(o.typeLabel().startsWith("UberXL"));
            assertEquals(28493,o.cents);assertEquals(19,o.pickupMinutes);assertEquals(41,o.tripMinutes);
        }
    }
    @Test public void changingPriceRequiresConfirmationAndChangesEconomics(){
        OfferParser.Offer first=OfferParser.parse(XL).offer;
        OfferParser.Offer next=OfferParser.parse(XL.replace("284.93","236.82").replace("11.17/km","9.29/km")).offer;
        assertNotNull(next);assertNotEquals(first.key(),next.key());
        OfferConfirmation c=new OfferConfirmation();assertFalse(c.observe(first,100));assertTrue(c.observe(first,520));
        assertFalse(c.observe(next,940));assertTrue(c.observe(next,1360));
        DriverConfig config=new DriverConfig();
        ScoreEngine.Evaluation a=ScoreEngine.evaluate(first,config,List.of(),LocalDate.now(),12),b=ScoreEngine.evaluate(next,config,List.of(),LocalDate.now(),12);
        assertEquals(48.11,a.margin-b.margin,.001);assertTrue(a.hourly>b.hourly);
    }
    @Test public void overlayLabelAndItsMoneyCannotBecomeAnotherXlCard(){
        OfferParser.Offer o=OfferParser.parse("TIPO: UBERXL · EXCLUSIVO\n$9999.00\n"+XL).offer;
        assertNotNull(o);assertEquals(28493,o.cents);assertFalse(o.exclusive);
        assertNull(OfferParser.parse(XL+"\n"+XL).offer);
    }
    @Test public void xlDoesNotRelaxCentsOrConsistencyChecks(){
        assertNull(OfferParser.parse(XL.replace("284.93","28493")).offer);
        assertNull(OfferParser.parse(XL.replace("284.93","28493.00")).offer);
        assertNull(OfferParser.parse(XL.replace("UberXL","UberXLT")).offer);
    }
    @Test public void categoryDoesNotAddPointsOrBypassPickupLimits(){
        DriverConfig c=new DriverConfig();
        ScoreEngine.Evaluation xl=ScoreEngine.evaluate(OfferParser.parse(XL).offer,c,List.of(),LocalDate.now(),12);
        ScoreEngine.Evaluation x=ScoreEngine.evaluate(OfferParser.parse(XL.replace("UberXL","UberX")).offer,c,List.of(),LocalDate.now(),12);
        assertEquals(x.score,xl.score);assertEquals(x.margin,xl.margin,0);assertEquals(ScoreEngine.Color.ROJO,xl.color);
        assertTrue(xl.reasons.contains("Recogida fuera de tus límites"));
    }
    @Test public void xlDoesNotBypassNewRiderRule(){
        DriverConfig c=new DriverConfig();c.maxPickupMin=30;c.maxPickupKm=12;
        ScoreEngine.Evaluation e=ScoreEngine.evaluate(OfferParser.parse(XL.replace("4.87 (91)","5.00 (3)")).offer,c,List.of(),LocalDate.now(),12);
        assertTrue(e.riderBlocked);assertEquals(ScoreEngine.Color.ROJO,e.color);
    }
}
