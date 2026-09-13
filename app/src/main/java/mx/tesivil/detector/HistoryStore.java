package mx.tesivil.detector;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Atomic, bounded local journal. Never receives raw OCR or endpoint addresses. */
final class HistoryStore {
    private static final ExecutorService io = Executors.newSingleThreadExecutor();
    private static JSONArray rows = new JSONArray();
    private static AtomicFile file;
    private static volatile long writeGeneration;
    static volatile String storageError = "";
    static synchronized void init(Context c) {
        if (file != null) return;
        file = new AtomicFile(new File(c.getFilesDir(), "offer-history-v2.json"));
        try { if (file.getBaseFile().exists()) rows = new JSONArray(new String(file.readFully(), StandardCharsets.UTF_8)); }
        catch (Exception e) { storageError = "No se pudo leer el historial anterior"; }
    }
    static synchronized JSONArray snapshot() {
        try { return new JSONArray(rows.toString()); } catch (Exception e) { return new JSONArray(); }
    }
    static synchronized void add(JSONObject row) {
        try{rows.put(new JSONObject(row.toString()));}catch(Exception e){storageError="No se pudo registrar la oferta";return;}
        while (rows.length() > 200) {
            int remove=0;
            for(int i=0;i<rows.length();i++)if(rows.optJSONObject(i)==null || !rows.optJSONObject(i).has("actual_result")){remove=i;break;}
            rows.remove(remove);
        }
        persist(rows.toString());
    }
    static synchronized void clear() { rows = new JSONArray(); persist("[]"); }
    /** Call from a worker: serial disk write must finish before reporting a saved result. */
    static synchronized void saveOutcome(String localId,TripOutcome result) throws Exception {
        String error=result.validate();if(error!=null)throw new IllegalArgumentException(error);
        JSONArray updated=new JSONArray(rows.toString());JSONObject target=null;
        for(int i=0;i<updated.length();i++)if(localId.equals(updated.getJSONObject(i).optString("local_id"))){target=updated.getJSONObject(i);break;}
        if(target==null)throw new IllegalArgumentException("La oferta seleccionada ya no está en el historial");
        String group=target.optString("episode_id",localId);
        for(int i=0;i<updated.length();i++){
            JSONObject row=updated.getJSONObject(i);
            if(group.equals(row.optString("episode_id",row.optString("local_id")))){row.remove("actual_result");row.put("outcome",result.state.name().toLowerCase(java.util.Locale.ROOT));}
        }
        if(result.state!=TripOutcome.State.UNKNOWN)target.put("actual_result",TripJournal.json(result));
        String json=updated.toString();
        // Supersede queued snapshots so committing a manual result never waits
        // behind hundreds of obsolete full-history writes. The active write
        // finishes first on the same executor.
        writeGeneration++;
        try{io.submit(()->{write(json);return null;}).get();}
        catch(Exception e){persist(rows.toString());throw e;}
        rows=updated;storageError="";
    }
    private static void persist(String json) {
        long generation=++writeGeneration;
        io.execute(() -> {
            if(generation!=writeGeneration)return;
            try {write(json);storageError="";}catch(Exception e){storageError="No se pudo guardar el historial";}
        });
    }
    private static void write(String json) throws Exception {
        FileOutputStream out=null;
        try{out=file.startWrite();out.write(json.getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
}
