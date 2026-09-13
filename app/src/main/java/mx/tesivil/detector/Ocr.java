package mx.tesivil.detector;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.IdentityHashMap;

final class Ocr {
    static TextRecognizer create() { return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS); }
    private static Bitmap contrast(Bitmap source){
        Bitmap result=Bitmap.createBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888);
        Paint paint=new Paint();paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                1.5f,0,0,0,-64, 1.5f,0,0,0,-64, 1.5f,0,0,0,-64, 0,0,0,1,0})));
        new Canvas(result).drawBitmap(source,0,0,paint);return result;
    }
    private static List<Text.Line> orderedLines(Text result) {
        List<Text.Line> lines = new ArrayList<>();
        for (Text.TextBlock block : result.getTextBlocks()) lines.addAll(block.getLines());
        lines.sort(Comparator.comparingInt((Text.Line l) -> {
            Rect r = l.getBoundingBox(); return r == null ? 0 : r.top;
        }).thenComparingInt(l -> { Rect r = l.getBoundingBox(); return r == null ? 0 : r.left; }));
        return lines;
    }
    static String orderedText(Text result) {
        StringBuilder text = new StringBuilder();
        for (Text.Line line : orderedLines(result)) text.append(line.getText()).append('\n');
        return text.toString();
    }
    private static Rect cardBounds(List<Text.Line> lines,Bitmap bitmap){
        Text.Line category=null,action=null;int left=bitmap.getWidth();
        for(Text.Line line:lines){
            if(OfferParser.isCategoryLine(line.getText())){if(category!=null)return null;category=line;}
            if(category==null)continue;
            Rect r=line.getBoundingBox();
            if(r!=null&&(line==category||OfferParser.numericFieldKind(line.getText())>0))left=Math.min(left,r.left);
            if(OfferParser.isCardActionLine(line.getText())){action=line;break;}
        }
        if(category==null||action==null||category.getBoundingBox()==null||action.getBoundingBox()==null)return null;
        Rect header=category.getBoundingBox(),button=action.getBoundingBox();
        int pad=Math.max(12,header.height());
        left=Math.max(0,left-pad);
        if(left>bitmap.getWidth()*.15||button.top<=header.bottom)return null;
        return new Rect(left,Math.max(0,header.top-pad),bitmap.getWidth()-left,Math.min(bitmap.getHeight(),button.bottom+pad));
    }
    private static List<Text.Line> cardLines(List<Text.Line> lines,Rect card){
        if(card==null)return lines;
        List<Text.Line> kept=new ArrayList<>();
        for(Text.Line line:lines){Rect r=line.getBoundingBox();if(r!=null&&card.contains(r.centerX(),r.centerY()))kept.add(line);}
        // A second offer must remain ambiguous even if its badge is outside the first card.
        for(Text.Line line:lines)if(!kept.contains(line)&&OfferParser.isCategoryLine(line.getText()))return lines;
        return kept;
    }
    private static String joined(List<Text.Line> lines){StringBuilder s=new StringBuilder();for(Text.Line l:lines)s.append(l.getText()).append('\n');return s.toString();}
    private static final class Field {
        final Text.Line line;final Rect source;final int kind,width,height;
        Field(Text.Line line,Rect source,int kind){this.line=line;this.source=source;this.kind=kind;double scale=Math.min(3d,1600d/source.width());width=Math.max(1,(int)(source.width()*scale));height=Math.max(1,(int)(source.height()*scale));}
    }
    private static Task<String> readField(TextRecognizer recognizer,Bitmap bitmap,Field field){
        Bitmap crop=Bitmap.createBitmap(bitmap,field.source.left,field.source.top,field.source.width(),field.source.height());
        Bitmap zoom=Bitmap.createScaledBitmap(crop,field.width,field.height,true);
        if(crop!=zoom&&crop!=bitmap)crop.recycle();
        try{
            Task<Text> task=recognizer.process(image(zoom));
            task.addOnCompleteListener(done->{if(zoom!=bitmap)zoom.recycle();});
            return task.continueWith(done->{
                if(!done.isSuccessful())return null;
                String value=null;
                for(Text.Line line:orderedLines(done.getResult()))if(OfferParser.numericFieldKind(line.getText())==field.kind){
                    if(value!=null)return null;value=line.getText();
                }
                return value;
            });
        }catch(RuntimeException e){if(zoom!=bitmap)zoom.recycle();throw e;}
    }
    /** One refinement stage: isolated crops retain natural context instead of a synthetic montage. */
    static Task<OfferParser.Result> read(TextRecognizer recognizer,Bitmap bitmap) {
        return read(recognizer,bitmap,new Rect[]{null});
    }
    private static Task<OfferParser.Result> read(TextRecognizer recognizer,Bitmap bitmap,Rect[] region) {
        // Keep the proven original-color path; retry contrast only after it fails.
        Task<OfferParser.Result> primary=readVariant(recognizer,bitmap,region);
        return primary.continueWithTask(done->{
                if(!done.isSuccessful())return Tasks.forException(done.getException()==null?new IllegalStateException("OCR unavailable"):done.getException());
                OfferParser.Result initial=done.getResult();
                if(initial.offer!=null||!(initial.needsCardRefinement||initial.needsMoneyRefinement||initial.needsNumericRefinement))return Tasks.forResult(initial);
                // One bounded full retry from the same pixels; never reuse another frame.
                Bitmap retry=contrast(bitmap);
                try{
                    Task<OfferParser.Result> second=readVariant(recognizer,retry,region);
                    second.addOnCompleteListener(completed->retry.recycle());
                    return second.continueWith(completed->completed.isSuccessful()&&completed.getResult().offer!=null?completed.getResult():initial);
                }catch(RuntimeException e){retry.recycle();return Tasks.forResult(initial);}
        });
    }
    private static Task<OfferParser.Result> readVariant(TextRecognizer recognizer,Bitmap bitmap,Rect[] region) {
        return recognizer.process(image(bitmap)).continueWithTask(first->{
            if(!first.isSuccessful())return Tasks.forException(first.getException()==null?new IllegalStateException("OCR unavailable"):first.getException());
            List<Text.Line> all=orderedLines(first.getResult());region[0]=cardBounds(all,bitmap);
            List<Text.Line> lines=cardLines(all,region[0]);OfferParser.Result initial=OfferParser.parse(joined(lines));
            if(!initial.needsMoneyRefinement&&!initial.needsNumericRefinement)return Tasks.forResult(initial);
            List<Field> fields=new ArrayList<>();boolean inCard=false;boolean[] seen=new boolean[5];
            for(Text.Line line:lines){
                if(OfferParser.isCategoryLine(line.getText()))inCard=true;
                if(!inCard)continue;if(OfferParser.isCardActionLine(line.getText()))break;
                int kind=OfferParser.numericFieldKind(line.getText());if(kind==0)continue;
                // Retry only the fare for missing cents, or rate and legs for disagreement.
                if(initial.needsMoneyRefinement&&!initial.needsNumericRefinement&&kind!=1)continue;
                // Preserve a fare whose cents were already legible.
                if(initial.needsNumericRefinement&&!initial.needsMoneyRefinement&&kind==1)continue;
                if(seen[kind]||line.getBoundingBox()==null)return Tasks.forResult(initial);seen[kind]=true;
                Rect bounds=new Rect(line.getBoundingBox());
                int pad=initial.needsNumericRefinement?Math.max(12,bounds.height()/2):Math.max(6,bounds.height()/3);
                bounds.inset(-pad,-pad);
                if(!bounds.intersect(0,0,bitmap.getWidth(),bitmap.getHeight()))return Tasks.forResult(initial);
                fields.add(new Field(line,bounds,kind));
            }
            if(fields.isEmpty()||initial.needsMoneyRefinement&&!seen[1])return Tasks.forResult(initial);
            List<Task<String>> retries=new ArrayList<>();
            try{for(Field field:fields)retries.add(readField(recognizer,bitmap,field));}
            catch(RuntimeException e){return Tasks.whenAllComplete(retries).continueWith(done->initial);}
            return Tasks.whenAllComplete(retries).continueWith(done->{
                IdentityHashMap<Text.Line,String> replacements=new IdentityHashMap<>();
                for(int i=0;i<fields.size();i++){
                    Task<String> retry=retries.get(i);if(!retry.isSuccessful()||retry.getResult()==null)return initial;
                    replacements.put(fields.get(i).line,retry.getResult());
                }
                StringBuilder refined=new StringBuilder();for(Text.Line line:lines)refined.append(replacements.getOrDefault(line,line.getText())).append('\n');
                OfferParser.Result parsed=OfferParser.parse(refined.toString());
                return parsed.offer!=null?parsed:initial;
            });
        });
    }
    static InputImage image(Bitmap bitmap) { return InputImage.fromBitmap(bitmap, 0); }
    private Ocr() { }
}
