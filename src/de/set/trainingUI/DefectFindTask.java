package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import spark.Request;

public class DefectFindTask extends Task {

	private final String codepath;
	private final String content;

	public DefectFindTask(final Properties p, final File codepath) throws IOException {
	    //TODO fix
	    super(p, codepath);
		this.codepath = codepath.getPath();
		this.content = loadFileAsString(codepath);
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

    @Override
    protected boolean isCorrectAnswer(final Request request) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected double estimateDifficulty() {
        return this.content.length() + 1;
    }

}
