package de.set.assessmentUi;

import spark.Request;

public class WorkingMemoryTest extends AssessmentItem {

	@Override
	public String getTemplate() {
		return "/wmspan.html.vm";
	}

	@Override
	public void handleResultData(final AssessmentSuite a, final int currentStep, final Request request) {
		this.handleResultDataDefault(a, currentStep, request, "wm test");
	}

}
