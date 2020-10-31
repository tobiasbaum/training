package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

import spark.Request;

@SuppressWarnings("nls")
public class DefectFindTask extends Task {

    private final String content;
    private final List<RemarkPattern> expectedRemarks;

    public static Task load(final Properties p, final File taskDirectory) throws IOException {
        return new DefectFindTask(p, taskDirectory);
    }

	private DefectFindTask(final Properties p, final File taskDirectory) throws IOException {
	    super(p, taskDirectory);
		this.content = loadFileAsString(new File(taskDirectory, "source"));
		this.expectedRemarks = new ArrayList<>();
		for (final Object property : p.keySet()) {
		    final String key = property.toString();
		    if (key.startsWith("remark.") && key.endsWith(".pattern")) {
		        this.expectedRemarks.add(new RemarkPattern(
		                p.getProperty(key),
		                p.getProperty(key.replace(".pattern", ".example"))));
		    }
		}
		Collections.sort(this.expectedRemarks,
	        (final RemarkPattern p1, final RemarkPattern p2) -> Integer.compare(p1.getExample().getLine(), p2.getExample().getLine()));
	}

	@Override
	public String getTemplate() {
		return "/defectFind.html.vm";
	}

	public String getContentEscaped() {
		return escapeForJsString(this.content);
	}

	@Override
	public String toString() {
		return "defectFind " + this.getId();
	}

    @Override
    protected boolean isCorrectAnswer(final Request request) {
        final String remarks = request.queryParams("remarks");
        if (remarks.isEmpty()) {
            return this.expectedRemarks.isEmpty();
        }
        final JsonObject json = Json.parse(remarks).asObject();
        final List<Remark> actualRemarks = new ArrayList<>();
        for (final Member e : json) {
            actualRemarks.add(new Remark(
                    Integer.parseInt(e.getName()),
                    RemarkType.valueOf(e.getValue().asObject().get("t").asString()),
                    e.getValue().asObject().get("m").asString()));
        }

        final ReviewResultRating rrr = new ReviewResultRating(this.expectedRemarks, actualRemarks);
        return rrr.isCorrect();
    }

    @Override
    protected double estimateDifficulty() {
        return this.content.length() + 1;
    }

    @Override
    public void addContextData(final Map<String, Object> data) {
        data.put("remarkTypes", Arrays.asList(RemarkType.values()));
    }

    @Override
    public List<List<String>> getSolution() {
        final List<List<String>> ret = new ArrayList<>();
        for (final RemarkPattern r : this.expectedRemarks) {
            ret.add(Arrays.asList(
                    "Zeile " + r.getExample().getLine(),
                    r.getExample().getType().getText(),
                    r.getExample().getMessage()));
        }
        return ret;
    }

}
