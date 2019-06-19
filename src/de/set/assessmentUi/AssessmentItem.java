package de.set.assessmentUi;

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

}
