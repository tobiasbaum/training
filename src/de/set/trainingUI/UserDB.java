package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDB {

    private static final File BASE_DIR = new File("users");
    private static Map<String, Trainee> trainees = new ConcurrentHashMap<>();

    static {
    	if (!BASE_DIR.exists()) {
    		throw new AssertionError("User directory " + BASE_DIR.getAbsolutePath() + " does not exist");
    	}
        for (final File user : BASE_DIR.listFiles()) {
            try {
				loadUser(user);
			} catch (final Exception e) {
				System.err.println("Skipping user due to error: " + user);
				e.printStackTrace();
			}
        }
    }

    private static void loadUser(final File user) throws IOException {
        trainees.put(user.getName(), Trainee.load(user, TaskDB.getInstance()));
    }

    public static Trainee getUser(final String userName) {
        return trainees.computeIfAbsent(userName, (final String n) -> Trainee.createNew(n, BASE_DIR));
    }

}
