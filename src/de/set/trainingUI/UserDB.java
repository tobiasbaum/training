package de.set.trainingUI;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDB {

    private static final File BASE_DIR = new File("users");
    private static Map<String, Trainee> trainees = new ConcurrentHashMap<>();

    static {
        for (final File user : BASE_DIR.listFiles()) {
            loadUser(user);
        }
    }

    private static void loadUser(final File user) {
        trainees.put(user.getName(), new Trainee(user));
    }

    public static Trainee getUser(final String userName) {
        return trainees.computeIfAbsent(userName, (final String n) -> Trainee.createNew(n, BASE_DIR));
    }

}
