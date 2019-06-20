package de.set.assessmentUi;

import java.io.IOException;

import spark.Request;

public class DefectFindTask extends AssessmentItem {

	private final String codepath;
	private final String content;

	public DefectFindTask(final String codepath) throws IOException {
		this.codepath = codepath;
		this.content = loadResourceAsString(codepath);
	}

	@Override
	public String getTemplate() {
		return "/defectFind.html.vm";
	}

	@Override
	public void handleResultData(final AssessmentSuite a, final int currentStep, final Request request) {
		this.handleResultDataDefault(a, currentStep, request, this.toString());
	}

	public String getContentEscaped() {
		return escapeForJsString(this.content);
	}

	@Override
	public String toString() {
		return "defectFind " + this.codepath;
	}

}
