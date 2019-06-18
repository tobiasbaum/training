package de.unihannover.orderingExperimentUi;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.unihannover.orderingExperimentUi.Treatments.TreatmentCombination;

public class Experiment {

    private static Random PRIVATE_ID_RND;

    private static final Set<String> VALID_VARIABLE_NAMES = new HashSet<>(Arrays.asList(
                    "clientSideIpHash",
                    "role",
                    "profDevExp",
                    "javaExp",
                    "reviewPract",
                    "programPract",
                    "workStartHours",
                    "fitness",
                    "methodAdded",
                    "idStatement",
                    "changeDescription",
                    "loadInfo",
                    "deletionTask",
                    "renaming",
                    "charChange",
                    "nextMatchImpl",
                    "matcherCreation",
                    "comparisonUnderstanding",
                    "explanationComparisonUnderstanding",
                    "comparisonComplicated",
                    "explanationComparisonComplicated",
                    "jedit",
                    "furtherRemarks"));

    private final long id;
    private final int privateKey;
    private final Map<String, String> variables = new LinkedHashMap<>();

    private String state;
    private Date lastStateChange;
    private TreatmentCombination treatmentCombination;
    private int testReviewScore;


    public Experiment(long id) {
        this.id = id;
        this.privateKey = getNextPrivateId(id);
        this.setState("fresh");
    }

    public long getId() {
        return this.id;
    }

    public String getIdAlpha() {
        final long hash = ((this.id & 0xFF) << 8) + (((this.id >> 8) + (this.id >> 16) + (this.id >> 24)) & 0xFF);
        return Long.toString(hash, 36);
    }

    public String getPrivateKey() {
        return Integer.toString(this.privateKey, 36);
    }

    private static synchronized int getNextPrivateId(long seed) {
        if (PRIVATE_ID_RND == null) {
            PRIVATE_ID_RND = new Random(seed);
        }
        return PRIVATE_ID_RND.nextInt(500) + 100;
    }

    public void start() {
        DataLog.log(this.id, "experiment started");
        this.setState("started");
    }

    public void setState(String string) {
        DataLog.log(this.id, "change state;" + string);
        this.state = string;
        this.lastStateChange = new Date();
    }

    private void assignTreatments(Treatments ts) {
        this.treatmentCombination = ts.getNextBestTreatment(this.getTestReviewScore());
        DataLog.log(this.id, "assigned treatment combination " + this.treatmentCombination);
    }

    private int getTestReviewScore() {
        return this.testReviewScore;
    }

    public void setTestReviewScore(int score) {
        DataLog.log(this.id, "setTestReviewScore;" + score);
        this.testReviewScore = score;
    }

    public boolean isVariableName(String key) {
        return VALID_VARIABLE_NAMES.contains(key);
    }

    public void setVariable(String key, String[] value) {
        final String valStr = value.length == 1 ? value[0] : Arrays.toString(value);
        DataLog.log(this.id, "set variable;" + key + ";" + valStr);
    }

    public synchronized TreatmentCombination getAssignedTreatment(Treatments treatments) {
        if (this.treatmentCombination == null) {
           this.assignTreatments(treatments);
        }
        return this.treatmentCombination;
    }

    public synchronized String getAssignedDiffDescription(int number) {
        final Diff d = number == 1 ? this.treatmentCombination.getD1() : this.treatmentCombination.getD2();
        switch (d) {
        case DIFF1:
            return "file system tasks";
        case DIFF2:
            return "search algorithm";
        }
        throw new AssertionError();
    }

    public String getState() {
        return this.state;
    }

    public Date getLastStateChange() {
        return this.lastStateChange;
    }

    public synchronized void cancel(Treatments treatments) {
        if (this.treatmentCombination != null) {
            treatments.remove(this.treatmentCombination, this.getTestReviewScore());
        }
        this.setState("cancelled");
    }

    public boolean isCancelled() {
        return this.state.equals("cancelled");
    }

}
