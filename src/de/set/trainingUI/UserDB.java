package de.set.trainingUI;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserDB {

    private static Map<String, Trainee> trainees = new ConcurrentHashMap<>();

    public static Trainee getUser(final String userName) {
        return trainees.computeIfAbsent(userName, (final String n) -> new Trainee(n));
    }

}
