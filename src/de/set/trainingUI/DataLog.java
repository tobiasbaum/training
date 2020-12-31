package de.set.trainingUI;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DataLog {

	private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    private static final Object LOCK = new Object();
    private static OutputStream stream;
    private static long lastOpen;

    private static AtomicLong userCounter = new AtomicLong(System.currentTimeMillis());
    private static ConcurrentHashMap<String, Long> userPseudonyms = new ConcurrentHashMap<String, Long>();

    public static void log(final String user, final String message) {
        try {
        	final long hashedUser = pseudonomize(user);
            final long time = System.currentTimeMillis();
            final String fullString = hashedUser + ";" + time + ";" + message.replace('\n', ' ').replace('\r', ' ') + "\n";
            final byte[] msg = fullString.getBytes("UTF-8");
            synchronized (LOCK) {
            	if (stream != null && time - lastOpen > ONE_DAY) {
            		stream.write((hashedUser + ";" + time + ";switching to next log\n").getBytes("UTF-8"));
            		stream.close();
            		stream = null;
            	}
                if (stream == null) {
                    stream = new FileOutputStream("experiment." + time + ".log");
                    lastOpen = time;
                }
                stream.write(msg);
                stream.flush();
            }
        } catch (final IOException e) {
            throw new RuntimeException("logging failed", e);
        }
    }

    private static long pseudonomize(String user) {
    	if (user == null) {
    		return 0;
    	}
		return userPseudonyms.computeIfAbsent(user, (String u) -> userCounter.getAndIncrement());
	}

	public static void close() throws IOException {
        synchronized (LOCK) {
            if (stream != null) {
                stream.close();
            }
        }
    }

}
