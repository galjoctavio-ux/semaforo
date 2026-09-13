package mx.tesivil.detector;

import java.util.UUID;

/** Session-only heuristic. Route text never leaves RAM; a group is not a confirmed trip. */
final class OfferEpisodes {
    static final long MAX_GAP_MS=180_000;
    static final class Observation {
        final String id;final int version;final Long previousCents;
        Observation(String id,int version,Long previous){this.id=id;this.version=version;previousCents=previous;}
    }
    private String signature,id;
    private long lastAt,lastCents;
    private int version;
    private Long previous;
    Observation observe(OfferParser.Offer o,long now,boolean live){
        String route=o.pickupMinutes+":"+o.pickupKm+":"+o.tripMinutes+":"+o.tripKm+":"+o.rider.key()+":"+o.serviceType+":"+o.reserved+":"+o.destinationNoticeCount
                +":"+ZoneRule.norm(o.pickupAddress)+":"+ZoneRule.norm(o.destinationAddress);
        boolean complete=!o.pickupAddress.isBlank() && !o.destinationAddress.isBlank();
        String next=live?route+(complete?"":":"+o.cents):o.key()+":"+route;
        if(id==null || !next.equals(signature) || now<lastAt || now-lastAt>MAX_GAP_MS){
            signature=next;id=UUID.randomUUID().toString();version=1;previous=null;
        }else if(o.cents!=lastCents){previous=lastCents;version++;}
        lastCents=o.cents;lastAt=now;
        return new Observation(id,version,previous);
    }
    void reset(){signature=id=null;version=0;previous=null;lastAt=lastCents=0;}
    void finish(String episode){if(episode!=null && episode.equals(id))reset();}
}
