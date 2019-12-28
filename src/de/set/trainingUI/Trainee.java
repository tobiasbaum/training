package de.set.trainingUI;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Trainee {

    private final String name;
    private final List<Trial> trials = new ArrayList<>();

    public Trainee(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Trial startNewTrial(final Task task) {
        return this.startNewTrial(task, 0);
    }

    private Trial startNewTrial(final Task task, final int retryCount) {
        final Trial t = new Trial(task, retryCount);
        this.trials.add(t);
        return t;
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

}
