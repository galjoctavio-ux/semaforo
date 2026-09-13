package mx.tesivil.detector;

import android.app.Activity;
import android.graphics.Color;
import android.text.InputType;
import android.view.WindowInsets;
import android.widget.*;

final class FormUi {
    static LinearLayout page(Activity a, String title, String help) {
        ScrollView scroll = new ScrollView(a); scroll.setFillViewport(true); scroll.setBackgroundColor(Ui.BG);
        LinearLayout page = column(a); int pad = Ui.dp(a,20); page.setPadding(pad,pad,pad,pad); scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars());
            page.setPadding(pad+b.left,pad+b.top,pad+b.right,pad+b.bottom); return insets;
        });
        text(page,title,25,true); text(page,help,14,false); a.setContentView(scroll); return page;
    }
    static LinearLayout column(android.content.Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static TextView text(LinearLayout parent, String text, float size, boolean bold) {
        TextView v = Ui.text(parent.getContext(),text,size,bold);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.topMargin=Ui.dp(parent.getContext(),12); parent.addView(v,p); return v;
    }
    static Button button(LinearLayout p, String label, Runnable action) {
        Button b = new Button(p.getContext()); b.setText(label); b.setAllCaps(false); b.setTextColor(Ui.INK); b.setMinHeight(Ui.dp(p.getContext(),52));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=Ui.dp(p.getContext(),10); p.addView(b,lp); b.setOnClickListener(v->action.run()); return b;
    }
    static EditText input(LinearLayout p, String label, String value, boolean numeric) {
        text(p,label,14,true); EditText e = new EditText(p.getContext()); e.setTextColor(Ui.INK); e.setTextSize(17);
        e.setSingleLine(true); e.setContentDescription(label);
        e.setInputType(numeric ? InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setText(value); p.addView(e,new LinearLayout.LayoutParams(-1,Ui.dp(p.getContext(),52))); return e;
    }
    static CheckBox check(LinearLayout p,String label,boolean checked) {
        CheckBox b=new CheckBox(p.getContext()); b.setText(label); b.setTextColor(Ui.INK); b.setChecked(checked); b.setMinHeight(Ui.dp(p.getContext(),48)); p.addView(b); return b;
    }
    static Spinner spinner(LinearLayout p, String label, String[] choices,int selected) {
        text(p,label,14,true); Spinner s = new Spinner(p.getContext()); s.setContentDescription(label);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(p.getContext(),android.R.layout.simple_spinner_item,choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); s.setAdapter(adapter); s.setSelection(selected);
        p.addView(s,new LinearLayout.LayoutParams(-1,Ui.dp(p.getContext(),52))); return s;
    }
    static double number(EditText e) {
        double d=Double.parseDouble(e.getText().toString().trim().replace(',','.'));
        if (!Double.isFinite(d)) throw new IllegalArgumentException("Valor inválido"); return d;
    }
    static String format(double d) { return d==Math.rint(d) ? Long.toString((long)d) : Double.toString(d); }
    static void error(Activity a, String text) { new android.app.AlertDialog.Builder(a).setTitle("Revisa los datos").setMessage(text).setPositiveButton("Entendido",null).show(); }
}
