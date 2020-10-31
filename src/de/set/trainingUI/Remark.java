package de.set.trainingUI;

public final class Remark {
    private final int line;
    private final RemarkType type;
    private final String message;

    public Remark(final int line, final RemarkType type, final String message) {
        this.line = line;
        this.type = type;
        this.message = message;
    }

	int getLine() {
		return line;
	}

	RemarkType getType() {
		return type;
	}

	String getMessage() {
		return message;
	}
}