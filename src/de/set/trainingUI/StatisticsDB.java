package de.set.trainingUI;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class StatisticsDB {

	private static final File SAVE_FILE = new File("statistics.save");

    private static final StatisticsDB INSTANCE = new StatisticsDB();

    private final Map<String, Statistics> taskStatistics;
    private final Map<String, Statistics> familyStatistics;
	private final Timer saveTimer = new Timer("StatisticsDB save", true);

	private StatisticsDB() {
		if (SAVE_FILE.exists()) {
			try (FileInputStream f = new FileInputStream(SAVE_FILE)) {
				final ObjectInputStream ois = new ObjectInputStream(f);
				this.familyStatistics = (Map<String, Statistics>) ois.readObject();
				this.taskStatistics = (Map<String, Statistics>) ois.readObject();
			} catch (final IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else {
			this.familyStatistics = new HashMap<>();
			this.taskStatistics = new HashMap<>();
		}
		this.saveTimer.schedule(
				new TimerTask() {
					@Override
					public void run() {
						StatisticsDB.this.save();
					}
				},
				1000L,
				60 * 1000L);
	}

    public static StatisticsDB getInstance() {
        return INSTANCE;
    }

    public Statistics getFor(final Task task) {
        final Statistics t = this.getOrCreate(this.taskStatistics, task.getId(), false);
        if (t.getFirstTryCount() >= 5) {
        	// if there is enough data for this single task, return it
            return t.copy();
        }
        // otherwise return the statistics for the whole family
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

	public void shutdown() {
		this.saveTimer.cancel();
		this.save();
	}

	private synchronized void save() {
		final File tmpFile = new File("statistics.tmp");
		try (FileOutputStream f = new FileOutputStream(tmpFile)) {
			final ObjectOutputStream oos = new ObjectOutputStream(f);
			oos.writeObject(this.familyStatistics);
			oos.writeObject(this.taskStatistics);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		tmpFile.renameTo(SAVE_FILE);
		tmpFile.delete();
	}

}
