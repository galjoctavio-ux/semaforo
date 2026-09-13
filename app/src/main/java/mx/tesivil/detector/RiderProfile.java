package mx.tesivil.detector;

/** Only the anonymous fields visible on the current offer; missing is never zero. */
public final class RiderProfile {
    public final Double rating;
    public final Integer count;
    public final boolean explicitNew, ambiguous;
    public final String countSource;
    public RiderProfile(Double rating, Integer count, boolean explicitNew, boolean ambiguous, String countSource) {
        boolean invalid = rating != null && (!Double.isFinite(rating) || rating < 1 || rating > 5)
                || count != null && (count < 0 || count > 1_000_000);
        this.ambiguous = ambiguous || invalid;
        this.rating = this.ambiguous ? null : rating;
        this.count = this.ambiguous ? null : count;
        this.explicitNew = explicitNew;
        this.countSource = countSource;
    }
    public static RiderProfile unknown() { return new RiderProfile(null,null,false,false,"NOT_VISIBLE"); }
    public String key() { return rating+":"+count+":"+explicitNew+":"+ambiguous+":"+countSource; }
    public String summary() {
        if(explicitNew) return "Pasajero: NUEVO"+(ambiguous?" · datos ambiguos":"");
        if(ambiguous) return "Pasajero: lectura ambigua";
        return "Pasajero: "+(rating==null?"★ sin leer":"★ "+String.format(java.util.Locale.US,"%.2f",rating))
                +" · "+(count==null?"contador sin leer":count+" visibles");
    }
}
