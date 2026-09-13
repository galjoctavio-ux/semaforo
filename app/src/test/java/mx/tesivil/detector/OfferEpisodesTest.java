package mx.tesivil.detector;
import org.junit.Test;
import static org.junit.Assert.*;

public class OfferEpisodesTest {
    private OfferParser.Offer o(long cents,String pickup){return new OfferParser.Offer(cents,19,8.5,41,17.1,null,pickup,"Monumental, Guadalajara");}
    @Test public void consecutivePriceUpdatesAreVersionsOfOneLiveOffer(){OfferEpisodes ep=new OfferEpisodes();OfferEpisodes.Observation a=ep.observe(o(28493,"Base Militar, Zapopan"),100,true),b=ep.observe(o(23682,"Base Militar, Zapopan"),200,true),again=ep.observe(o(23682,"Base Militar, Zapopan"),300,true);assertEquals(a.id,b.id);assertEquals(2,b.version);assertEquals(Long.valueOf(28493),b.previousCents);assertEquals(2,again.version);assertEquals(b.id,again.id);}
    @Test public void anImportedDifferentFareIsNotInferredToBeALiveUpdate(){OfferEpisodes ep=new OfferEpisodes();String first=ep.observe(o(28493,"A"),100,false).id;OfferEpisodes.Observation second=ep.observe(o(23682,"A"),200,false);assertNotEquals(first,second.id);assertNull(second.previousCents);}
    @Test public void missingRouteDoesNotInferUpdates(){OfferEpisodes ep=new OfferEpisodes();String first=ep.observe(o(28493,""),100,true).id;assertNotEquals(first,ep.observe(o(23682,""),200,true).id);}
    @Test public void differentRouteGapOrBackwardClockCreatesANewGroup(){OfferEpisodes ep=new OfferEpisodes();String first=ep.observe(o(10000,"A"),100,true).id;String second=ep.observe(o(10000,"B"),200,true).id;assertNotEquals(first,second);String third=ep.observe(o(10000,"B"),200+OfferEpisodes.MAX_GAP_MS+1,true).id;assertNotEquals(second,third);assertNotEquals(third,ep.observe(o(10000,"B"),100,true).id);}
    @Test public void finishingAnUnrelatedOfferDoesNotResetCurrentOffer(){OfferEpisodes ep=new OfferEpisodes();String first=ep.observe(o(10000,"A"),100,true).id;ep.finish("unrelated");assertEquals(first,ep.observe(o(10000,"A"),200,true).id);ep.finish(first);assertNotEquals(first,ep.observe(o(10000,"A"),300,true).id);}
}
