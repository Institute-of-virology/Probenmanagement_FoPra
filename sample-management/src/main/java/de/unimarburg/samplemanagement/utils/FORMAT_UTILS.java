package de.unimarburg.samplemanagement.utils;

public class FORMAT_UTILS {

    public static String getOrdinal(int n) {
        return switch (n) {
            case 0 -> "First";
            case 1 -> "Second";
            case 2 -> "Third";
            default -> (n + 1) + getSuffix(n + 1);
        };
    }

    private static String getSuffix(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
