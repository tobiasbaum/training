package de.set.assessmentUi;

public enum Diff {

    DIFF1("c1"),
    DIFF2("c2");

    private final String path;

    private Diff(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

}
