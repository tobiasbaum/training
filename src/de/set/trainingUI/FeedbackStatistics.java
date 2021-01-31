package de.set.trainingUI;

public class FeedbackStatistics {

    private final Statistics stats;
    private final Trial trial;
	private final Trainee trainee;

    public FeedbackStatistics(final Statistics s, final Trial trial, Trainee trainee) {
        this.stats = s;
        this.trial = trial;
        this.trainee = trainee;
    }

    public boolean isForFamily() {
        return this.stats.isForFamily();
    }

    public int getSuccessOnFirstTry() {
        return this.toPercent(this.stats.getSuccessCountOnFirstTry(), this.stats.getFirstTryCount());
    }

    private int toPercent(final int value, final int total) {
        if (total == 0) {
            return 0;
        }
        return value * 100 / total;
    }

    public double getMeanTryCount() {
        return this.stats.getMeanTryCount();
    }

    public double getBestTryCount() {
        return this.stats.getBestTryCount();
    }

    public boolean isWithTime() {
        return this.stats.getSuccessCountOnFirstTry() > 0;
    }

    public double getMeanTime() {
        return this.stats.getMeanTimeForFirstTry();
    }

    public double getBestTime() {
        return this.stats.getBestTimeForFirstTry();
    }

    public boolean isRecord() {
        if (this.trial.isIncorrect()) {
            return false;
        }
        return this.isRecordFirst()
            || this.isRecordTimeBest()
            || this.isRecordTimeMean()
            || this.isRecordTimeBestPersonal()
            || this.isRecordTryBest()
            || this.isRecordTryMean();
    }

    public boolean isRecordFirst() {
        return this.stats.getSuccessCountOnFirstTry() == 0;
    }

    public boolean isRecordTimeMean() {
        return this.trial.getNeededTime() < this.stats.getMeanTimeForFirstTry()
            && !this.isRecordTimeBest()
            && !this.isRecordFirst();
    }

    public boolean isRecordTimeBest() {
        return this.trial.getNeededTime() < this.stats.getBestTimeForFirstTry()
            && !this.isRecordFirst();
    }

    public boolean isRecordTimeBestPersonal() {
    	return this.trial.getNeededTime() < this.trainee.getBestTimeForTask(this.trial)
			&& !this.isRecordFirst()
			&& !this.isRecordTimeBest();
    }

    public boolean isRecordTryMean() {
        return this.trial.getTryCount() < this.stats.getMeanTryCount()
            && !this.isRecordTryBest()
            && !this.isRecordFirst();
    }

    public boolean isRecordTryBest() {
        return this.trial.getTryCount() < this.stats.getBestTryCount()
            && !this.isRecordFirst();
    }

}
