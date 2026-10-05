package ua.store.util;

import java.util.Locale;

final class FormatHelper {

    private FormatHelper() {
    }

    static String trimAndLower(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    static String trimAndUpper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    static String trim(String value) {
        return value == null ? null : value.trim();
    }

    static String capitalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return value;
        }
        String trimmed = value.trim();
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1).toLowerCase();
    }

    static String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f UAH", amount);
    }
}