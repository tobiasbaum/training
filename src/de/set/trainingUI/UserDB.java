package de.set.trainingUI;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.set.trainingUI.Trainee.TraineeId;

public class UserDB {

    private static final File BASE_DIR = new File("users");
    private static Map<TraineeId, Trainee> trainees = new ConcurrentHashMap<>();

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
        trainees.put(new TraineeId(user.getName()), Trainee.load(user, TaskDB.getInstance()));
    }

    public static Trainee initUser(String authId, final String userName) {
    	final TraineeId id = new TraineeId(authId, userName);
        return trainees.computeIfAbsent(id, (final TraineeId n) -> Trainee.createNew(n, BASE_DIR));
    }

    public static Trainee getUser(String authId, final String userName) {
    	final TraineeId id = new TraineeId(authId, userName);
    	final Trainee t = trainees.get(id);
    	if (t == null) {
    		throw new IllegalStateException("User not logged in: " + id.encode());
    	}
        return t;
    }

}
