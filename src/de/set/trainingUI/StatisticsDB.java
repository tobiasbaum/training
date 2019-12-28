package de.set.trainingUI;

import java.util.HashMap;
import java.util.Map;

public class StatisticsDB {

    private static final StatisticsDB INSTANCE = new StatisticsDB();

    private final Map<String, Statistics> taskStatistics = new HashMap<>();
    private final Map<String, Statistics> familyStatistics = new HashMap<>();

    public static StatisticsDB getInstance() {
        return INSTANCE;
    }

    public Statistics getFor(final Task task) {
        final Statistics t = this.getOrCreate(this.taskStatistics, task.getId(), false);
        if (t.getFirstTryCount() >= 5) {
            return t.copy();
        }
        return this.getOrCreate(this.familyStatistics, task.getFamilyId(), true).copy();
    }

    public void count(final Trial trial) {
        final Task task = trial.getTask();
        this.getOrCreate(this.taskStatistics, task.getId(), false).count(trial);
        this.getOrCreate(this.familyStatistics, task.getFamilyId(), false).count(trial);
    }

    private synchronized Statistics getOrCreate(
            final Map<String, Statistics> m, final String id, final boolean forFamily) {
        Statistics s = m.get(id);
        if (s == null) {
            s = new Statistics(forFamily);
            m.put(id, s);
        }
        return s;
    }

}
