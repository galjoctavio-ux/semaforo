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
        public boolean tripBasis, fixedShortfall;
        public double financedWearReserve, contribution, contributionHourly, contributionPerKm, conservativeContributionHourly;
        public double decisionMargin, decisionHourly, decisionPerKm, decisionConservativeHourly;
        public double allocatedFixedMonthly;
        public ZoneRule.Assessment zones;
        public RiderProfile rider;
        public boolean riderBlocked, riderUnknown, riderCaution, zoneBlocked;
        public boolean profilePending, rangeStale, pickupDisproportionate, belowFloor, returnBelowFloor;
        public double pickupKmShare, pickupTimeShare, extraWaitToleranceMin, minimumFare;
        public Double returnKm,returnMinutes,returnMargin,returnHourly,returnPerKm;
        public Double returnContribution,returnDecisionHourly,returnDecisionPerKm;
        public String economyStatus="Sin evaluación";
        public String riderLimit="";
        public final List<String> pending=new ArrayList<>(), alerts=new ArrayList<>();
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
        e.tripBasis=c.tripBasis();e.allocatedFixedMonthly=c.allocatedFixedMonthly();
        e.fixedCost = e.allocatedFixedMonthly / c.hoursMonthly * e.minutes / 60;
        e.extraCost = c.extrasPerTrip;
        e.revenue = o.cents / 100d * (1 - c.additionalFeePercent / 100);
        e.margin = e.revenue - e.energyCost - e.upkeepCost - e.fixedCost - e.extraCost;
        e.hourly = e.margin * 60 / e.minutes; e.perKm = e.margin / e.km;
        e.financedWearReserve=e.km*c.financedWearRate();
        e.contribution=e.revenue-e.energyCost-e.upkeepCost-e.extraCost-e.financedWearReserve;
        e.contributionHourly=e.contribution*60/e.minutes;e.contributionPerKm=e.contribution/e.km;
        e.conservativeContributionHourly=e.contribution*60/(e.minutes+c.conservativeExtraMin);
        e.decisionMargin=e.tripBasis?e.contribution:e.margin;
        e.decisionHourly=e.tripBasis?e.contributionHourly:e.hourly;e.decisionPerKm=e.tripBasis?e.contributionPerKm:e.perKm;
        double extraFixed = e.allocatedFixedMonthly / c.hoursMonthly * c.conservativeExtraMin / 60;
        e.conservativeHourly = (e.margin - extraFixed) * 60 / (e.minutes + c.conservativeExtraMin);
        e.decisionConservativeHourly=e.tripBasis?e.conservativeContributionHourly:e.conservativeHourly;
        e.timePoints = points(e.decisionHourly, c.minHourly, c.targetHourly);
        e.kmPoints = points(e.decisionPerKm, c.minPerKm, c.targetPerKm);
        e.pickupPoints = Math.max(0, 100 - 80 * Math.max(o.pickupMinutes / c.maxPickupMin, o.pickupKm / c.maxPickupKm));
        double weights = c.weightHourly + c.weightKm + c.weightPickup;
        e.economyScore = (int)Math.round((e.timePoints*c.weightHourly + e.kmPoints*c.weightKm + e.pickupPoints*c.weightPickup)/weights);
        e.score = e.economyScore;
        e.pickupKmShare=100*o.pickupKm/o.totalKm();e.pickupTimeShare=100d*o.pickupMinutes/o.totalMinutes();
        e.pickupDisproportionate=Math.max(e.pickupKmShare,e.pickupTimeShare)>=c.pickupShareAlertPercent;
        if(e.pickupDisproportionate)e.alerts.add("Recogida: "+Math.round(e.pickupKmShare)+"% de km · "+Math.round(e.pickupTimeShare)+"% del tiempo visible");
        double fixedHourly=e.allocatedFixedMonthly/c.hoursMonthly;
        double decisionFixedHourly=e.tripBasis?0:fixedHourly;
        double hourlyTolerance=(60*e.decisionMargin-c.minHourly*e.minutes)/(c.minHourly+decisionFixedHourly);
        double kmTolerance=decisionFixedHourly==0?Double.POSITIVE_INFINITY:(e.decisionMargin-c.minPerKm*e.km)*60/decisionFixedHourly;
        e.extraWaitToleranceMin=Math.max(0,Math.min(hourlyTolerance,kmTolerance));
        if(e.decisionMargin<c.minPerKm*e.km || e.decisionMargin<c.minHourly*e.minutes/60 || e.decisionMargin<=0)e.extraWaitToleranceMin=0;
        e.minimumFare=(e.energyCost+e.upkeepCost+(e.tripBasis?e.financedWearReserve:e.fixedCost)+e.extraCost+Math.max(c.minHourly*e.minutes/60,c.minPerKm*e.km))/(1-c.additionalFeePercent/100);
        e.minimumFare=Math.ceil(e.minimumFare*100-1e-8)/100;
        e.profilePending=!c.pendingProfile().isEmpty();e.rangeStale=c.rangeStale(System.currentTimeMillis());
        if(c.usesPresetEstimates())e.alerts.add("Costos del perfil: estimaciones iniciales, ajustables");
        if(e.profilePending)e.pending.add("Perfil: falta revisar "+String.join(", ",c.pendingProfile()));
        if(e.rangeStale)e.pending.add("Autonomía sin actualizar en las últimas 24 h");
        if(c.returnScenarioEnabled){
            // Alternative replaces the base repositioning; it does not count it twice.
            e.returnKm=o.totalKm()+c.returnScenarioKm;e.returnMinutes=o.totalMinutes()+c.passengerWaitMin+c.returnScenarioMin;
            double electric=c.energyMode.equals("GASOLINA")?0:c.energyMode.equals("ELECTRICO")?e.returnKm:Math.min(e.returnKm,c.remainingElectricKm);
            double energy=electric*c.kwhPer100Km/100*c.electricityPrice+(e.returnKm-electric)/c.kmPerLiter*c.fuelPrice;
            double upkeep=e.returnKm*(c.maintenanceCost/c.maintenanceIntervalKm+c.tiresCost/c.tiresLifeKm+c.wearPerKm);
            e.returnMargin=e.revenue-energy-upkeep-fixedHourly*e.returnMinutes/60-e.extraCost;
            e.returnHourly=e.returnMargin*60/e.returnMinutes;e.returnPerKm=e.returnMargin/e.returnKm;
            e.returnContribution=e.revenue-energy-upkeep-e.returnKm*c.financedWearRate()-e.extraCost;
            double returnDecisionMargin=e.tripBasis?e.returnContribution:e.returnMargin;
            e.returnDecisionHourly=returnDecisionMargin*60/e.returnMinutes;e.returnDecisionPerKm=returnDecisionMargin/e.returnKm;
            e.returnBelowFloor=e.returnDecisionHourly<c.minHourly || e.returnDecisionPerKm<c.minPerKm || returnDecisionMargin<=0
                    || c.energyMode.equals("ELECTRICO") && e.returnKm>c.remainingElectricKm;
            if(e.returnBelowFloor)e.alerts.add("Con tu escenario de regreso no cumple los mínimos o la autonomía");
        }else if(c.repositionKm==0 && c.repositionMin==0)e.pending.add("Regreso no considerado");
        if(o.reserved)e.pending.add("Reserva: horario y espera previa por verificar");
        if(o.destinationNoticeCount>0)e.pending.add("Aviso de destinos: revisar paradas y esperas");
        e.zones = ZoneRule.assess(o, zones, date, hour);
        e.zoneBlocked=c.zoneFilter && e.zones.blocked;
        boolean belowFloor = e.decisionHourly < c.minHourly || e.decisionPerKm < c.minPerKm || e.decisionMargin <= 0;
        e.belowFloor=belowFloor;
        e.economyStatus=belowFloor?"Bajo tus mínimos":e.decisionHourly>=c.targetHourly && e.decisionPerKm>=c.targetPerKm?"Cumple metas económicas":"Supera mínimos; falta alcanzar metas";
        boolean pickupTooLong = o.pickupMinutes > c.maxPickupMin || o.pickupKm > c.maxPickupKm;
        boolean unavailableRange = c.energyMode.equals("ELECTRICO") && e.km > c.remainingElectricKm;
        e.color = e.score >= c.greenScore && e.decisionHourly >= c.targetHourly ? Color.VERDE
                : e.score >= c.amberScore ? Color.AMBAR : Color.ROJO;
        e.rider=o.rider;
        if(c.riderFilter) {
            boolean newByCount=o.rider.count!=null && o.rider.count<c.newRiderMinCount;
            if(c.blockNewRider && (o.rider.explicitNew || newByCount)) {
                e.riderBlocked=true;e.reasons.add(o.rider.explicitNew?"Usuario NUEVO: rojo por tu regla":"Usuario nuevo por tu umbral: historial visible "+o.rider.count+" < "+(int)c.newRiderMinCount+" (regla personal)");
                e.riderLimit=o.rider.explicitNew?"NUEVO visible · tu regla":"Historial "+o.rider.count+" < "+(int)c.newRiderMinCount+" visibles · tu regla";
            }
            if(o.rider.rating!=null && o.rider.rating<c.minRiderRating) {
                e.riderBlocked=true;e.reasons.add("Calificación del pasajero bajo tu mínimo de "+String.format(java.util.Locale.US,"%.2f",c.minRiderRating));
                if(e.riderLimit.isEmpty())e.riderLimit=String.format(java.util.Locale.US,"Estrellas %.2f < %.2f · tu regla",o.rider.rating,c.minRiderRating);
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
            e.reasons.add(e.decisionHourly < c.minHourly ? "Bajo tu mínimo por hora" : "Bajo tu mínimo por kilómetro");
        }
        if (pickupTooLong) { e.color = Color.ROJO; e.score = Math.min(e.score, (int)Math.ceil(c.amberScore)-1); e.reasons.add("Recogida fuera de tus límites"); }
        if(e.riderCaution){e.score=Math.max(0,e.score-(int)Math.round(c.riderCautionPenalty));capAmber(e,c);e.reasons.add("Pasajero: calificación o historial bajo tu meta");}
        if(e.riderUnknown){capAmber(e,c);e.reasons.add(o.rider.ambiguous?"Pasajero: lectura ambigua; sin verde":"Pasajero: faltan calificación o contador; sin verde");}
        if(e.riderUnknown)e.pending.add("Pasajero: datos ausentes o ambiguos");
        if (c.zoneFilter && e.zones.caution) {
            e.score = Math.max(0, e.score - (int)Math.round(c.cautionPenalty)); capAmber(e,c); e.reasons.add(e.zones.reason);
        }
        if (c.zoneFilter && e.zones.unknown && !e.zones.blocked) { capAmber(e,c); e.reasons.add("Riesgo de zona no verificado"); }
        if(c.zoneFilter && e.zones.pickupUnknown)e.pending.add("Zonas: recogida · "+e.zones.pickupStatus);
        if(c.zoneFilter && e.zones.destinationUnknown)e.pending.add("Zonas: destino · "+e.zones.destinationStatus);
        if(o.destinationNoticeCount>0){
            capAmber(e,c);e.reasons.add("Aviso de destinos: revisar paradas, direcciones y esperas; sin verde");
        }
        if(o.reserved){capAmber(e,c);e.reasons.add("Reserva: horario y espera previa por verificar; sin verde");}
        if (e.profilePending) { capAmber(e,c); e.reasons.add("Costos y metas iniciales sin revisar"); }
        if(e.rangeStale){capAmber(e,c);e.reasons.add("Autonomía pendiente de actualizar; sin verde");}
        if(e.returnBelowFloor){capAmber(e,c);e.reasons.add("Con tu escenario de regreso no cumple tus mínimos; sin verde");}
        if (e.decisionConservativeHourly < c.minHourly && !belowFloor) { capAmber(e,c); e.reasons.add("Una espera extra lo deja bajo tu mínimo"); }
        e.fixedShortfall=e.tripBasis && e.contribution>0 && e.margin<0;
        if(e.fixedShortfall){
            capAmber(e,c);
            e.reasons.add("Aporta al viaje; no cubre los gastos fijos asignados a este tiempo");
            e.alerts.add("Saldo después de fijos negativo: revisa Plan del mes. No es una pérdida adicional causada por este viaje.");
        }
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
