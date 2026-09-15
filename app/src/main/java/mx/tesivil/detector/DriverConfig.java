package mx.tesivil.detector;

/** Editable assumptions, not manufacturer specifications or market quotations. */
public final class DriverConfig {
    public String vehicle = "Mi vehículo · ejemplo por configurar";
    public String energyMode = "GASOLINA";
    public double modelYear = 0, odometerKm = 0;
    public double fuelPrice = 25, kmPerLiter = 12, kwhPer100Km = 20, electricityPrice = 0;
    public double electricRangeKm = 100, remainingElectricKm = 100;
    public double maintenanceCost = 3000, maintenanceIntervalKm = 10000;
    public double tiresCost = 10000, tiresLifeKm = 50000, wearPerKm = 1;
    public double fixedMonthly = 3000, hoursMonthly = 160;
    public double passengerWaitMin = 3, conservativeExtraMin = 5;
    public double repositionKm = 0, repositionMin = 0, extrasPerTrip = 0, additionalFeePercent = 0;
    public double minHourly = 120, targetHourly = 180, minPerKm = 5, targetPerKm = 8;
    public double maxPickupMin = 15, maxPickupKm = 7;
    public double weightHourly = 55, weightKm = 30, weightPickup = 15;
    public double greenScore = 75, amberScore = 45, cautionPenalty = 20;
    public boolean zoneFilter = true, calibrated = false;
    public boolean vehicleConfirmed=false, energyReviewed=false, costsReviewed=false, goalsReviewed=false;
    public long rangeUpdatedAtMs=0;
    public double pickupShareAlertPercent=60;
    public boolean returnScenarioEnabled=false;
    public double returnScenarioKm=10, returnScenarioMin=20;
    public boolean riderFilter = true, blockNewRider = true;
    public boolean captureWholeScreen = true;
    public double newRiderMinCount = 5, establishedRiderCount = 20;
    public double minRiderRating = 4.70, goodRiderRating = 4.85, riderCautionPenalty = 10;
    public long revision = 1;
    public boolean onboardingCompleted=false, presetAccepted=false;
    public String presetId="", ownership="OWNED";
    public long presetVersion=0;
    public double carPaymentMonthly=0;
    public boolean rentalIncludesUpkeep=false;
    // Legacy fixedMonthly was already assigned to Uber: 100% and TOTAL preserve that calculation.
    public String carUse="UNSET", evaluationBasis="TOTAL";
    public double uberUsePercent=100, uberOnlyFixedMonthly=0;
    public double financedWearPerKm=0, plannedContributionHourly=0;
    public boolean monthlyPlanConfigured=false;

    public boolean tripBasis(){return evaluationBasis.equals("TRIP");}
    public double allocatedFixedMonthly(){return fixedMonthly*uberUsePercent/100+uberOnlyFixedMonthly;}
    public double financedWearRate(){return ownership.equals("FINANCED")?financedWearPerKm:0;}
    public String basisLabel(){return tripBasis()?"Aporte del viaje antes de gastos fijos":"Saldo después de gastos fijos y pagos";}

    public boolean usesPresetEstimates() {
        return presetAccepted && !presetId.isBlank() && (!energyReviewed || !costsReviewed);
    }
    public String profileStatus() {
        if(usesPresetEstimates())return "Perfil inicial con costos estimados · puedes afinarlos en Ajustes avanzados";
        return pendingProfile().isEmpty()?"Costos marcados como revisados por ti":"Perfil pendiente: "+String.join(", ",pendingProfile());
    }

    public java.util.List<String> pendingProfile() {
        java.util.List<String> out=new java.util.ArrayList<>();
        if(!vehicleConfirmed)out.add("vehículo y energía");
        boolean acceptedPreset=presetAccepted && !presetId.isBlank();
        if(!energyReviewed && !acceptedPreset)out.add("precio y rendimiento de energía");
        if(!costsReviewed && !acceptedPreset)out.add("mantenimiento, desgaste y fijos");
        if(!goalsReviewed)out.add("metas y esperas");
        if(tripBasis() && ownership.equals("FINANCED") && financedWearPerKm==0 && wearPerKm==0)out.add("reserva de desgaste del carro financiado");
        if(!calibrated && !acceptedPreset && out.isEmpty())out.add("revisión general del perfil");
        return out;
    }
    public boolean rangeStale(long now) {
        return !energyMode.equals("GASOLINA") && (rangeUpdatedAtMs<=0 || rangeUpdatedAtMs>now || now-rangeUpdatedAtMs>86_400_000L);
    }

