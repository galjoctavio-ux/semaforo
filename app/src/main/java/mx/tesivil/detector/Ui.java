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
                + "\nTotal mostrado: " + o.totalMinutes() + " min · " + km(o.totalKm()) + " km";
    }
    static String amount(double value) { return String.format(java.util.Locale.US,"$%.2f",value); }
    static int signalColor(ScoreEngine.Evaluation e) {
        if(e==null)return Color.rgb(90,103,112);
        return switch(e.color){case VERDE->Color.rgb(22,112,61);case AMBAR->Color.rgb(148,88,0);case ROJO->Color.rgb(170,36,44);case GRIS->Color.rgb(90,103,112);};
    }
    static String evaluationSummary(ScoreEngine.Evaluation e) {
        if(e==null)return "Confirmando la lectura…";
        if(e.color==ScoreEngine.Color.GRIS)return e.reason();
        return "Disponible estimado: "+amount(e.hourly)+"/h\n"+amount(e.margin)+" por viaje · "+amount(e.perKm)+"/km\n"+e.reason();
    }
    static String details(ScoreEngine.Evaluation e,DriverConfig c){
        if(e==null)return "Todavía no hay una lectura confirmada";
        return e.label()+" · "+e.score+"/100\n"+String.join("\n",e.reasons)
                +"\n\nEstimación en MXN antes de impuestos. Incluye costos variables y fijos prorrateados."
                +"\nImporte tras descuento adicional: "+amount(e.revenue)+"\nEnergía: "+amount(e.energyCost)
                +"\nMantenimiento, llantas y desgaste: "+amount(e.upkeepCost)+"\nFijos prorrateados: "+amount(e.fixedCost)+"\nExtras: "+amount(e.extraCost)
                +"\nDisponible estimado: "+amount(e.margin)+"\nPor hora: "+amount(e.hourly)+"\nPor km: "+amount(e.perKm)+"\nTiempo con esperas y reposición: "+km(e.minutes)+" min\nDistancia con reposición: "+km(e.km)+" km"
                +"\nEscenario de +"+km(c.conservativeExtraMin)+" min: "+amount(e.conservativeHourly)+"/h"
                +"\n\nScore económico: "+e.economyScore+"/100\nHora: "+Math.round(e.timePoints)+" · km: "+Math.round(e.kmPoints)+" · recogida: "+Math.round(e.pickupPoints)
                +"\nPesos: "+km(c.weightHourly)+" / "+km(c.weightKm)+" / "+km(c.weightPickup)
                +(e.rider==null?"":"\n\n"+e.rider.summary()+"\nFiltro: "+(c.riderFilter?"activo":"desactivado")+" · Nuevo < "+(int)c.newRiderMinCount+" · Verde desde "+(int)c.establishedRiderCount+" visibles\nCalificación mínima "+c.minRiderRating+" · meta "+c.goodRiderRating)
                +(e.zones==null?"":"\n\nRecogida: "+e.zones.pickupStatus+"\nDestino: "+e.zones.destinationStatus)
                +"\nNo se evalúa la ruta intermedia ni se certifica seguridad."
                +"\n\nPerfil: "+c.vehicle+" · revisión "+c.revision+"\n"+(c.calibrated?"Costos marcados como revisados por ti":"Valores iniciales pendientes de revisar");
    }
}
