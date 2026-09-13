package mx.tesivil.detector;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit Spanish offer formats. Category labels do not imply extra earnings or safety. */
public final class OfferParser {
    private static final Pattern CATEGORY = Pattern.compile("(?m)^\\s*[^a-z\\n]{0,4}(uber\\s*x\\s*l|uber\\s*x(?:\\s+priority)?|uber\\s+priority|priority)\\s*(?:exclusivo)?\\s*$");
    private static final Pattern CATEGORY_PREFIX = Pattern.compile("(?m)^\\s*[^a-z\\n]{0,4}(?:uber\\s*x\\s*l|uber\\s*x|uber\\s+priority|priority)(?=\\s|$)");
    private static final Pattern CTA = Pattern.compile("\\b(viaje\\s+disponible|aceptar(?:\\s+viaje)?)\\b");
    private static final Pattern FARE = Pattern.compile(
            "(?m)^\\s*(?:mx\\s*\\$|\\$|mxn)\\s*([0-9][0-9., ]{0,12})\\s*(?:mxn)?\\s*$");
    // Real sample: OCR read 11 as 1l. Repair only the minute token of a complete leg.
    private static final String LEG = "([0-9il|]{1,3})\\s*min(?:utos)?\\s*\\(\\s*([0-9]+(?:[.,][0-9]+)?)\\s*(km|m)\\s*\\)";
    private static final Pattern PICKUP = Pattern.compile("(?m)^\\s*a\\s*" + LEG + "\\s*$");
    private static final Pattern TRIP = Pattern.compile("(?m)^\\s*viaje\\s*:\\s*" + LEG + "\\s*$");
    private static final Pattern RATE = Pattern.compile("\\$\\s*([0-9]+[.,][0-9]{2})\\s*/\\s*km");
    private static final Pattern RIDER = Pattern.compile("(?m)^\\s*(?:identidad\\s+verificada\\s+)?[★☆⭐*]?\\s*([0-9][.,][0-9]{1,2})\\s*(?:\\(\\s*([0-9]{1,7})\\s*\\)|(?:[·|]\\s*)?([0-9]{1,7})\\s+viajes)?\\s*$");
    private static final Pattern NEW_RIDER = Pattern.compile("(?m)^\\s*[★☆⭐*]?\\s*(?:nuevo|usuario nuevo|pasajero nuevo|nuevo usuario|nuevo pasajero)\\s*$");
    private static final Pattern RIDER_COUNT = Pattern.compile("(?m)^\\s*([0-9]{1,7})\\s+viajes\\s*$");
    private static final Pattern RIDER_PAIR = Pattern.compile("(?<![0-9$])([0-9][.,][0-9]{1,2})\\s*\\(\\s*([0-9]{1,7})\\s*\\)");
    private static final Pattern EXCLUSIVE = Pattern.compile("\\bexclusivo\\b");
    public enum ServiceType { UBER_X, UBER_XL, PRIORITY }

    public static final class Offer {
        public final long cents;
        public final int pickupMinutes, tripMinutes;
        public final double pickupKm, tripKm;
        public final Double displayedRate;
        public final String pickupAddress, destinationAddress;
        public final RiderProfile rider;
        public final ServiceType serviceType;
        public final boolean exclusive;
        public Offer(long cents, int pickupMinutes, double pickupKm,
                     int tripMinutes, double tripKm, Double displayedRate) {
            this(cents, pickupMinutes, pickupKm, tripMinutes, tripKm, displayedRate, "", "");
        }
        public Offer(long cents, int pickupMinutes, double pickupKm, int tripMinutes, double tripKm,
                     Double displayedRate, String pickupAddress, String destinationAddress) {
            this(cents,pickupMinutes,pickupKm,tripMinutes,tripKm,displayedRate,pickupAddress,destinationAddress,RiderProfile.unknown());
        }
        public Offer(long cents, int pickupMinutes, double pickupKm, int tripMinutes, double tripKm,
                     Double displayedRate, String pickupAddress, String destinationAddress, RiderProfile rider) {
            this(cents,pickupMinutes,pickupKm,tripMinutes,tripKm,displayedRate,pickupAddress,destinationAddress,rider,ServiceType.UBER_X,false);
        }
        public Offer(long cents, int pickupMinutes, double pickupKm, int tripMinutes, double tripKm,
                     Double displayedRate, String pickupAddress, String destinationAddress, RiderProfile rider,
                     ServiceType serviceType, boolean exclusive) {
            this.cents = cents; this.pickupMinutes = pickupMinutes; this.pickupKm = pickupKm;
            this.tripMinutes = tripMinutes; this.tripKm = tripKm; this.displayedRate = displayedRate;
            this.pickupAddress = pickupAddress; this.destinationAddress = destinationAddress;
            this.rider = rider == null ? RiderProfile.unknown() : rider;
            this.serviceType = serviceType == null ? ServiceType.UBER_X : serviceType;this.exclusive=exclusive;
        }
        public double totalKm() { return pickupKm + tripKm; }
        public int totalMinutes() { return pickupMinutes + tripMinutes; }
        public String typeLabel(){return (serviceType==ServiceType.PRIORITY?"Priority":serviceType==ServiceType.UBER_XL?"UberXL":"UberX")+(exclusive?" · Exclusivo":"");}
        // Exclusive can change when the same offer moves from radar to a direct card.
        public String key() { return cents + ":" + pickupMinutes + ":" + pickupKm + ":" + tripMinutes + ":" + tripKm + ":" + rider.key()+":"+serviceType.name(); }
    }

