package de.set.trainingUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

public class TaskDB {

    private static final int SAMPLE_SIZE = 10;

	private static TaskDB INSTANCE;

    private final AtomicReference<Task[]> tasks = new AtomicReference<>();
    private Random random;
	private final Timer reloadTimer = new Timer("TaskDB update", true);

    private TaskDB(List<File> roots) {
        try {
            this.loadTasks(roots);
            this.random = new Random(1234);
            this.reloadTimer.schedule(
            		new TimerTask() {
						@Override
						public void run() {
							try {
								TaskDB.this.loadTasks(roots);
							} catch (final IOException e) {
								e.printStackTrace();
							}
						}
            		},
            		5 * 60 * 1000L,
            		5 * 60 * 1000L);
        } catch (final IOException e) {
            throw new RuntimeException("problem while loading tasks", e);
        }
    }

	private void loadTasks(List<File> roots) throws IOException {
		final List<Task> t = new ArrayList<>();
		for (final File root: roots) {
			for (final File dir : root.listFiles()) {
			    if (!dir.isDirectory()) {
			        continue;
			    }
			    if (this.isIgnored(dir)) {
			    	continue;
			    }
			    t.add(this.loadTask(dir));
			}
		}
		t.sort(Comparator.comparingDouble(Task::estimateDifficulty));
		this.tasks.set(t.toArray(new Task[t.size()]));
	}

    private boolean isIgnored(File dir) {
		return new File(dir, "ignore").exists();
	}

    private Task loadTask(final File taskDirectory) throws IOException {
    	try {
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
    	} catch (final Exception e) {
    		throw new IOException("Problem parsing " + taskDirectory, e);
    	}
    }

    public static void init(List<File> paths) {
    	INSTANCE = new TaskDB(paths);
    }

    public static TaskDB getInstance() {
    	if (INSTANCE == null) {
    		throw new AssertionError("TaskDB not yet initialized");
    	}
        return INSTANCE;
    }

    private static final class TaskFamilyStats {
    	private final List<Task> tasks;
    	private final Instant lastTrial;
    	private final double difficulty;

    	public TaskFamilyStats(Trainee trainee, Task t) {
    		this.tasks = new ArrayList<Task>();
    		this.add(t);
			this.lastTrial = trainee.getLastTrialFromFamily(t);
    		this.difficulty = t.estimateDifficulty();
		}

		public Instant getLastTrial() {
    		return this.lastTrial;
    	}

    	public double getDifficulty() {
    		return this.difficulty;
    	}

		public void add(Task t) {
			this.tasks.add(t);
		}
    }

    public Task getNextTask(final Trainee trainee) {
        final List<TaskFamilyStats> tasks = this.getFamilyStats(trainee);

        // get a random sample of 20 tasks
        final List<TaskFamilyStats> sample = new ArrayList<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            sample.add(tasks.get(this.nextRandomInt(tasks.size())));
        }

        // sort by last time a task from the same family was done
        // and remove the half that has been done most recently
        Collections.sort(sample, Comparator.comparing(TaskFamilyStats::getLastTrial));
        while (sample.size() > SAMPLE_SIZE / 2) {
            sample.remove(sample.size() - 1);
        }

		sample.sort(Comparator.comparingDouble(TaskFamilyStats::getDifficulty));
        final Trial lastTrial = trainee.getCurrentTrial();
        if (lastTrial != null) {
            final double lastTrialDifficulty = lastTrial.getTask().estimateDifficulty();
            if (lastTrial.isIncorrect()) {
            	// if the last trial was incorrect, drop to the middle of the easier tasks
            	final List<TaskFamilyStats> easierTasks = new ArrayList<>();
                for (final TaskFamilyStats t : sample) {
                    if (t.getDifficulty() < lastTrialDifficulty) {
                        easierTasks.add(t);
                    }
                }
                if (easierTasks.isEmpty()) {
                	// none of the tasks is easier, choose the easiest one from the sample
                	return this.pickFromFamily(sample.get(0));
                } else {
                	return this.pickFromFamily(getMiddle(easierTasks));
                }
            } else {
            	// if the last trial was correct, choose the trial that is minimally harder
                for (final TaskFamilyStats t : sample) {
                    if (t.getDifficulty() > lastTrialDifficulty) {
                        return this.pickFromFamily(t);
                    }
                }
            	// none of the tasks is harder, choose the hardest one from the sample
                return this.pickFromFamily(sample.get(sample.size() - 1));
            }
        } else {
        	// if this is the first trial, choose an easy task
        	return this.pickFromFamily(sample.get(0));
        }
    }

	private List<TaskFamilyStats> getFamilyStats(Trainee trainee) {
		final Map<String, TaskFamilyStats> stats = new LinkedHashMap<>();
		for (final Task t : this.tasks.get()) {
			TaskFamilyStats fs = stats.get(t.getFamilyId());
			if (fs == null) {
				fs = new TaskFamilyStats(trainee, t);
				stats.put(t.getFamilyId(), fs);
			} else {
				fs.add(t);
			}
		}
		return new ArrayList<>(stats.values());
	}

	private Task pickFromFamily(TaskFamilyStats family) {
		return family.tasks.get(this.nextRandomInt(family.tasks.size()));
	}

	private static TaskFamilyStats getMiddle(final List<TaskFamilyStats> easierTasks) {
		return easierTasks.get(easierTasks.size() / 2);
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

	public void shutdown() {
		this.reloadTimer.cancel();
	}

	public int getTaskCount() {
		return this.tasks.get().length;
	}

}
