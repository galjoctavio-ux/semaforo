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
        rows.put(row);
        while (rows.length() > 200) rows.remove(0);
        persist(rows.toString());
    }
    static synchronized void clear() { rows = new JSONArray(); persist("[]"); }
    private static void persist(String json) {
        io.execute(() -> {
            FileOutputStream out = null;
            try { out = file.startWrite(); out.write(json.getBytes(StandardCharsets.UTF_8)); file.finishWrite(out); storageError = ""; }
            catch (Exception e) { if (out != null) file.failWrite(out); storageError = "No se pudo guardar el historial"; }
        });
    }
}
