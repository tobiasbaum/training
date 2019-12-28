package de.set.trainingUI;

public class Util {

    public static String formatTime(final long seconds) {
        if (seconds <= 60) {
            return seconds + " Sek.";
        } else {
            final long min = seconds / 60;
            final long secRemain = seconds % 60;
            return min + " Min. " + secRemain + " Sek.";
        }
    }

}
