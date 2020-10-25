package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import spark.Request;

@SuppressWarnings("nls")
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

    @Override
    public List<List<String>> getSolution() {
        return Collections.singletonList(Collections.singletonList(this.expectedAnswer));
    }

}
