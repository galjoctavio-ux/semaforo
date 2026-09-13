package mx.tesivil.detector;

import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;

public class ZoneRuleTest {
    private ZoneRule rule(){ZoneRule r=new ZoneRule();r.neighborhood="Las Palmas";r.municipality="Zapopan";r.reviewedOn="2026-09-01";return r;}
    @Test public void matchesNeighborhoodWithAccentsPostalCodeAndMunicipality(){ZoneRule r=rule();assertEquals(2,r.matches("Calle Uno 52, 45000 Las Pálmas, Zapopan"));}
    @Test public void streetNameIsNotAColony(){assertEquals(0,rule().matches("Avenida Las Palmas 52, Otra Colonia, Zapopan"));}
    @Test public void wholeColonyRequired(){assertEquals(0,rule().matches("Calle 2, Las Palmas del Norte, Zapopan"));}
    @Test public void missingMunicipalityIsAmbiguous(){assertEquals(1,rule().matches("Calle 2, Las Palmas"));}
    @Test public void explicitColoniaPrefixSupported(){ZoneRule r=rule();r.neighborhood="Las Palmas";assertEquals(2,r.matches("Calle 2, Col. Las Palmas, Zapopan"));}
    @Test public void wrappedColonyIsOnePlace(){assertEquals(2,rule().matches("Calle 2, 45000 Las\nPalmas, Zapopan"));}
    @Test public void overnightHoursWork(){ZoneRule r=rule();r.startHour=20;r.endHour=6;assertTrue(r.activeAt(23));assertTrue(r.activeAt(5));assertFalse(r.activeAt(6));assertFalse(r.activeAt(12));}
    @Test public void expiredReviewDoesNotPermitGreen(){ZoneRule r=rule();r.action=ZoneRule.Action.REVISADA;r.validDays=1;OfferParser.Offer o=new OfferParser.Offer(10000,1,1,10,5,null,"Las Palmas, Zapopan","Las Palmas, Zapopan");assertTrue(ZoneRule.assess(o,List.of(r),LocalDate.of(2026,9,12),12).unknown);}
    @Test public void unknownNeverMeansSafe(){OfferParser.Offer o=new OfferParser.Offer(10000,1,1,10,5,null,"","Un lugar");assertTrue(ZoneRule.assess(o,List.of(),LocalDate.of(2026,9,12),12).unknown);}
    @Test public void historicalRuleStaysActiveWithoutPretendingDateWasUpdated(){
        ZoneRule r=rule();r.reviewedOn="2021-10-31";r.noExpiry=true;r.action=ZoneRule.Action.PRECAUCION;
        assertTrue(r.current(LocalDate.of(2026,9,12)));assertFalse(r.current(LocalDate.of(2021,1,1)));
        OfferParser.Offer o=new OfferParser.Offer(10000,1,1,10,5,null,"Las Palmas, Zapopan","Las Palmas, Zapopan");
        assertTrue(ZoneRule.assess(o,List.of(r),LocalDate.of(2026,9,12),12).caution);
        assertEquals("2021-10-31",r.reviewedOn);
    }
    @Test public void sourceAbbreviationsMatchButSectionsStayDistinct(){
        ZoneRule r=rule();r.neighborhood="Hacienda Santa Fe";assertEquals(2,r.matches("Calle 1, Fracc. Hda. Santa Fe, Zapopan"));
        r.neighborhood="Loma Dorada Seccion A";assertEquals(2,r.matches("Calle 1, Loma Dorada Secc. A, Zapopan"));
        assertEquals(0,r.matches("Calle 1, Loma Dorada Secc. B, Zapopan"));assertEquals(0,r.matches("Calle 1, Loma Dorada, Zapopan"));
    }
    @Test public void tlaquepaqueMunicipalityShortNameIsAccepted(){ZoneRule r=rule();r.municipality="San Pedro Tlaquepaque";assertEquals(2,r.matches("Las Palmas, Tlaquepaque"));assertEquals(1,r.matches("Las Palmas, Zapopan"));}
    @Test public void migrationIdentityUsesAbbreviationsAndDoesNotMixMunicipalities(){
        ZoneRule a=rule(),b=rule();a.neighborhood="Fracc. Hda. Santa Fe";b.neighborhood="Hacienda Santa Fe";assertTrue(ZoneRule.samePlace(a,b));
        b.municipality="Guadalajara";assertFalse(ZoneRule.samePlace(a,b));
    }
}
