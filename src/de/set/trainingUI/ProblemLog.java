package de.set.trainingUI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProblemLog {

	private static final ProblemLog INSTANCE = new ProblemLog();

	private final File dir;

	private ProblemLog() {
		this.dir = new File("problems");
		this.dir.mkdirs();
	}

	public static ProblemLog getInstance() {
		return INSTANCE;
	}

	public void registerProblem(Trainee trainee, String message) throws IOException {
		final long time = System.currentTimeMillis();
		final String traineeId = trainee.getName();
		final String taskId = trainee.getCurrentTrial().getTask().getId();

		final File problemFile = new File(this.dir, String.format("%s.%s.problem", taskId, time));
		try (FileWriter w = new FileWriter(problemFile)) {
			w.write("task:" + taskId + "\n");
			w.write("trainee:" + traineeId + "\n");
			w.write("\n");
			w.write(message);
		}
	}

}
