package mx.tesivil.detector;
import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.Assert.*;
public class ZoneContextTest {
    private ZoneRule rule(){ZoneRule r=new ZoneRule();r.neighborhood="Colonia Ejemplo";r.municipality="Zapopan";r.action=ZoneRule.Action.PRECAUCION;r.reviewedOn="2026-09-13";r.catalogId="historical";r.officialEvidence="Fiscalía / IIEG\nPeriodo: enero–octubre de 2021";r.officialUrl="https://iieg.gob.mx/";return r;}
    private OfferParser.Offer offer(String pickup){return new OfferParser.Offer(20000,2,1,20,5,null,pickup,"Otro lugar, Guadalajara");}
    @Test public void updatedPersonalReviewDoesNotMakeHistoricalReferenceRecent(){ZoneRule r=rule();r.source="Revisé hoy";ZoneRule.Assessment a=ZoneRule.assess(offer("Colonia Ejemplo, Zapopan"),List.of(r),LocalDate.of(2026,9,13),12);assertTrue(a.pickupReference.contains("2021"));assertTrue(a.pickupReference.contains("revisión 2026-09-13"));assertTrue(a.pickupReference.contains("no es la fecha de un delito"));assertFalse(a.pickupUnknown);assertTrue(a.destinationUnknown);}
    @Test public void uncertainMunicipalityAndExpiredReviewHaveDifferentExplanations(){ZoneRule r=rule();ZoneRule.Assessment a=ZoneRule.assess(offer("Colonia Ejemplo"),List.of(r),LocalDate.of(2026,9,13),12);assertTrue(a.pickupStatus.contains("municipio sin confirmar"));assertTrue(a.pickupUnknown);r.reviewedOn="2025-01-01";a=ZoneRule.assess(offer("Colonia Ejemplo, Zapopan"),List.of(r),LocalDate.of(2026,9,13),12);assertTrue(a.pickupStatus.contains("vencida"));assertTrue(a.pickupUnknown);assertFalse(a.blocked);}
}
