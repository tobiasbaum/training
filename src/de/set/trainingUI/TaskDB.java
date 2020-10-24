package de.set.trainingUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class TaskDB {

    private static final int SAMPLE_SIZE = 10;

	private static TaskDB INSTANCE;

    private final AtomicReference<Task[]> tasks = new AtomicReference<>();
    private Random random;

    private TaskDB(File root) {
        try {
            final List<Task> t = new ArrayList<>();
            for (final File dir : root.listFiles()) {
                if (!dir.isDirectory()) {
                    continue;
                }
                t.add(this.loadTask(dir));
            }
            sortByDifficulty(t);
            this.tasks.set(t.toArray(new Task[t.size()]));
            this.random = new Random(1234);
        } catch (final IOException e) {
            throw new RuntimeException("problem while loading tasks", e);
        }
    }

    private static void sortByDifficulty(final List<Task> taskList) {
        taskList.sort(Comparator.comparingDouble(Task::estimateDifficulty));
    }

    private Task loadTask(final File taskDirectory) throws IOException {
        final Properties p = new Properties();
        try (InputStream s = new FileInputStream(new File(taskDirectory, "task.properties"))) {
            p.load(s);
        }
        final String type = p.getProperty("type");
        switch (type) {
        case "understanding":
            return UnderstandingTask.load(p, taskDirectory);
        case "review":
            return DefectFindTask.load(p, taskDirectory);
        default:
            throw new RuntimeException("unknown task type " + type + " in " + taskDirectory);
        }
    }

    public static void init(File path) {
    	INSTANCE = new TaskDB(path);
    }

    public static TaskDB getInstance() {
    	if (INSTANCE == null) {
    		throw new AssertionError("TaskDB not yet initialized");
    	}
        return INSTANCE;
    }

    public Task getNextTask(final Trainee trainee) {
        final Task[] tasks = this.tasks.get();

        // get a random sample of 20 tasks
        final List<Task> sample = new ArrayList<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            sample.add(tasks[this.nextRandomInt(tasks.length)]);
        }

        // sort by last time a task from the same family was done
        // and remove the half that has been done most recently
        Collections.sort(sample, Comparator.comparing(trainee::getLastTrialFromFamily));
        while (sample.size() > SAMPLE_SIZE / 2) {
            sample.remove(sample.size() - 1);
        }

        final Trial lastTrial = trainee.getCurrentTrial();
        if (lastTrial != null) {
            final int lastTrialIndex = this.indexOf(tasks, lastTrial.getTask());
            if (lastTrial.isIncorrect()) {
            	// if the last trial was incorrect, drop to the middle of the easier tasks
            	final List<Task> easierTasks = new ArrayList<>();
                for (final Task t : sample) {
                    if (this.indexOf(tasks, t) < lastTrialIndex) {
                        easierTasks.add(t);
                    }
                }
                if (!easierTasks.isEmpty()) {
                	sortByDifficulty(easierTasks);
                	return easierTasks.get(easierTasks.size() / 2);
                }
            } else {
            	// if the last trial was correct, choose the trial that is minimally harder
            	sortByDifficulty(sample);
                for (final Task t : sample) {
                    if (this.indexOf(tasks, t) > lastTrialIndex) {
                        return t;
                    }
                }
            }
        }

        // if none of the previous applied, just use the least recently used (random) one
        return sample.get(0);
    }

    private int indexOf(final Task[] array, final Task t) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == t) {
                return i;
            }
        }
        return -1;
    }

    private synchronized int nextRandomInt(final int max) {
        return this.random.nextInt(max);
    }

	public Task getTaskById(String id) {
		final Task[] taskArray = this.tasks.get();
		for (final Task t : taskArray) {
			if (t.getId().contentEquals(id)) {
				return t;
			}
		}
		return null;
	}

}
