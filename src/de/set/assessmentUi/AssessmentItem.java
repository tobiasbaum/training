package de.set.assessmentUi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import spark.Request;

public abstract class AssessmentItem {

	public abstract String getTemplate();

	public abstract void handleResultData(final AssessmentSuite a, int currentStep, Request request);

	protected final void handleResultDataDefault(
			final AssessmentSuite a, final int currentStep, final Request request, final String stepType) {
		for (final String key : request.queryParams()) {
	        for (final String line : request.queryParamOrDefault(key, "").split("\n")) {
	            DataLog.log(a.getId(), key + " from " + stepType + ";step " + currentStep + ";" + line);
	        }
		}
	}

	protected static String loadResourceAsString(final String path) throws IOException {
		try (InputStream s = UnderstandingTask.class.getResourceAsStream(path)) {
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
		return s.replace("\\", "\\\\")
				.replace("'", "\\'")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\"", "\\\"");
	}

}
