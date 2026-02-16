package me.typical.lixiplugin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses money strings: 100, 1.5k, 2M, 5.5T. */
public class MoneyUtil {

    private static final Pattern MONEY_PATTERN = Pattern.compile("^([0-9]*\\.?[0-9]+)([kMT])?$", Pattern.CASE_INSENSITIVE);

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
