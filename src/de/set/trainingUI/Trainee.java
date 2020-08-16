package de.set.trainingUI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Trainee {

    private final File dir;
    private final String name;
    private final List<Trial> trials = new ArrayList<>();
	private Instant currentSessionStart;

    public Trainee(final File dir) {
        this.dir = dir;
        this.name = dir.getName();
    }

    public static Trainee createNew(final String name, final File baseDir) {
        final File f = new File(baseDir, name);
        f.mkdir();
        return new Trainee(f);
    }

    public String getName() {
        return this.name;
    }

    public Trial startNewTrial(final Task task) {
        return this.startNewTrial(task, 0);
    }

    private Trial startNewTrial(final Task task, final int retryCount) {
        this.saveTrials();

        final Trial t = new Trial(task, retryCount);
        this.trials.add(t);
        return t;
    }

    private void saveTrials() {
        final File f = new File(this.dir, "trials");
        try (FileOutputStream w = new FileOutputStream(f)) {
            for (final Trial t : this.trials) {
                w.write(t.serialize().getBytes("UTF-8"));
                w.write('\n');
            }
        } catch (final IOException e) {
            throw new RuntimeException("could not persist data", e);
        }
    }

    public Trial retryCurrentTask() {
        final Trial prev = this.getCurrentTrial();
        return this.startNewTrial(prev.getTask(), prev.getRetryCount() + 1);
    }

    public Trial getCurrentTrial() {
        return this.trials.isEmpty() ? null : this.trials.get(this.trials.size() - 1);
    }

    public Instant getLastTrialFromFamily(final Task task) {
        for (int i = this.trials.size() - 1; i >= 0; i--) {
            if (this.trials.get(i).getTask().isSameFamily(task)) {
                return this.trials.get(i).getStartTime();
            }
        }
        return Instant.EPOCH;
    }

	public void setCurrentSessionStart(Instant instant) {
		this.currentSessionStart = instant;
	}

	public int getMinutesInCurrentSession() {
		if (this.currentSessionStart == null) {
			return 0;
		}
		return (int) Duration.between(this.currentSessionStart, Instant.now()).toMinutes();
	}

}
