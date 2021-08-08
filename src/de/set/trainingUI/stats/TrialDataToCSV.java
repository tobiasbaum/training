package de.set.trainingUI.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrialDataToCSV {

	private static class Multiset {
		private final Map<String, Integer> map = new LinkedHashMap<>();

		public int get(String string) {
			final Integer i = this.map.get(string);
			return i == null ? 0 : i;
		}

		public void add(String string) {
			this.map.put(string, this.get(string) + 1);
		}
	}

	public static void main(String[] args) throws IOException {
		final File f = new File("downloaded/users");
		try (FileWriter w = new FileWriter("trialData.csv")) {
			w.write("task;start;retry;end;incorrect;user;trialOverall;trialForTask\n");
			for (final File user : f.listFiles()) {
				convertForUser(user, w);
			}
		}
	}

	private static void convertForUser(File userDir, FileWriter csvOut) throws IOException {
		final File trials = new File(userDir, "trials");
		try (FileReader f = new FileReader(trials);
				BufferedReader b = new BufferedReader(f)) {
			String line;
			int trialNumberOfUser = 0;
			final Multiset countPerTask = new Multiset();
			while ((line = b.readLine()) != null) {
				final String[] parts = line.split(";");
				if (!parts[3].equals("null")) {
					trialNumberOfUser++;
					csvOut.write(line);
					csvOut.write(";usr" + userDir.getName().hashCode() + ";");
					csvOut.write(trialNumberOfUser + ";");
					csvOut.write(Integer.toString(countPerTask.get(parts[0])));
					csvOut.write('\n');
					countPerTask.add(parts[0]);
				}
			}
		}
	}
}
