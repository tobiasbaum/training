package de.set.trainingUI;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class RemarkPattern {
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

        if (!this.matches(this.getExample())) {
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

    boolean matches(final Remark remark) {
        return this.allowedLines.contains(remark.getLine())
            && this.allowedTypes.contains(remark.getType())
            && this.allowedMessages.matcher(remark.getMessage()).find();
    }

	Remark getExample() {
		return this.example;
	}

	public int distanceToExample(Remark remark) {
		return Math.abs(this.example.getLine() - remark.getLine());
	}

}