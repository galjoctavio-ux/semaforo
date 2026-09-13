package mx.tesivil.detector;

/** Driver-reported result. Missing costs remain unknown, never estimated actual costs. */
public final class TripOutcome {
    public enum State { UNKNOWN, DECLINED, ACCEPTED, COMPLETED, CANCELLED }
    public enum Reason { UNSPECIFIED, ECONOMY, PICKUP, ZONE, RIDER_RULE, OTHER }
    public State state=State.UNKNOWN;
    public Reason reason=Reason.UNSPECIFIED;
    public Long settledCents,energyCents,upkeepCents,fixedCents,extrasCents;
    public Double totalMinutes,totalKm,waitMinutes,repositionMinutes,repositionKm;
    public String validate(){
        if(state==null || reason==null)return "Estado o motivo inválido";
        if(state!=State.COMPLETED)return hasFigures()?"Solo un viaje completado admite cifras reales":null;
        if(!validMoney(settledCents) || totalMinutes==null || totalKm==null)return "Completa importe recibido, tiempo total y km totales";
        if(!validNumber(totalMinutes,1440) || totalMinutes<=0 || !validNumber(totalKm,2000) || totalKm<=0)return "Revisa tiempo y km reales";
        for(Long value:new Long[]{energyCents,upkeepCents,fixedCents,extrasCents})if(value!=null && !validMoney(value))return "Costo real inválido";
        if(waitMinutes!=null && !validNumber(waitMinutes,totalMinutes) || repositionMinutes!=null && !validNumber(repositionMinutes,totalMinutes)
                || repositionKm!=null && !validNumber(repositionKm,totalKm))return "Espera o regreso supera el total real";
        if(waitMinutes!=null && repositionMinutes!=null && waitMinutes+repositionMinutes>totalMinutes)return "Espera y regreso superan el tiempo total";
        return null;
    }
    private boolean hasFigures(){return settledCents!=null || totalMinutes!=null || totalKm!=null || waitMinutes!=null || repositionMinutes!=null || repositionKm!=null || energyCents!=null || upkeepCents!=null || fixedCents!=null || extrasCents!=null;}
    private static boolean validMoney(Long n){return n!=null && n>=0 && n<=10_000_000;}
    private static boolean validNumber(double n,double max){return Double.isFinite(n) && n>=0 && n<=max;}
    public Double actualMargin(){return state==State.COMPLETED && validate()==null && energyCents!=null && upkeepCents!=null && fixedCents!=null && extrasCents!=null
            ?(settledCents-energyCents-upkeepCents-fixedCents-extrasCents)/100d:null;}
    public Double actualHourly(){Double margin=actualMargin();return margin==null?null:margin*60/totalMinutes;}
}
