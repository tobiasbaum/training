package de.set.trainingUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

import spark.Request;

public abstract class Task {

    private final String family;
    private final String id;

    public Task(final Properties p, final File taskDirectory) {
        this.family = p.getProperty("type") + "." + p.getProperty("family");
        this.id = taskDirectory.getName();
    }

    public boolean isSameFamily(final Task task) {
        return this.family.equals(task.family);
    }

    public final String getId() {
        return this.id;
    }

    public final String getFamilyId() {
        return this.family;
    }

	public abstract String getTemplate();

    public abstract void addContextData(Map<String, Object> data);

	protected static String loadFileAsString(final File path) throws IOException {
		try (InputStream s = new FileInputStream(path)) {
			final BufferedReader r = new BufferedReader(new InputStreamReader(s, "UTF-8"));
			String line;
			final StringBuilder ret = new StringBuilder();
			while ((line = r.readLine()) != null) {
				ret.append(line).append('\n');
			}
			return ret.toString();
		}
	}

	protected static String escapeForJsString(final String s) {
		//neben dem Escapen wird an den Anfang jeder Zeile ein Zero-Width-Zeichen gesetzt
		//  dadurch wird Copy-and-Paste des Codes deutlich erschwert
		return s.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\n", "\\n\u180E")
				.replace("\r", "\\r")
				.replace("\"", "\\\"");
	}

    protected abstract AnnotatedSolution checkSolution(Request request);

    protected abstract double estimateDifficulty();

}
