package mx.tesivil.detector;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Local, explicit neighborhood rules. No geolocation or crime probability is inferred. */
public final class ZoneRule {
    public enum Action { EVITAR, PRECAUCION, REVISADA }
    public String neighborhood = "", municipality = "", source = "Preferencia personal", reviewedOn = LocalDate.now().toString();
    public Action action = Action.EVITAR;
    public boolean pickup = true, destination = true;
    public int startHour = 0, endHour = 0;
    public int validDays = 90;
    public boolean noExpiry = false;
    // Immutable provenance from a bundled source, kept separately from the user's reason.
    public String catalogId = "", officialEvidence = "", officialUrl = "";
    public String supplementEvidence = "";
    public String referenceKind() {
        boolean community=supplementEvidence.contains("REPORTE COMUNITARIO");
        boolean official=!officialEvidence.isEmpty() || supplementEvidence.contains("FUENTE OFICIAL");
        return community?(official?"Fuentes oficiales y reporte comunitario":"Reporte comunitario sin corroborar"):"Antecedentes oficiales históricos";
    }
    public String validate() {
        if (norm(neighborhood).length() < 3 || neighborhood.length() > 80 || neighborhood.contains("\n")) return "Escribe una colonia de 3 a 80 caracteres";
        if (municipality.length() > 80 || source.isBlank() || source.length() > 200) return "Revisa municipio y fuente";
        if (catalogId.length() > 100 || officialEvidence.length() > 2000 || officialUrl.length() > 500 || supplementEvidence.length()>12000) return "Referencia de zona inválida";
        if (!pickup && !destination) return "Selecciona recogida, destino o ambos";
        if (startHour < 0 || startHour > 23 || endHour < 0 || endHour > 23 || validDays < 1 || validDays > 365) return "Revisa horario o vigencia";
        try { LocalDate.parse(reviewedOn); } catch (Exception e) { return "Fecha inválida; usa AAAA-MM-DD"; }
        if (LocalDate.parse(reviewedOn).isAfter(LocalDate.now())) return "La revisión no puede estar en el futuro";
        return null;
    }
    public boolean activeAt(int hour) {
        return startHour == endHour || (startHour < endHour ? hour >= startHour && hour < endHour : hour >= startHour || hour < endHour);
    }
    public boolean current(LocalDate today) {
        try { LocalDate d = LocalDate.parse(reviewedOn); return !d.isAfter(today) && (noExpiry || !d.plusDays(validDays).isBefore(today)); }
        catch (Exception e) { return false; }
    }
    // 0=no match, 1=neighborhood but municipality missing, 2=explicit matching text.
    int matches(String address) {
        if (address == null || address.isBlank()) return 0;
        String n = placeNorm(neighborhood), a = norm(address);
        boolean colony = false;
        for (String raw : address.split("[,;]|\\s+-\\s+")) {
            String part = placeNorm(raw);
            // A street named after a neighborhood does not establish its location.
            if (part.equals(n)) colony = true;
        }
        if (!colony) return 0;
        String muni = norm(municipality);
        boolean municipalityPresent = (" " + a + " ").contains(" " + muni + " ");
        if (muni.equals("san pedro tlaquepaque")) municipalityPresent |= (" " + a + " ").contains(" tlaquepaque ");
        return municipality.isBlank() || municipalityPresent ? 2 : 1;
    }
    static String placeNorm(String text) {
        return norm(text).replaceFirst("^\\d{5}\\s+", "")
            .replaceFirst("^(colonia|col|fraccionamiento|fracc)\\s+", "")
            .replaceAll("\\bhda\\b", "hacienda").replaceAll("\\bjards\\b", "jardines")
            .replaceAll("\\bsecc\\b", "seccion").replaceAll("\\bote\\b", "oriente");
    }
    static boolean samePlace(ZoneRule a, ZoneRule b) {
        return placeNorm(a.neighborhood).equals(placeNorm(b.neighborhood))
            && norm(a.municipality).replace("san pedro tlaquepaque","tlaquepaque").equals(norm(b.municipality).replace("san pedro tlaquepaque","tlaquepaque"));
    }
    public String levelLabel() {
        return switch(action) { case EVITAR -> "Alto · Evitar"; case PRECAUCION -> "Medio · Precaución"; case REVISADA -> "Bajo · Revisada por mí"; };
    }
    public static String norm(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }
    public static final class Assessment {
        public boolean blocked, caution, unknown;
        public String pickupStatus, destinationStatus, reason;
    }
    public static Assessment assess(OfferParser.Offer o, List<ZoneRule> rules, LocalDate date, int hour) {
        Endpoint p = endpoint(o.pickupAddress, rules, true, date, hour);
        Endpoint d = endpoint(o.destinationAddress, rules, false, date, hour);
        Assessment a = new Assessment();
        a.blocked = p.blocked || d.blocked; a.caution = p.caution || d.caution; a.unknown = !p.reviewed || !d.reviewed;
        a.pickupStatus = p.message; a.destinationStatus = d.message;
        a.reason = a.blocked ? (p.blocked ? "Recogida: " + p.message : "Destino: " + d.message)
                : a.caution ? (p.caution ? "Recogida: " + p.message : "Destino: " + d.message)
                : a.unknown ? "Zona sin evaluación suficiente" : "Ambos extremos revisados por ti";
        return a;
    }
    private static final class Endpoint { boolean blocked, caution, reviewed; String message = "Sin evaluación de zona"; }
    private static Endpoint endpoint(String address, List<ZoneRule> rules, boolean pickup, LocalDate date, int hour) {
        Endpoint e = new Endpoint();
        if (address == null || address.isBlank()) { e.message = "Dirección no legible"; return e; }
        boolean uncertain = false;
        for (ZoneRule r : rules) {
            if (pickup ? !r.pickup : !r.destination) continue;
            if (!r.activeAt(hour)) continue;
            int match = r.matches(address);
            if (match == 0) continue;
            if (match == 1 || !r.current(date)) { uncertain = true; continue; }
            e.reviewed = true;
            if (r.action == Action.EVITAR) { e.blocked = true; e.message = "Evitar " + r.neighborhood + (r.catalogId.isEmpty()?" (regla tuya)":" (base histórica / ajuste tuyo)"); }
            else if (r.action == Action.PRECAUCION && !e.blocked) { e.caution = true; e.message = "Precaución: " + r.neighborhood + (r.catalogId.isEmpty()?"":r.referenceKind().startsWith("Reporte comunitario")?" (reporte comunitario)":" (antecedente histórico)"); }
            else if (r.action == Action.REVISADA && !e.blocked && !e.caution) { e.reviewed = true; e.message = "Revisada por ti: " + r.neighborhood; }
        }
        if (uncertain && !e.blocked && !e.caution) { e.reviewed = false; e.message = "Coincidencia ambigua o revisión vencida"; }
        return e;
    }
}
