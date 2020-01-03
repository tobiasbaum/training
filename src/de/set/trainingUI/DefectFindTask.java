package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonObject.Member;

import spark.Request;

@SuppressWarnings("nls")
public class DefectFindTask extends Task {

    public static enum RemarkType {
        SYNTAX_ERROR("Syntaxfehler"),
        WRONG_COMPARISON("Fehlerhafter Vergleich"),
        OTHER_ALGORITHMIC_PROBLEM("Anderes algorithmisches Problem"),
        DUPLICATE_CODE("Doppelter Code");

        private String text;

        private RemarkType(final String text) {
            this.text = text;
        }

        public String getValue() {
            return this.name();
        }

        public String getText() {
            return this.text;
        }
    }

    public static final class Remark {
        private final int line;
        private final RemarkType type;
        private final String message;

        public Remark(final int line, final RemarkType type, final String message) {
            this.line = line;
            this.type = type;
            this.message = message;
        }
    }

    public static final class RemarkPattern {
        private final Set<Integer> allowedLines;
        private final Set<RemarkType> allowedTypes;
        private final Pattern allowedMessages;
        private final Remark example;

        public RemarkPattern(final String patternProperty, final String exampleProperty) {
            final String[] patternParts = patternProperty.split(";", 3);
            this.allowedLines = this.parseNumberSet(patternParts[0]);
            this.allowedTypes = this.parseTypeSet(patternParts[1]);
            this.allowedMessages = Pattern.compile(patternParts[2]);
            final String[] exampleParts = exampleProperty.split(";", 3);
            this.example = new Remark(
                    Integer.parseInt(exampleParts[0]),
                    RemarkType.valueOf(exampleParts[1]),
                    exampleParts[2]);

            if (!this.matches(this.example)) {
                throw new RuntimeException("example is invalid");
            }
        }

        private Set<Integer> parseNumberSet(final String string) {
            final Set<Integer> ret = new LinkedHashSet<>();
            final String[] parts = string.split(",");
            for (final String part : parts) {
                ret.add(Integer.parseInt(part));
            }
            return ret;
        }

        private Set<RemarkType> parseTypeSet(final String string) {
            final Set<RemarkType> ret = EnumSet.noneOf(RemarkType.class);
            final String[] parts = string.split(",");
            for (final String part : parts) {
                ret.add(RemarkType.valueOf(part));
            }
            return ret;
        }

        private boolean matches(final Remark remark) {
            return this.allowedLines.contains(remark.line)
                && this.allowedTypes.contains(remark.type)
                && this.allowedMessages.matcher(remark.message).find();
        }

    }

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

        for (final RemarkPattern expected : this.expectedRemarks) {
            if (!this.matchesAny(expected, actualRemarks)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAny(final RemarkPattern expected, final List<Remark> remarks) {
        for (final Remark remark : remarks) {
            if (expected.matches(remark)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected double estimateDifficulty() {
        return this.content.length() + 1;
    }

    @Override
    public void addContextData(final Map<String, Object> data) {
        data.put("remarkTypes", Arrays.asList(RemarkType.values()));
    }

}
