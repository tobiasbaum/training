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

    private static final TaskDB INSTANCE = new TaskDB();

    private final AtomicReference<Task[]> tasks = new AtomicReference<>();
    private Random random;

    private TaskDB() {
        try {
            final File root = new File("taskdb");
            final List<Task> t = new ArrayList<>();
            for (final File dir : root.listFiles()) {
                if (!dir.isDirectory()) {
                    continue;
                }
                t.add(this.loadTask(dir));
            }
            this.sortByDifficulty(t);
            this.tasks.set(t.toArray(new Task[t.size()]));
            this.random = new Random(1234);
        } catch (final IOException e) {
            throw new RuntimeException("problem while loading tasks", e);
        }
    }

    private void sortByDifficulty(final List<Task> taskList) {
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

    public static TaskDB getInstance() {
        return INSTANCE;
    }

    public Task getNextTask(final Trainee trainee) {
        final Task[] tasks = this.tasks.get();

        final List<Task> sample = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sample.add(tasks[this.nextRandomInt(tasks.length)]);
        }

        Collections.sort(sample, Comparator.comparing(trainee::getLastTrialFromFamily));
        while (sample.size() > 5) {
            sample.remove(sample.size() - 1);
        }

        final Trial lastTrial = trainee.getCurrentTrial();
        if (lastTrial != null) {
            final int lastTrialIndex = this.indexOf(tasks, lastTrial.getTask());
            if (lastTrial.isIncorrect()) {
                for (final Task t : sample) {
                    if (this.indexOf(tasks, t) < lastTrialIndex) {
                        return t;
                    }
                }
            } else {
                for (final Task t : sample) {
                    if (this.indexOf(tasks, t) > lastTrialIndex) {
                        return t;
                    }
                }
            }
        }

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

}
