package mx.tesivil.detector;

/** Prevents stopped, resized, hidden or stale captures from publishing a reading. */
public final class ReadingWindow {
    public static final long MAX_FRAME_AGE_MS = 1400;
    private long generation;
    private boolean active;
    public synchronized long start() { active = true; return ++generation; }
    public synchronized void invalidate() { active = false; generation++; }
    public synchronized long generation() { return generation; }
    public synchronized boolean accepts(long token, long capturedAt, long now) {
        return active && token == generation && now >= capturedAt && now - capturedAt <= MAX_FRAME_AGE_MS;
    }
}
