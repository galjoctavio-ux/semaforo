package mx.tesivil.detector;

/** Starting assumptions by vehicle class, never model specifications or market averages. */
public final class OnboardingProfiles {
    public static final long VERSION=2;
    public static final double WEEKS_PER_MONTH=52d/12;
    public enum Vehicle {
        COMPACT("Compacto a gasolina","Ejemplos de tamaño: March, Spark","GASOLINA",12,2500,8000,50000,.7,20,0),
        SEDAN("Sedán a gasolina","Ejemplos de tipo: Aveo, Versa, Vento, K3","GASOLINA",11,3000,10000,50000,1,20,0),
        SUV("SUV a gasolina","Camioneta de uso familiar","GASOLINA",8,4000,14000,50000,1.4,22,0),
        HYBRID("Híbrido sin enchufe","Ejemplos de tipo: Prius o Corolla híbrido","GASOLINA",18,3500,10000,50000,1.1,20,0),
        PHEV("PHEV: eléctrico + gasolina","Ejemplo de tipo: Captiva PHEV","PHEV",12,3000,12000,50000,1.16,20,100),
        ELECTRIC("100% eléctrico","Carro que se carga y no usa gasolina","ELECTRICO",12,2000,12000,40000,1.2,18,250);

        public final String title, examples, energyMode;
        private final double kmPerLiter, maintenance, tires, tiresLife, wear, kwh, range;
        Vehicle(String title,String examples,String energyMode,double kmPerLiter,double maintenance,double tires,double tiresLife,double wear,double kwh,double range) {
            this.title=title;this.examples=examples;this.energyMode=energyMode;this.kmPerLiter=kmPerLiter;
            this.maintenance=maintenance;this.tires=tires;this.tiresLife=tiresLife;this.wear=wear;this.kwh=kwh;this.range=range;
        }
        public boolean needsCharge(){return !energyMode.equals("GASOLINA");}
        public double exampleRange(){return range;}
        public DriverConfig profile(){
            DriverConfig c=new DriverConfig();
            c.vehicle=title+" · perfil inicial";c.energyMode=energyMode;
            c.presetId=name().toLowerCase(java.util.Locale.ROOT);c.presetVersion=VERSION;
            c.fuelPrice=25;c.kmPerLiter=kmPerLiter;c.kwhPer100Km=kwh;c.electricityPrice=3;
            c.electricRangeKm=range;c.remainingElectricKm=0;c.rangeUpdatedAtMs=0;
            c.maintenanceCost=maintenance;c.maintenanceIntervalKm=10000;
            c.tiresCost=tires;c.tiresLifeKm=tiresLife;c.wearPerKm=wear;
            c.fixedMonthly=3000;c.hoursMonthly=40*WEEKS_PER_MONTH;
            return c;
        }
    }
    public enum Ownership { OWNED, FINANCED, RENTED }
    public enum Use { EXCLUSIVE, MIXED }
    public enum Charging {
        HOME("En casa · estimación $3/kWh",3), PUBLIC("Carga pública · estimación $6/kWh",6), FREE("No pago la carga · $0/kWh",0);
        public final String label;public final double price;
        Charging(String label,double price){this.label=label;this.price=price;}
    }
    public enum Strategy {
        FLEXIBLE("Flexible","Mínimo $100/h · meta $140/h",100,140,4,6),
        BALANCED("Equilibrado","Mínimo $120/h · meta $180/h",120,180,5,8),
        SELECTIVE("Selectivo","Mínimo $150/h · meta $220/h",150,220,6,10);
        public final String title,description;
        public final double minHourly,targetHourly,minPerKm,targetPerKm;
        Strategy(String title,String description,double minHourly,double targetHourly,double minPerKm,double targetPerKm) {
            this.title=title;this.description=description;this.minHourly=minHourly;this.targetHourly=targetHourly;this.minPerKm=minPerKm;this.targetPerKm=targetPerKm;
        }
        void apply(DriverConfig c){c.minHourly=minHourly;c.targetHourly=targetHourly;c.minPerKm=minPerKm;c.targetPerKm=targetPerKm;}
    }
    public static DriverConfig build(Vehicle vehicle,Ownership ownership,double payment,double weeklyHours,
            boolean rentalIncludesUpkeep,Charging charging,double remainingKm,Strategy strategy,DriverConfig previous,long now) {
        return build(vehicle,ownership,payment,weeklyHours,rentalIncludesUpkeep,charging,remainingKm,strategy,Use.EXCLUSIVE,100,previous,now);
    }
    public static DriverConfig build(Vehicle vehicle,Ownership ownership,double payment,double weeklyHours,
            boolean rentalIncludesUpkeep,Charging charging,double remainingKm,Strategy strategy,Use use,double uberPercent,DriverConfig previous,long now) {
        if(vehicle==null || ownership==null || charging==null || strategy==null || previous==null)throw new IllegalArgumentException("Elige tu perfil");
        if(use==null || !Double.isFinite(uberPercent) || uberPercent<1 || uberPercent>100 || use==Use.EXCLUSIVE && uberPercent!=100)throw new IllegalArgumentException("Revisa qué porcentaje de los kilómetros es para Uber");
        if(!Double.isFinite(weeklyHours) || weeklyHours<1 || weeklyHours>168)throw new IllegalArgumentException("Escribe de 1 a 168 horas por semana");
        if(ownership!=Ownership.OWNED && (!Double.isFinite(payment) || payment<=0 || payment>100000))throw new IllegalArgumentException("Escribe cuánto pagas por el carro");
        if(vehicle.needsCharge() && (!Double.isFinite(remainingKm) || remainingKm<0 || remainingKm>1000 || now<=0))throw new IllegalArgumentException("Revisa los km eléctricos restantes (0 a 1000)");
        DriverConfig c=vehicle.profile();
        // A new vehicle preset does not silently loosen the driver's personal filters or existing return scenario.
        c.riderFilter=previous.riderFilter;c.blockNewRider=previous.blockNewRider;c.newRiderMinCount=previous.newRiderMinCount;
        c.establishedRiderCount=previous.establishedRiderCount;c.minRiderRating=previous.minRiderRating;
        c.goodRiderRating=previous.goodRiderRating;c.riderCautionPenalty=previous.riderCautionPenalty;
        c.zoneFilter=previous.zoneFilter;c.captureWholeScreen=previous.captureWholeScreen;
        c.maxPickupMin=previous.maxPickupMin;c.maxPickupKm=previous.maxPickupKm;c.pickupShareAlertPercent=previous.pickupShareAlertPercent;
        c.passengerWaitMin=previous.passengerWaitMin;c.conservativeExtraMin=previous.conservativeExtraMin;
        c.weightHourly=previous.weightHourly;c.weightKm=previous.weightKm;c.weightPickup=previous.weightPickup;
        c.greenScore=previous.greenScore;c.amberScore=previous.amberScore;c.cautionPenalty=previous.cautionPenalty;
        c.repositionKm=previous.repositionKm;c.repositionMin=previous.repositionMin;
        c.returnScenarioEnabled=previous.returnScenarioEnabled;c.returnScenarioKm=previous.returnScenarioKm;c.returnScenarioMin=previous.returnScenarioMin;
        c.additionalFeePercent=previous.additionalFeePercent;c.extrasPerTrip=previous.extrasPerTrip;
        c.uberOnlyFixedMonthly=previous.uberOnlyFixedMonthly;
        // A different vehicle changes per-hour costs: do not reuse an income assumption silently.
        c.carUse=use.name();c.uberUsePercent=uberPercent;c.evaluationBasis="TRIP";
        c.ownership=ownership.name();c.rentalIncludesUpkeep=ownership==Ownership.RENTED && rentalIncludesUpkeep;
        c.carPaymentMonthly=ownership==Ownership.OWNED?0:ownership==Ownership.RENTED?payment*WEEKS_PER_MONTH:payment;
        c.fixedMonthly+=c.carPaymentMonthly;c.hoursMonthly=weeklyHours*WEEKS_PER_MONTH;
        // Keep a per-km reserve for loan cars when choosing a trip. The payment view
        // uses the full installment instead, so it does not also deduct that reserve.
        if(ownership==Ownership.FINANCED){c.financedWearPerKm=c.wearPerKm;c.wearPerKm=0;}
        if(ownership==Ownership.RENTED)c.wearPerKm=0;
        if(c.rentalIncludesUpkeep){c.maintenanceCost=0;c.tiresCost=0;}
        if(vehicle.needsCharge()){
            c.electricityPrice=charging.price;c.remainingElectricKm=remainingKm;
            c.electricRangeKm=Math.max(c.electricRangeKm,remainingKm);c.rangeUpdatedAtMs=now;
        }
        strategy.apply(c);c.onboardingCompleted=true;c.presetAccepted=true;
        c.vehicleConfirmed=true;c.goalsReviewed=true;
        // Acceptance of starting estimates is distinct from having checked bills or measured consumption.
        c.energyReviewed=false;c.costsReviewed=false;c.calibrated=false;
        String error=c.validate();if(error!=null)throw new IllegalArgumentException(error);
        return c;
    }
    private OnboardingProfiles(){}
}
