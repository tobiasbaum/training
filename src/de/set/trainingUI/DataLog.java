package de.set.trainingUI;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DataLog {

    private static final Object LOCK = new Object();
    private static OutputStream stream;

    public static void log(final long experimentId, final String message) {
        try {
            final long time = System.currentTimeMillis();
            final String fullString = experimentId + ";" + time + ";" + message.replace('\n', ' ').replace('\r', ' ') + "\n";
            final byte[] msg = fullString.getBytes("UTF-8");
            synchronized (LOCK) {
                if (stream == null) {
                    stream = new FileOutputStream("experiment." + time + ".log");
                }
                stream.write(msg);
                stream.flush();
            }
        } catch (final IOException e) {
            throw new RuntimeException("logging failed", e);
        }
    }

    public static void close() throws IOException {
        synchronized (LOCK) {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
