package mx.tesivil.detector;

/** Two matching observations, with bounded gaps, scoped to the active session. */
public final class OfferConfirmation {
    private String candidate;
    private int matches;
    private long lastAt;
    public boolean observe(OfferParser.Offer o, long now) {
        String key = o.key() + "|" + ZoneRule.norm(o.pickupAddress) + "|" + ZoneRule.norm(o.destinationAddress);
        if (!key.equals(candidate) || now < lastAt || now - lastAt > 1800) { candidate = key; matches = 1; }
        else if (now > lastAt) matches++;
        lastAt = now;
        return matches >= 2;
    }
    public void reset() { candidate = null; matches = 0; lastAt = 0; }
}
