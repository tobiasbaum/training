package de.set.trainingUI;

import java.util.Map;

import spark.Request;

public class MissingTask extends Task {

	public MissingTask(String id) {
		super("unknownTask", id);
	}

	@Override
	public String getTemplate() {
		return "missing task";
	}

	@Override
	public void addContextData(Map<String, Object> data) {
	}

	@Override
	protected AnnotatedSolution checkSolution(Request request) {
		throw new AssertionError("task is missing but shall be checked");
	}

	@Override
	protected double estimateDifficulty() {
		return 0.0;
	}

}
