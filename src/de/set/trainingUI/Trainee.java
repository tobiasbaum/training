package de.set.trainingUI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import spark.Request;

public class Trainee {

    private final File dir;
    private final String name;
    private final List<Trial> trials = new ArrayList<>();
	private Instant currentSessionStart;
	private AnnotatedSolution curSolution;

    private Trainee(final File dir) {
        this.dir = dir;
        this.name = dir.getName();
    }

    public static Trainee createNew(final String name, final File baseDir) {
        final File f = new File(baseDir, name);
        f.mkdir();
        return new Trainee(f);
    }

    public static Trainee load(final File traineeDir, TaskDB tasks) throws IOException {
    	final Trainee ret = new Trainee(traineeDir);
    	final File trialsFile = ret.getTrialsFile();
    	if (trialsFile.exists()) {
    		final Map<Instant, Trial> trials = new LinkedHashMap<>();
    		Files.lines(trialsFile.toPath())
    			.map((String line) -> Trial.deserialize(line, tasks))
    			.filter(Objects::nonNull)
    			.forEach((Trial t) -> trials.put(t.getStartTime(), t));
    		ret.trials.addAll(trials.values());
    	}
    	return ret;
    }

    public String getName() {
        return this.name;
    }

    public synchronized Trial startNewTrial(final Task task) {
        return this.startNewTrial(task, 0);
    }

    private Trial startNewTrial(final Task task, final int retryCount) {
    	Trial t;
        do {
        	t = new Trial(task, retryCount);
        } while (this.hasSameStartTimeAsPrevious(t));
        this.writeTrialToLog(t);
        this.trials.add(t);
        return t;
    }

    private boolean hasSameStartTimeAsPrevious(Trial t) {
    	return !this.trials.isEmpty()
			&& this.trials.get(this.trials.size() - 1).hasSameStartAs(t);
	}

	private void writeTrialToLog(Trial t) {
        final File f = this.getTrialsFile();
        try (FileOutputStream w = new FileOutputStream(f, true)) {
            w.write(t.serialize().getBytes("UTF-8"));
            w.write('\n');
        } catch (final IOException e) {
            throw new RuntimeException("could not persist data", e);
        }
    }

	private File getTrialsFile() {
		return new File(this.dir, "trials");
	}

	public synchronized Trial checkCurrentTrialAnswer(Request request) {
		final Trial t = this.getCurrentTrial();
		this.curSolution = t.checkAnswer(request);
		this.writeTrialToLog(t);
		return t;
	}

	public synchronized AnnotatedSolution getCurrentSolution() {
		return this.curSolution;
	}

    public synchronized Trial retryCurrentTask() {
        final Trial prev = this.getCurrentTrial();
        return this.startNewTrial(prev.getTask(), prev.getRetryCount() + 1);
    }

    public synchronized Trial getCurrentTrial() {
        return this.trials.isEmpty() ? null : this.trials.get(this.trials.size() - 1);
    }

    public synchronized Instant getLastTrialFromFamily(final Task task) {
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
