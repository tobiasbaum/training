package de.set.assessmentUi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import spark.Request;

public class UnderstandingTask extends AssessmentItem {

	private final String codepath;
	private final String content;

	public UnderstandingTask(final String codepath) throws IOException {
		this.codepath = codepath;
		this.content = this.loadResourceAsString(codepath);
	}

	private String loadResourceAsString(final String path) throws IOException {
		try (InputStream s = UnderstandingTask.class.getResourceAsStream(this.codepath)) {
			final BufferedReader r = new BufferedReader(new InputStreamReader(s, "UTF-8"));
			String line;
			final StringBuilder ret = new StringBuilder();
			while ((line = r.readLine()) != null) {
				ret.append(line).append('\n');
			}
			return ret.toString();
		}
	}

	@Override
	public String getTemplate() {
		return "/understanding.html.vm";
	}

	@Override
	public void handleResultData(final AssessmentSuite a, final int currentStep, final Request request) {
		this.handleResultDataDefault(a, currentStep, request, this.toString());
	}

	public String getContentEscaped() {
		return this.content.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
	}

	@Override
	public String toString() {
		return "understanding " + this.codepath;
	}

}
