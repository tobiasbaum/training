package de.set.trainingUI;

import java.io.Serializable;

public class Statistics implements Cloneable, Serializable {

	private static final long serialVersionUID = 6934873524833747803L;

	private final boolean forFamily;
    private int trySum;
    private int firstTryCount;
    private int firstTrySuccessCount;
    private int bestTryCount = Integer.MAX_VALUE;
    private long bestTimeForFirstTry = Long.MAX_VALUE;
    private long timeSum;

    public Statistics(final boolean forFamily) {
        this.forFamily = forFamily;
    }

    void count(final Trial t) {
        if (t.getTryCount() == 1) {
            this.firstTryCount++;
        }
        if (!t.isIncorrect()) {
            this.trySum += t.getTryCount();
            if (t.getTryCount() == 1) {
                this.firstTrySuccessCount++;
            }
            if (t.getTryCount() < this.bestTryCount) {
                this.bestTryCount = t.getTryCount();
            }
            final long time = t.getNeededTime();
            if (time < this.bestTimeForFirstTry) {
                this.bestTimeForFirstTry = time;
            }
            this.timeSum += time;
        }
    }

    public boolean isForFamily() {
        return this.forFamily;
    }

    public int getSuccessCountOnFirstTry() {
        return this.firstTrySuccessCount;
    }

    public int getFirstTryCount() {
        return this.firstTryCount;
    }

    public double getMeanTryCount() {
        return ((double) this.trySum) / this.firstTryCount;
    }

    public boolean areDefined() {
    	return this.firstTryCount > 0;
    }

    public int getBestTryCount() {
        return this.bestTryCount;
    }

    public double getMeanTimeForFirstTry() {
        if (this.firstTrySuccessCount == 0) {
            return 0.0;
        }
        return ((double) this.timeSum) / this.firstTrySuccessCount;
    }

    public double getBestTimeForFirstTry() {
        return this.bestTimeForFirstTry;
    }

    public Statistics copy() {
        try {
            return (Statistics) this.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

}
