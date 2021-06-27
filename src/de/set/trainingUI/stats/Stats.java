package de.set.trainingUI.stats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public class Stats {

	private static class UserInfo {
		int finishedTrials;
		Date lastTrial;
	}

	public static void main(String[] args) throws IOException {
		final File f = new File("downloaded/users");
		for (final File user : f.listFiles()) {
			final UserInfo i = determineInfo(user);
			System.out.println(user.getName() + "\t" + i.finishedTrials + "\t" + i.lastTrial);
		}
	}

	private static UserInfo determineInfo(File userDir) throws IOException {
		final File trials = new File(userDir, "trials");
		final UserInfo ret = new UserInfo();
		try (FileReader f = new FileReader(trials);
				BufferedReader b = new BufferedReader(f)) {
			String line;
			while ((line = b.readLine()) != null) {
				final String[] parts = line.split(";");
				final long start = Long.parseLong(parts[1]);
				ret.lastTrial = new Date(start);
				if (!parts[3].equals("null")) {
					ret.finishedTrials++;
				}
			}
		}
		return ret;
	}

}
