package mx.tesivil.detector;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Deterministic arithmetic + explicit preferences; the score is NOT a probability. */
public final class ScoreEngine {
    public enum Color { VERDE, AMBAR, ROJO, GRIS }
    public static final class Evaluation {
        public Color color = Color.GRIS;
        public int score, economyScore;
        public double km, minutes, electricKm, fuelKm, energyCost, upkeepCost, fixedCost, extraCost;
        public double revenue, margin, hourly, perKm, conservativeHourly, timePoints, kmPoints, pickupPoints;
        public ZoneRule.Assessment zones;
        public RiderProfile rider;
        public boolean riderBlocked, riderUnknown, riderCaution;
        public final List<String> reasons = new ArrayList<>();
        public String reason() { return reasons.isEmpty() ? "Sin evaluación" : reasons.get(0); }
        public String label() { return color == Color.AMBAR ? "ÁMBAR" : color.name(); }
    }
    public static Evaluation evaluate(OfferParser.Offer o, DriverConfig c, List<ZoneRule> zones, LocalDate date, int hour) {
        Evaluation e = new Evaluation();
        String error = c.validate();
        if (error != null || o == null) { e.reasons.add(error == null ? "Faltan datos de la oferta" : error); return e; }
        if (o.cents <= 0 || o.cents > 10_000_000 || o.pickupMinutes < 0 || o.pickupMinutes > 240 || o.tripMinutes <= 0 || o.tripMinutes > 600 || !Double.isFinite(o.totalKm()) || o.pickupKm < 0 || o.pickupKm > 300 || o.tripKm <= 0 || o.tripKm > 1000) {
            e.reasons.add("Datos de viaje inválidos"); return e;
        }
        e.km = o.totalKm() + c.repositionKm;
        e.minutes = o.totalMinutes() + c.passengerWaitMin + c.repositionMin;
        e.electricKm = c.energyMode.equals("GASOLINA") ? 0 : c.energyMode.equals("ELECTRICO") ? e.km : Math.min(e.km, c.remainingElectricKm);
        e.fuelKm = e.km - e.electricKm;
        e.energyCost = e.electricKm * c.kwhPer100Km / 100 * c.electricityPrice + e.fuelKm / c.kmPerLiter * c.fuelPrice;
        e.upkeepCost = e.km * (c.maintenanceCost / c.maintenanceIntervalKm + c.tiresCost / c.tiresLifeKm + c.wearPerKm);
        e.fixedCost = c.fixedMonthly / c.hoursMonthly * e.minutes / 60;
        e.extraCost = c.extrasPerTrip;
        e.revenue = o.cents / 100d * (1 - c.additionalFeePercent / 100);
        e.margin = e.revenue - e.energyCost - e.upkeepCost - e.fixedCost - e.extraCost;
        e.hourly = e.margin * 60 / e.minutes; e.perKm = e.margin / e.km;
        double extraFixed = c.fixedMonthly / c.hoursMonthly * c.conservativeExtraMin / 60;
        e.conservativeHourly = (e.margin - extraFixed) * 60 / (e.minutes + c.conservativeExtraMin);
        e.timePoints = points(e.hourly, c.minHourly, c.targetHourly);
        e.kmPoints = points(e.perKm, c.minPerKm, c.targetPerKm);
        e.pickupPoints = Math.max(0, 100 - 80 * Math.max(o.pickupMinutes / c.maxPickupMin, o.pickupKm / c.maxPickupKm));
        double weights = c.weightHourly + c.weightKm + c.weightPickup;
        e.economyScore = (int)Math.round((e.timePoints*c.weightHourly + e.kmPoints*c.weightKm + e.pickupPoints*c.weightPickup)/weights);
        e.score = e.economyScore;
        e.zones = ZoneRule.assess(o, zones, date, hour);
        boolean belowFloor = e.hourly < c.minHourly || e.perKm < c.minPerKm || e.margin <= 0;
        boolean pickupTooLong = o.pickupMinutes > c.maxPickupMin || o.pickupKm > c.maxPickupKm;
        boolean unavailableRange = c.energyMode.equals("ELECTRICO") && e.km > c.remainingElectricKm;
        e.color = e.score >= c.greenScore && e.hourly >= c.targetHourly ? Color.VERDE
                : e.score >= c.amberScore ? Color.AMBAR : Color.ROJO;
        e.rider=o.rider;
        if(c.riderFilter) {
            boolean newByCount=o.rider.count!=null && o.rider.count<c.newRiderMinCount;
            if(c.blockNewRider && (o.rider.explicitNew || newByCount)) {
                e.riderBlocked=true;e.reasons.add(o.rider.explicitNew?"Usuario NUEVO: rojo por tu regla":"Usuario nuevo por tu umbral: "+o.rider.count+" < "+(int)c.newRiderMinCount+" visibles");
            }
            if(o.rider.rating!=null && o.rider.rating<c.minRiderRating) {
                e.riderBlocked=true;e.reasons.add("Calificación del pasajero bajo tu mínimo de "+String.format(java.util.Locale.US,"%.2f",c.minRiderRating));
            }
            if(e.riderBlocked){e.color=Color.ROJO;e.score=Math.min(e.score,(int)Math.ceil(c.amberScore)-1);}
            e.riderUnknown=o.rider.ambiguous || !o.rider.explicitNew && (o.rider.rating==null || o.rider.count==null);
            e.riderCaution=!e.riderBlocked && (o.rider.explicitNew || o.rider.rating!=null && o.rider.rating<c.goodRiderRating
                    || o.rider.count!=null && o.rider.count<c.establishedRiderCount);
        }
        if (c.zoneFilter && e.zones.blocked) {
            e.color = Color.ROJO; e.score = Math.min(e.score, (int)Math.ceil(c.amberScore)-1); e.reasons.add(e.zones.reason);
        }
        if (unavailableRange) { e.color = Color.ROJO; e.score = Math.min(e.score, (int)Math.ceil(c.amberScore)-1); e.reasons.add("Autonomía eléctrica insuficiente"); }
        if (belowFloor) {
            e.color = Color.ROJO; e.score = Math.min(e.score, (int)Math.ceil(c.amberScore)-1);
            e.reasons.add(e.hourly < c.minHourly ? "Bajo tu mínimo por hora" : "Bajo tu mínimo por kilómetro");
        }
        if (pickupTooLong) { e.color = Color.ROJO; e.score = Math.min(e.score, (int)Math.ceil(c.amberScore)-1); e.reasons.add("Recogida fuera de tus límites"); }
        if(e.riderCaution){e.score=Math.max(0,e.score-(int)Math.round(c.riderCautionPenalty));capAmber(e,c);e.reasons.add("Pasajero: calificación o historial bajo tu meta");}
        if(e.riderUnknown){capAmber(e,c);e.reasons.add(o.rider.ambiguous?"Pasajero: lectura ambigua; sin verde":"Pasajero: faltan calificación o contador; sin verde");}
        if (c.zoneFilter && e.zones.caution) {
            e.score = Math.max(0, e.score - (int)Math.round(c.cautionPenalty)); capAmber(e,c); e.reasons.add(e.zones.reason);
        }
        if (c.zoneFilter && e.zones.unknown && !e.zones.blocked) { capAmber(e,c); e.reasons.add("Riesgo de zona no verificado"); }
        if(o.destinationNoticeCount>0){
            capAmber(e,c);e.reasons.add("Aviso de destinos: revisar paradas, direcciones y esperas; sin verde");
        }
        if (!c.calibrated) { capAmber(e,c); e.reasons.add("Costos y metas iniciales sin revisar"); }
        if (e.conservativeHourly < c.minHourly && !belowFloor) { capAmber(e,c); e.reasons.add("Una espera extra lo deja bajo tu mínimo"); }
        if (e.reasons.isEmpty()) e.reasons.add(e.color == Color.VERDE ? "Cumple tus metas y reglas" : e.color == Color.ROJO ? "Calificación bajo tu mínimo" : "No alcanza tu meta completa");
        if (!c.zoneFilter) e.reasons.add("Filtro de zonas desactivado");
        if (!c.riderFilter) e.reasons.add("Filtro de pasajero desactivado");
        return e;
    }
    private static void capAmber(Evaluation e, DriverConfig c) {
        if (e.color == Color.VERDE) e.color = Color.AMBAR;
        e.score = Math.min(e.score, (int)Math.ceil(c.greenScore)-1);
        if (e.score < c.amberScore && e.color == Color.AMBAR) e.color = Color.ROJO;
    }
    static double points(double value, double floor, double target) {
        if (value <= floor) return Math.max(0, 50 * value / floor);
        if (value <= target) return 50 + 30 * (value-floor)/(target-floor);
        return Math.min(100, 80 + 20 * (value-target)/(target*.25));
    }
}
