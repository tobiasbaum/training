package de.set.trainingUI;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import spark.Request;

public class Trial {

    private final Task task;
    private final Instant startTime;
    private final int retryCount;
    private Instant endTime;
    private boolean incorrect;

    public Trial(final Task task, final int retryCount) {
        this.task = task;
        this.startTime = Instant.now();
        this.retryCount = retryCount;
    }

    public Task getTask() {
        return this.task;
    }

    public int getRetryCount() {
        return this.retryCount;
    }

    public int getTryCount() {
        return this.retryCount + 1;
    }

    public boolean isIncorrect() {
        return this.incorrect;
    }

    public String getNeededTimeFormatted() {
        if (this.endTime == null) {
            return "not solved yet";
        }
        return Util.formatTime(this.getNeededTime());
    }

    public long getNeededTime() {
        return this.startTime.until(this.endTime, ChronoUnit.SECONDS);
    }

    public void checkAnswer(final Request request) {
        assert this.endTime == null;

        this.endTime = Instant.now();
        this.incorrect = !this.task.isCorrectAnswer(request);
    }

    public Instant getStartTime() {
        return this.startTime;
    }

}
