package me.typical.lixiplugin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing money values with short format suffixes.
 * Supports: k (thousand), M (million), T (trillion)
 */
public class MoneyUtil {

    private static final Pattern MONEY_PATTERN = Pattern.compile("^([0-9]*\\.?[0-9]+)([kMT])?$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse a money string with optional short format suffix.
     * Examples:
     * - "100" -> 100.0
     * - "1.5k" -> 1500.0
     * - "2M" -> 2000000.0
     * - "5.5T" -> 5500000000000.0
     *
     * @param input The input string to parse
     * @return The parsed money value as double
     * @throws IllegalArgumentException if the input format is invalid
     */
    public static double parseMoneyValue(String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Money value cannot be null or empty");
        }

        Matcher matcher = MONEY_PATTERN.matcher(input.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid money format: " + input);
        }

        String numberPart = matcher.group(1);
        String suffix = matcher.group(2);

        double baseValue;
        try {
            baseValue = Double.parseDouble(numberPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + input);
        }

        if (baseValue < 0) {
            throw new IllegalArgumentException("Money value cannot be negative");
        }

        if (suffix == null) {
            return baseValue;
        }

        return switch (suffix.toLowerCase()) {
            case "k" -> baseValue * 1_000;
            case "m" -> baseValue * 1_000_000;
            case "t" -> baseValue * 1_000_000_000_000L;
            default -> throw new IllegalArgumentException("Unknown suffix: " + suffix);
        };
    }

    /**
     * Format a money value into a readable string with appropriate suffix.
     *
     * @param value The money value to format
     * @return Formatted string (e.g., "1.5k", "2M", "3.2T")
     */
    public static String formatMoney(double value) {
        if (value >= 1_000_000_000_000L) {
            return String.format("%.2fT", value / 1_000_000_000_000.0);
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.2fk", value / 1_000.0);
        } else {
            return String.format("%.2f", value);
        }
    }
}
