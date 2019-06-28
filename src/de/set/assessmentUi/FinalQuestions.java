package de.set.assessmentUi;

import spark.Request;

public class FinalQuestions extends AssessmentItem {

	@Override
	public String getTemplate() {
		return "/finalquestions.html.vm";
	}

	@Override
	public void handleResultData(final AssessmentSuite a, final int currentStep, final Request request) {
		this.handleResultDataDefault(a, currentStep, request, this.toString());
	}

	@Override
	public String toString() {
		return "final questions";
	}

}
