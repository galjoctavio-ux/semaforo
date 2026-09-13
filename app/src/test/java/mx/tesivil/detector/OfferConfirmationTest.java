package mx.tesivil.detector;
import org.junit.Test;
import static org.junit.Assert.*;
public class OfferConfirmationTest {
    @Test public void changedPassengerClearsPreviousConfirmation(){OfferConfirmation c=new OfferConfirmation();OfferParser.Offer old=OfferParser.parse(OfferParserTest.SAMPLE).offer;assertFalse(c.observe(old,100));assertTrue(c.observe(old,520));OfferParser.Offer next=OfferParser.parse(OfferParserTest.SAMPLE.replace("4.83 (370)","Nuevo")).offer;assertFalse(c.observe(next,940));assertTrue(c.observe(next,1360));}
    private OfferParser.Offer offer(long cents){return new OfferParser.Offer(cents,5,1.7,18,4.7,null,"A","B");}
    @Test public void requiresTwoReadingsAndRejectsRepeatedTimestamp(){OfferConfirmation c=new OfferConfirmation();assertFalse(c.observe(offer(6956),100));assertFalse(c.observe(offer(6956),100));assertTrue(c.observe(offer(6956),520));}
    @Test public void changedFareOrEndpointStartsAgain(){OfferConfirmation c=new OfferConfirmation();c.observe(offer(6956),100);assertTrue(c.observe(offer(6956),520));assertFalse(c.observe(offer(9831),940));assertFalse(c.observe(new OfferParser.Offer(9831,5,1.7,18,4.7,null,"A","C"),1360));}
    @Test public void oldOrResetReadingsDontConfirm(){OfferConfirmation c=new OfferConfirmation();c.observe(offer(6956),100);assertFalse(c.observe(offer(6956),3000));c.reset();assertFalse(c.observe(offer(6956),3420));}
}
