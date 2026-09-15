package mx.tesivil.detector;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

final class Ui {
    static final int INK = Color.rgb(24, 42, 52), TEAL = Color.rgb(8, 125, 147), BG = Color.rgb(243, 246, 248);
    static int dp(Context c, float value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable shape(int color, int radius, Context c) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d;
    }
    static TextView text(Context c, String text, float size, boolean bold) {
        TextView v = new TextView(c); v.setText(text); v.setTextSize(size); v.setTextColor(INK);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    static String km(double n) { return String.format(java.util.Locale.US, "%.1f", n); }
    static String money(long cents) { return String.format(java.util.Locale.US, "$%.2f", cents / 100d); }
    static String summary(OfferParser.Offer o) {
        return "Tipo: "+o.typeLabel()+"\nOferta " + money(o.cents) + "\nRecogida: " + o.pickupMinutes + " min · " + km(o.pickupKm) + " km"
                + "\nViaje: " + o.tripMinutes + " min · " + km(o.tripKm) + " km"
                + "\nTotal mostrado: " + o.totalMinutes() + " min · " + km(o.totalKm()) + " km"
                +(o.reserved?"\nReserva: revisar horario y espera previa":"")
                +(o.destinationNoticeCount>0?"\nAviso visible: "+o.destinationNoticeCount+" destino · revisar paradas y esperas":"");
    }
    static String amount(double value) { return String.format(java.util.Locale.US,"$%.2f",value); }
    static int signalColor(ScoreEngine.Evaluation e) {
        if(e==null)return Color.rgb(90,103,112);
        return color(e.color);
    }
    static int color(ScoreEngine.Color color){return switch(color){case VERDE->Color.rgb(22,112,61);case AMBAR->Color.rgb(148,88,0);case ROJO->Color.rgb(170,36,44);case GRIS->Color.rgb(90,103,112);};}
    static String monthlySummary(DriverConfig c){
        MonthlyPlan.Result r=MonthlyPlan.evaluate(c);
        return !r.known?"Mes: ingresos por estimar · fijos Uber "+amount(r.fixedMonthly)+"/mes"
                :"Mes simulado: "+(r.afterFixed<0?"faltan "+amount(r.uncovered):"saldo "+amount(r.afterFixed))+" · antes de impuestos";
    }
    static String monthlyPlan(DriverConfig c){
        MonthlyPlan.Result r=MonthlyPlan.evaluate(c);
        String s=(r.known?r.color==ScoreEngine.Color.AMBAR?"ÁMBAR":r.color.name():"SIN ESTIMACIÓN")+" · "+r.status
                +"\nHoras previstas: "+km(r.hours)+"/mes"
                +"\nFijos asignados a Uber: "+amount(r.fixedMonthly)+"/mes"
                +"\nAporte necesario para cubrirlos: "+amount(r.requiredHourly)+" por hora de jornada";
        if(r.known)s+="\nAporte mensual del supuesto: "+amount(r.monthlyContribution)
                +"\nSaldo después de fijos y pagos: "+amount(r.afterFixed)
                +(r.uncovered>0?"\nFaltante mensual: "+amount(r.uncovered):"")
                +(r.breakEvenWeeklyHours==null?"\nCon aporte $0/h no hay horas que cubran estos fijos":"\nPara cubrir fijos, sin sueldo: "+km(r.breakEvenWeeklyHours)+" h/semana");
        if(r.outsideUber>0)s+="\nParte compartida fuera de Uber: "+amount(r.outsideUber)+"/mes · requiere otra fuente de pago";
        if(r.known && r.outsideUber>0)s+="\nSi Uber debe pagar todos los fijos del carro: saldo "+amount(r.afterAllCarFixed);
        return s+"\nSupuesto, no ingreso confirmado. No incluye sueldo objetivo ni impuestos.";
    }
    static String evaluationSummary(ScoreEngine.Evaluation e) {
        if(e==null)return "Confirmando la lectura…";
        if(e.color==ScoreEngine.Color.GRIS)return e.reason();
        return (e.tripBasis?"Aporte del viaje: ":"Saldo tras fijos: ")+e.economyStatus
                +"\nAporte antes de fijos: "+amount(e.contribution)+"/viaje · "+amount(e.contributionHourly)+"/h · "+amount(e.contributionPerKm)+"/km"
                +"\nSaldo tras fijos y pagos: "+amount(e.margin)+"/viaje · "+amount(e.hourly)+"/h"
                +"\nLímite principal: "+restriction(e)
                +"\nDatos pendientes: "+(e.pending.isEmpty()?"ninguno de los comprobados por la app":String.join("; ",e.pending))
                +(e.alerts.isEmpty()?"":"\n"+String.join("\n",e.alerts))
                +"\n"+(e.belowFloor?"Ya está bajo tus mínimos económicos":"Espera extra tolerable estimada: "+km(Math.floor(e.extraWaitToleranceMin*10)/10)+" min")
                +"\nImporte mínimo económico estimado: "+amount(e.minimumFare)
                +(e.returnHourly==null?"":"\nCon regreso configurado (base elegida): "+amount(e.returnDecisionHourly)+"/h");
    }
    static String restriction(ScoreEngine.Evaluation e){
        if(e.zoneBlocked)return e.zones.reason;
        if(e.riderBlocked)return "Regla personal de pasajero: "+(e.rider.explicitNew?"NUEVO visible":e.reason());
        for(String reason:e.reasons)if(reason.contains("Autonomía eléctrica insuficiente") || reason.contains("Recogida fuera"))return reason;
        if(e.belowFloor)return "Bajo tus mínimos económicos";
        if(e.fixedShortfall)return "Aporta, pero falta cubrir fijos; revisa el mes";
        if(e.color==ScoreEngine.Color.ROJO)return "No alcanza tu calificación mínima";
        return e.reason();
    }
    static String compact(ScoreEngine.Evaluation e){
        if(e==null||e.color==ScoreEngine.Color.GRIS)return "Sin datos";
        long hourly=Math.round(e.decisionHourly);
        return "Est. "+(hourly<0?"−$"+Math.abs(hourly):"$"+hourly)+"/h";
    }
    static String details(ScoreEngine.Evaluation e,DriverConfig c){
        if(e==null)return "Todavía no hay una lectura confirmada";
        if(e.color==ScoreEngine.Color.GRIS)return "Sin cálculo válido: "+e.reason();
        return e.label()+" · "+c.basisLabel()+"\n"+restriction(e)
                +"\n\nAporte del viaje: "+amount(e.contribution)+" · "+amount(e.contributionHourly)+"/h · "+amount(e.contributionPerKm)+"/km"
                +"\nSaldo tras fijos y pagos: "+amount(e.margin)+" · "+amount(e.hourly)+"/h"
                +"\n\nOferta tras descuento adicional: "+amount(e.revenue)
                +"\nEnergía: "+amount(e.energyCost)
                +"\nReserva de mantenimiento, llantas y desgaste: "+amount(e.upkeepCost)
                +(e.financedWearReserve>0?"\nReserva de desgaste para comparar el viaje financiado: "+amount(e.financedWearReserve)+" (no se resta otra vez del saldo tras pagos)":"")
                +"\nExtras: "+amount(e.extraCost)
                +"\nFijos y pagos asignados al viaje: "+amount(e.fixedCost)
                +"\nUso para Uber: "+km(c.uberUsePercent)+"% · solo reparte fijos compartidos"
                +"\n\nImporte mínimo (base elegida): "+amount(e.minimumFare)
                +"\nCon +"+km(c.conservativeExtraMin)+" min (base elegida): "+amount(e.decisionConservativeHourly)+"/h"
                +(e.returnDecisionHourly==null?"":"\nCon regreso configurado (base elegida): "+amount(e.returnDecisionHourly)+"/h")
                +"\n\nPasajero: "+(!c.riderFilter?"filtro desactivado":e.riderBlocked?"bloqueado por tu regla":e.riderUnknown?"datos incompletos":e.riderCaution?"bajo tu meta personal":"cumple reglas evaluadas")
                +"\nZonas: recogida · "+e.zones.pickupStatus+"; destino · "+e.zones.destinationStatus
                +"\nPendientes: "+(e.pending.isEmpty()?"ninguno detectado":String.join("; ",e.pending))
                +"\n\n"+monthlySummary(c)
                +(c.carPaymentMonthly>0?"\nPago completo del carro: "+amount(c.carPaymentMonthly)+"/mes · sigue siendo obligación":"")
                +"\n"+c.profileStatus()
                +"\nEstimaciones antes de impuestos. Verde no acredita utilidad mensual ni seguridad; no se evalúa la ruta intermedia.";
    }
    static String technicalDetails(ScoreEngine.Evaluation e,DriverConfig c){
        if(e==null)return "Todavía no hay una lectura confirmada";
        if(e.color==ScoreEngine.Color.GRIS)return "Sin cálculo válido: "+e.reason();
        return e.label()+" · "+e.score+"/100\n"+String.join("\n",e.reasons)
                +"\n\n"+evaluationSummary(e)
                +"\n\nEstimación en MXN antes de impuestos. Base: "+c.basisLabel()+". Fijos mensuales asignados: "+amount(c.allocatedFixedMonthly())
                +"\nImporte tras descuento adicional: "+amount(e.revenue)+"\nEnergía: "+amount(e.energyCost)
                +"\nMantenimiento, llantas y desgaste: "+amount(e.upkeepCost)+"\nFijos prorrateados: "+amount(e.fixedCost)+"\nExtras: "+amount(e.extraCost)
                +"\nReserva de desgaste del viaje financiado: "+amount(e.financedWearReserve)
                +"\nDisponible estimado: "+amount(e.margin)+"\nPor hora: "+amount(e.hourly)+"\nPor km: "+amount(e.perKm)+"\nTiempo con esperas y reposición: "+km(e.minutes)+" min\nDistancia con reposición: "+km(e.km)+" km"
                +"\nEscenario de +"+km(c.conservativeExtraMin)+" min: "+amount(e.conservativeHourly)+"/h"
                +"\n\nScore económico: "+e.economyScore+"/100\nHora: "+Math.round(e.timePoints)+" · km: "+Math.round(e.kmPoints)+" · recogida: "+Math.round(e.pickupPoints)
                +"\nPesos: "+km(c.weightHourly)+" / "+km(c.weightKm)+" / "+km(c.weightPickup)
                +(e.rider==null?"":"\n\n"+e.rider.summary()+"\nFiltro: "+(c.riderFilter?"activo":"desactivado")+" · Nuevo < "+(int)c.newRiderMinCount+" · Verde desde "+(int)c.establishedRiderCount+" visibles\nCalificación mínima "+c.minRiderRating+" · meta "+c.goodRiderRating)
                +(e.zones==null?"":"\n\nRecogida: "+e.zones.pickupStatus+"\nDestino: "+e.zones.destinationStatus)
                +(e.zones==null?"":"\nFuente de recogida: "+e.zones.pickupReference+"\nFuente de destino: "+e.zones.destinationReference)
                +"\nNo se evalúa la ruta intermedia ni se certifica seguridad."
                +"\nEl margen de espera supone el carro detenido sin costos adicionales de energía ni km; es una estimación. El importe mínimo económico no elimina restricciones de zona, pasajero o recogida."
                +"\n\nPerfil: "+c.vehicle+" · revisión "+c.revision+"\n"+c.profileStatus()
                +(c.carPaymentMonthly>0?"\nPago del carro informado: "+amount(c.carPaymentMonthly)+" al mes":"");
    }
}
