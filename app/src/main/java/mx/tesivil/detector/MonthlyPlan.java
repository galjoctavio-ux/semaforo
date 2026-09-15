package mx.tesivil.detector;

/** A driver-entered online-hour scenario, never a forecast inferred from an offer. */
public final class MonthlyPlan {
    public static final class Result {
        public double hours, fixedMonthly, requiredHourly, monthlyContribution, afterFixed, uncovered, outsideUber;
        public double afterAllCarFixed;
        public Double breakEvenHours, breakEvenWeeklyHours;
        public boolean known;
        public ScoreEngine.Color color=ScoreEngine.Color.GRIS;
        public String status="Falta un supuesto de aporte por hora de jornada";
    }
    public static Result evaluate(DriverConfig c){
        String error=c.validate();if(error!=null)throw new IllegalArgumentException(error);
        Result r=new Result();r.hours=c.hoursMonthly;r.fixedMonthly=c.allocatedFixedMonthly();
        r.requiredHourly=r.fixedMonthly/r.hours;
        r.outsideUber=c.fixedMonthly*(1-c.uberUsePercent/100);
        r.known=c.monthlyPlanConfigured;
        if(!r.known)return r;
        r.monthlyContribution=c.plannedContributionHourly*r.hours;
        r.afterFixed=r.monthlyContribution-r.fixedMonthly;r.uncovered=Math.max(0,-r.afterFixed);
        r.afterAllCarFixed=r.monthlyContribution-c.fixedMonthly-c.uberOnlyFixedMonthly;
        if(r.fixedMonthly==0)r.breakEvenHours=0d;
        else if(c.plannedContributionHourly>0)r.breakEvenHours=r.fixedMonthly/c.plannedContributionHourly;
        if(r.breakEvenHours!=null)r.breakEvenWeeklyHours=r.breakEvenHours/OnboardingProfiles.WEEKS_PER_MONTH;
        r.color=r.afterFixed<-.000001?ScoreEngine.Color.ROJO:r.afterFixed>.000001?ScoreEngine.Color.VERDE:ScoreEngine.Color.AMBAR;
        r.status=r.color==ScoreEngine.Color.ROJO?"El supuesto no cubre los gastos asignados a Uber"
                :r.color==ScoreEngine.Color.AMBAR?"Solo alcanza para cubrir los gastos asignados"
                :"El supuesto cubre gastos asignados y deja un saldo";
        if(r.color==ScoreEngine.Color.VERDE && r.outsideUber>0 && r.afterAllCarFixed<-.000001){
            r.color=ScoreEngine.Color.AMBAR;r.status="Cubre la parte de Uber; el resto necesita otra fuente de pago";
        }
        return r;
    }
    private MonthlyPlan(){}
}
