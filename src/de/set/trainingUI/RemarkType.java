package de.set.trainingUI;

public enum RemarkType {
    WRONG_COMPARISON("Fehlerhafte(r) Vergleich/Bedingung", "Eine Bedingung (in if, while, ...) oder ein Vergleich (<, >, ==, ...) ist falsch, sodass sich das Programm inkorrekt verhält."),
    WRONG_CALCULATION("Fehlerhafte Berechnung", "Eine Berechnung (+, -, +=, ...) ist falsch, sodass sich das Programm inkorrekt verhält."),
    WRONG_MESSAGE("Fehlerhafte Meldung", "Eine Meldung für den Benutzer (Exception, Ausgabe, ...) ist falsch oder unvollständig."),
    MISSING_CODE("Fehlender Code", "Es fehlt Code (fehlendes Statement / Expression / Keyword / Fallunterscheidung / ...), sodass sich das Programm inkorrekt verhält."),
    OTHER_ALGORITHMIC_PROBLEM("Anderes Korrektheitsproblem", "Der Code in der Zeile sorgt dafür, dass sich das Programm inkorrekt verhält und es trifft keine der anderen Problemkategorien zu."),
    DUPLICATE_CODE("Doppelter Code", "Es wurde in größerem Umfang Code dupliziert, sodass ein Wartbarkeitsproblem vorliegt.");

    private String text;
    private String description;

    private RemarkType(final String text, String description) {
        this.text = text;
        this.description = description;
    }

    public String getValue() {
        return this.name();
    }

    public String getText() {
        return this.text;
    }

    public String getDescription() {
    	return this.description;
    }
}