package mx.tesivil.detector;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReadingWindowTest {
    @Test public void stoppedSessionCannotPublish() {
        ReadingWindow window = new ReadingWindow(); long token = window.start();
        window.invalidate(); assertFalse(window.accepts(token, 100, 200));
    }
    @Test public void priorCaptureCannotPublishAfterRestartOrResize() {
        ReadingWindow window = new ReadingWindow(); long token = window.start();
        window.start(); assertFalse(window.accepts(token, 100, 200));
    }
    @Test public void freshReadingAcceptedButStaleReadingRejected() {
        ReadingWindow window = new ReadingWindow(); long token = window.start();
        assertTrue(window.accepts(token, 100, 200));
        assertFalse(window.accepts(token, 100, 1501));
        assertFalse(window.accepts(token, 100, 99));
    }
}
