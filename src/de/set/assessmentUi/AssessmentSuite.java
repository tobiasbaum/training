package de.set.assessmentUi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import spark.Request;

public class AssessmentSuite {

	private final long id;
	private final String greeting;
	private final List<AssessmentItem> steps = new ArrayList<>();

	private int currentStep;
	private Date lastAccess;

	public AssessmentSuite(final long id, final String greeting) {
		this.id = id;
		this.greeting = greeting;
		DataLog.log(id, "creating assessment for " + greeting);
		this.currentStep = -1;
		this.lastAccess = new Date();
	}

	public void addStep(final AssessmentItem step) {
		this.steps.add(step);
	}

	public long getId() {
		return this.id;
	}

	public String getGreeting() {
		return this.greeting;
	}

	public AssessmentItem getStep(final int step) {
		if (step >= this.steps.size()) {
			return null;
		}
		return this.steps.get(step);
	}

	public boolean isNextStep(final int step) {
		return step == this.currentStep + 1;
	}

	public void setCurrentStep(final int step) {
		this.currentStep = step;
		DataLog.log(this.id, "moved to step " + step + ": " + this.getStep(step));
	}

	public int getCurrentStepNumber() {
		return this.currentStep;
	}

	public Date getLastAccess() {
		return this.lastAccess;
	}

	private AssessmentItem getCurrentStep() {
		if (this.currentStep >= 0 && this.currentStep < this.steps.size()) {
			return this.steps.get(this.currentStep);
		} else {
			return null;
		}
	}

	public String getStepNames() {
		return this.steps.toString();
	}

	public void handleResultForCurrentStep(final Request request) {
		this.lastAccess = new Date();
    	final AssessmentItem previousItem = this.getCurrentStep();
    	if (previousItem != null) {
    		previousItem.handleResultData(this, this.currentStep, request);
    	}
	}

}
