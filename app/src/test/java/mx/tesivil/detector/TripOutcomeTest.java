package mx.tesivil.detector;
import org.junit.Test;
import static org.junit.Assert.*;
public class TripOutcomeTest {
    private TripOutcome r(){TripOutcome r=new TripOutcome();r.state=TripOutcome.State.COMPLETED;r.settledCents=10000L;r.totalMinutes=30d;r.totalKm=10d;return r;}
    @Test public void missingCostsAreUnknownAndExplicitZerosAreKnown(){TripOutcome r=r();assertNull(r.validate());assertNull(r.actualMargin());r.energyCents=r.upkeepCents=r.fixedCents=r.extrasCents=0L;assertEquals(100,r.actualMargin(),0);assertEquals(200,r.actualHourly(),0);}
    @Test public void noRealFiguresAllowedForAcceptedOrDeclinedOffers(){TripOutcome r=r();r.state=TripOutcome.State.ACCEPTED;assertNotNull(r.validate());assertNull(r.actualMargin());r=new TripOutcome();r.state=TripOutcome.State.DECLINED;assertNull(r.validate());}
    @Test public void negativeNetIsAValidReportedLoss(){TripOutcome r=r();r.energyCents=11000L;r.upkeepCents=r.fixedCents=r.extrasCents=0L;assertNull(r.validate());assertEquals(-10,r.actualMargin(),0);}
    @Test public void missingRequiredOrInvalidTotalsNeverBecomeRealIncome(){TripOutcome r=r();r.settledCents=null;r.energyCents=r.upkeepCents=r.fixedCents=r.extrasCents=0L;assertNotNull(r.validate());assertNull(r.actualMargin());r=r();r.totalMinutes=Double.NaN;assertNotNull(r.validate());r=r();r.totalKm=0d;assertNotNull(r.validate());r=r();r.energyCents=-1L;assertNotNull(r.validate());}
    @Test public void waitAndReturnAreComponentsAndMayNotExceedTotals(){TripOutcome r=r();r.waitMinutes=20d;r.repositionMinutes=15d;assertNotNull(r.validate());r.repositionMinutes=10d;r.repositionKm=10d;assertNull(r.validate());r.repositionKm=10.1;assertNotNull(r.validate());}
}
