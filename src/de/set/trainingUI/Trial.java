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

    private Trial(String serialized, TaskDB tasks) {
    	final String[] parts = serialized.split(";");
    	if (parts.length != 5) {
    		throw new RuntimeException("parse error at " + serialized);
    	}
    	final Task loadedTask = tasks.getTaskById(parts[0]);
    	this.task = loadedTask != null ? loadedTask : new MissingTask(parts[0]);
    	this.startTime = this.parseInstant(parts[1]);
    	this.retryCount = Integer.parseInt(parts[2]);
    	this.endTime = this.parseInstant(parts[3]);
    	this.incorrect = Boolean.parseBoolean(parts[4]);
    }

    private Instant parseInstant(String string) {
    	if (string.equals("null")) {
    		return null;
    	} else {
    		return Instant.ofEpochMilli(Long.parseLong(string));
    	}
	}

	public String serialize() {
        return this.task.getId()
            + ";" + this.startTime.toEpochMilli()
            + ";" + this.retryCount
            + ";" + (this.endTime == null ? "null" : this.endTime.toEpochMilli())
            + ";" + this.incorrect;
    }

    public static Trial deserialize(String serialized, TaskDB tasks) {
    	return new Trial(serialized, tasks);
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
    	if (this.endTime == null) {
    		return 0;
    	}
        return this.startTime.until(this.endTime, ChronoUnit.SECONDS);
    }

    public AnnotatedSolution checkAnswer(final Request request) {
        assert this.endTime == null;

        this.endTime = Instant.now();
        final AnnotatedSolution result = this.task.checkSolution(request);
        this.incorrect = !result.isCorrect();
        return result;
    }

    public Instant getStartTime() {
        return this.startTime;
    }

    /**
     * The start time (with millisecond granularity) is also used as a key for trials of a user.
     * This method can be used to check if two trials have the same.
     */
	public boolean hasSameStartAs(Trial t) {
		return this.getStartTime().toEpochMilli() == t.getStartTime().toEpochMilli();
	}

	public boolean isCorrect() {
		return this.endTime != null && !this.isIncorrect();
	}

	public boolean isFinished() {
		return this.endTime != null;
	}

}
