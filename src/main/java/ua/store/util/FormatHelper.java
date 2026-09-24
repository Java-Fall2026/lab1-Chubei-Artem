package ua.store.util;

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
}