    public String validate() {
        if (vehicle == null || vehicle.isBlank() || vehicle.length() > 100) return "Escribe el modelo (máximo 100 caracteres)";
        if(presetId==null || presetId.length()>100 || ownership==null || !java.util.Set.of("OWNED","FINANCED","RENTED").contains(ownership)
                || presetVersion<0 || presetAccepted && (presetId.isBlank() || presetVersion<1 || !onboardingCompleted))return "Perfil inicial inválido";
        if (energyMode == null || !java.util.Set.of("PHEV", "GASOLINA", "ELECTRICO").contains(energyMode)) return "Modo de energía inválido";
        if(carUse==null || !java.util.Set.of("UNSET","EXCLUSIVE","MIXED").contains(carUse)
                || evaluationBasis==null || !java.util.Set.of("TOTAL","TRIP").contains(evaluationBasis))return "Revisa el uso del carro y cómo comparas las ofertas";
        try {
            for (java.lang.reflect.Field f : getClass().getFields()) if (f.getType() == double.class) {
                double v = f.getDouble(this);
                if (!Double.isFinite(v) || v < 0 || v > 10_000_000) return "Valores numéricos fuera de rango: " + f.getName();
            }
        } catch (IllegalAccessException e) { return "No se pudo validar el perfil"; }
        if (kmPerLiter < .1 || kmPerLiter > 100 || kwhPer100Km < .1 || kwhPer100Km > 200) return "Revisa los rendimientos de energía";
        if (maintenanceIntervalKm < 1 || tiresLifeKm < 1 || hoursMonthly < 1 || hoursMonthly > 744) return "Intervalos y horas deben ser al menos 1";
        if(uberUsePercent<1 || uberUsePercent>100 || carUse.equals("EXCLUSIVE") && uberUsePercent!=100)return "El uso para Uber debe estar entre 1 y 100%; uso exclusivo requiere 100%";
        if(plannedContributionHourly>10000)return "Revisa el aporte previsto por hora de jornada (máximo $10,000)";
        if (modelYear != 0 && (modelYear < 1980 || modelYear > 2100 || modelYear != Math.rint(modelYear))) return "Año inválido (0 significa sin registrar)";
        if (remainingElectricKm > electricRangeKm) return "La autonomía restante supera la autonomía cargado";
        if(rangeUpdatedAtMs<0) return "Fecha de autonomía inválida";
        if(pickupShareAlertPercent<1 || pickupShareAlertPercent>100) return "La alerta de proporción debe estar entre 1 y 100%";
        if(returnScenarioKm>300 || returnScenarioMin>300 || returnScenarioEnabled && returnScenarioKm==0 && returnScenarioMin==0) return "Revisa el escenario de regreso (0–300 km/min; no ambos en cero si está activo)";
        if (additionalFeePercent > 50) return "Revisa el descuento adicional; máximo 50%";
        if (passengerWaitMin > 120 || conservativeExtraMin > 120 || repositionMin > 300 || repositionKm > 300) return "Revisa las esperas y el reposicionamiento";
        if (minHourly < .01 || targetHourly <= minHourly || minPerKm < .01 || targetPerKm <= minPerKm) return "Cada meta debe superar su mínimo y los mínimos deben ser al menos 0.01";
        if (maxPickupMin <= 0 || maxPickupMin > 240 || maxPickupKm <= 0 || maxPickupKm > 300) return "Límites de recogida fuera de rango";
        if (weightHourly + weightKm + weightPickup <= 0 || Math.max(weightHourly, Math.max(weightKm, weightPickup)) > 100) return "Los pesos deben estar entre 0 y 100 y sumar más de cero";
        if (amberScore <= 0 || greenScore <= amberScore || greenScore > 100 || cautionPenalty > 100) return "Revisa los límites del score (0 a 100)";
        if (minRiderRating < 1 || minRiderRating > 5 || goodRiderRating < minRiderRating || goodRiderRating > 5) return "Calificaciones de pasajero: de 1 a 5; la meta no puede ser menor al mínimo";
        if (newRiderMinCount != Math.rint(newRiderMinCount) || establishedRiderCount != Math.rint(establishedRiderCount)
                || newRiderMinCount < 1 || establishedRiderCount < newRiderMinCount || establishedRiderCount > 1_000_000 || riderCautionPenalty > 100) return "Revisa los contadores enteros del pasajero y su penalización";
        return null;
    }
}