    public static final class Result {
        public final Offer offer;
        public final boolean offerScreen;
        public final String reason;
        public final boolean needsMoneyRefinement;
        public final boolean needsNumericRefinement;
        public final boolean needsCardRefinement;
        private Result(Offer offer, boolean offerScreen, String reason) {
            this(offer,offerScreen,reason,false);
        }
        private Result(Offer offer, boolean offerScreen, String reason, boolean needsMoneyRefinement) {
            this(offer,offerScreen,reason,needsMoneyRefinement,false);
        }
        private Result(Offer offer, boolean offerScreen, String reason, boolean needsMoneyRefinement,boolean needsNumericRefinement) {
            this(offer,offerScreen,reason,needsMoneyRefinement,needsNumericRefinement,false);
        }
        private Result(Offer offer, boolean offerScreen, String reason, boolean needsMoneyRefinement,boolean needsNumericRefinement,boolean needsCardRefinement) {
            this.offer = offer; this.offerScreen = offerScreen; this.reason = reason;
            this.needsMoneyRefinement=needsMoneyRefinement;
            this.needsNumericRefinement=needsNumericRefinement;
            this.needsCardRefinement=needsCardRefinement;
        }
    }

    public static Result parse(String original) {
        if (original == null || original.isBlank()) return missing(false, "Sin texto legible");
        String text = normalized(original);
        Matcher category=CATEGORY.matcher(text);
        if (!category.find()) return new Result(null,false,"Esperando oferta UberX, UberXL o Priority",false,false,
                CATEGORY_PREFIX.matcher(text).find() && FARE.matcher(text).find() && PICKUP.matcher(text).find() && TRIP.matcher(text).find());
        int categoryStart=category.start();String label=category.group(1).replaceAll("\\s+","");
        ServiceType type=label.contains("priority")?ServiceType.PRIORITY:label.equals("uberxl")?ServiceType.UBER_XL:ServiceType.UBER_X;
        // Horizontally adjacent badges can be sorted with Exclusive just before UberX.
        boolean precedingExclusive=Pattern.compile("(?:^|\\n)\\s*exclusivo\\s*$").matcher(text.substring(0,categoryStart)).find();
        if(category.find())return missing(true,"Hay varias tarjetas: lectura ambigua");
        Matcher cta=CTA.matcher(text);
        if (!cta.find(categoryStart)) return new Result(null,false,"Sin tarjeta de solicitud vigente",false,false,
                FARE.matcher(text.substring(categoryStart)).find() && PICKUP.matcher(text.substring(categoryStart)).find() && TRIP.matcher(text.substring(categoryStart)).find());
        // Exclude our own score, banners and other app amounts above/below the current card.
        text=text.substring(categoryStart,cta.end());
        Matcher fare = FARE.matcher(text);
        if (!fare.find()) return missing(true, "Falta el importe de la oferta");
        int fareEnd = fare.end();
        String fareValue=fare.group(1);
        if (fare.find()) return missing(true, "Hay varios importes: lectura ambigua");
        // Never turn $7706 into either $7,706 or $77.06 by assumption. Retry the pixels.
        if(!hasMoneyDecimals(fareValue))return new Result(null,true,"Importe sin centavos legibles",true);
        long cents;
        try { cents = parseMoney(fareValue); }
        catch (IllegalArgumentException e) { return missing(true, "Importe ambiguo"); }
        Matcher pickup = PICKUP.matcher(text), trip = TRIP.matcher(text);
        if (!pickup.find()) return missing(true, "Faltan minutos o km de recogida");
        int pickupStart = pickup.start(), pickupEnd = pickup.end();
        int pMinutes = minutes(pickup.group(1));
        double pKm = distance(pickup.group(2), pickup.group(3));
        if (pickup.find()) return missing(true, "Hay varias recogidas: lectura ambigua");
        if (!trip.find()) return missing(true, "Faltan minutos o km del viaje");
        int tripStart = trip.start(), tripEnd = trip.end();
        int tMinutes = minutes(trip.group(1));
        double tKm = distance(trip.group(2), trip.group(3));
        if (trip.find()) return missing(true, "Hay varios viajes: lectura ambigua");
        if(fareEnd>pickupStart || pickupEnd>tripStart)return missing(true,"Orden de datos ambiguo");
        if (cents <= 0 || cents > 10_000_000 || pMinutes > 240 || tMinutes < 1 || tMinutes > 600
                || pKm > 300 || tKm <= 0 || tKm > 1000) return missing(true, "Valores fuera del formato soportado");
        Double rate = null;
        String header=text.substring(fareEnd,pickupStart);
        Matcher rateMatch = RATE.matcher(header);
        if (rateMatch.find()) {
            rate = Double.parseDouble(rateMatch.group(1).replace(',', '.'));
            if(rateMatch.find())return missing(true,"Hay varias tarifas por km: lectura ambigua");
            double expected=rate*(pKm+tKm), difference=Math.abs(cents/100d-expected);
            // Wide heuristic tolerance for rounded/estimated km. This only rejects disagreement;
            // it never derives/replaces the fare from the displayed rate.
            if(!Double.isFinite(rate)||rate<=0 || difference>expected*.25+rate*.2)return new Result(null,true,"Importe y tarifa por km incoherentes",false,true);
        }
        String pickupAddress = tripStart > pickupEnd ? text.substring(pickupEnd, tripStart).trim() : "";
        Matcher endCard = CTA.matcher(text);
        String destinationAddress = endCard.find(tripEnd) ? text.substring(tripEnd, endCard.start()).trim() : "";
        if (pickupAddress.length() > 600) pickupAddress = "";
        if (destinationAddress.length() > 600) destinationAddress = "";
        RiderProfile rider = parseRider(header);
        boolean exclusive=precedingExclusive||EXCLUSIVE.matcher(text.substring(0,pickupStart)).find();
        return new Result(new Offer(cents, pMinutes, pKm, tMinutes, tKm, rate, pickupAddress, destinationAddress,rider,type,exclusive), true, "Datos detectados");
    }

