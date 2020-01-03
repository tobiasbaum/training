package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import spark.Request;

public class UnderstandingTask extends Task {

	private final String content;
	private final String expectedAnswer;

    public static Task load(final Properties p, final File taskDirectory) throws IOException {
        return new UnderstandingTask(p, taskDirectory);
    }

	private UnderstandingTask(final Properties p, final File taskDirectory) throws IOException {
	    super(p, taskDirectory);
		this.expectedAnswer = p.getProperty("expectedAnswer");
		this.content = loadFileAsString(new File(taskDirectory, "source"));
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
		return escapeForJsString(this.content);
	}

	@Override
	public String toString() {
		return "understanding " + this.getId();
	}

    @Override
    protected boolean isCorrectAnswer(final Request request) {
        final String answer = request.queryParams("answer");
        return this.expectedAnswer.equals(answer);
    }

    @Override
    protected double estimateDifficulty() {
        return this.content.length();
    }

    @Override
    public void addContextData(final Map<String, Object> data) {
    }

}