    static String normalized(String text){return Normalizer.normalize(text,Normalizer.Form.NFKD).replaceAll("\\p{M}","").replace('\u00a0',' ').replace('\r','\n').toLowerCase(Locale.ROOT);}
    static boolean isCategoryLine(String text){return CATEGORY.matcher(normalized(text)).matches();}
    static boolean moneyNeedsRefinement(String text){Matcher m=FARE.matcher(normalized(text));return m.matches()&&!hasMoneyDecimals(m.group(1));}
    static int numericFieldKind(String original){String text=normalized(original);if(FARE.matcher(text).matches())return 1;if(RATE.matcher(text).find())return 2;if(PICKUP.matcher(text).matches())return 3;if(TRIP.matcher(text).matches())return 4;return 0;}
    static boolean isCardActionLine(String text){return CTA.matcher(normalized(text)).find();}
    private static boolean hasMoneyDecimals(String value){return value.replace(" ","").matches(".*[.,][0-9]{2}");}
    private static int minutes(String value){return Integer.parseInt(value.replace('i','1').replace('l','1').replace('|','1'));}

    private static RiderProfile parseRider(String header) {
        boolean isNew = NEW_RIDER.matcher(header).find();
        Double rating=null; Integer count=null; String source="NOT_VISIBLE"; int matches=0;
        Matcher pair=RIDER_PAIR.matcher(header);
        while(pair.find()) {matches++;rating=Double.parseDouble(pair.group(1).replace(',','.'));count=Integer.valueOf(pair.group(2));source="ADJACENT_TO_RATING";}
        // OCR can join the identity badge with the star, or mistake the star for a glyph.
        // The tightly paired numeric form is accepted only in the current offer header.
        Matcher m = RIDER.matcher(RIDER_PAIR.matcher(header).replaceAll(""));
        while(m.find()) {
            matches++;
            rating=Double.parseDouble(m.group(1).replace(',','.'));
            String visible=m.group(2)!=null?m.group(2):m.group(3);
            if(visible!=null){count=Integer.valueOf(visible);source=m.group(2)!=null?"ADJACENT_TO_RATING":"LABELED_TRIPS";}
        }
        Matcher countOnly=RIDER_COUNT.matcher(header);int counts=0;
        while(countOnly.find()){counts++;Integer next=Integer.valueOf(countOnly.group(1));if(count!=null && !count.equals(next))matches=2;count=next;source="LABELED_TRIPS";}
        // Two passenger badges or a New badge plus a positive count cannot be reconciled.
        return new RiderProfile(rating,count,isNew,matches>1 || counts>1 || isNew && count!=null && count>0,source);
    }

    private static double distance(String value, String unit) {
        double number = Double.parseDouble(value.replace(',', '.'));
        return "m".equals(unit) ? number / 1000d : number;
    }

    static long parseMoney(String input) {
        String value = input.replace(" ", "");
        if (!value.matches("(?:[0-9]+|[0-9]{1,3}(?:[.,][0-9]{3})+)(?:[.,][0-9]{2})?"))
            throw new IllegalArgumentException("money");
        int separator = Math.max(value.lastIndexOf('.'), value.lastIndexOf(','));
        if (separator >= 0 && value.length() - separator - 1 == 2) {
            String whole = value.substring(0, separator).replace(".", "").replace(",", "");
            return Long.parseLong(whole) * 100 + Integer.parseInt(value.substring(separator + 1));
        }
        return Long.parseLong(value.replace(".", "").replace(",", "")) * 100;
    }

    private static Result missing(boolean card, String reason) { return new Result(null, card, reason); }
    private OfferParser() { }
}
