package ua.store.util;

import ua.store.model.OrderItem;
import java.time.LocalDate;
import java.util.Set;

public final class StoreUtils {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "NEW", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"
    );

    private StoreUtils() {
    }

    public static String validateAndNormalizeEmail(String email) {
        ValidationHelper.requireNotBlank(email, "Email");
        return FormatHelper.trimAndLower(email);
    }

    public static String validateAndNormalizeString(String value, String fieldName) {
        ValidationHelper.requireNotBlank(value, fieldName);
        return FormatHelper.trim(value);
    }

    public static void validateNotNull(Object value, String fieldName) {
        ValidationHelper.requireNotNull(value, fieldName);
    }

    public static void validatePrice(double price) {
        ValidationHelper.requireStrictlyPositive(price, "Price");
    }

    public static void validateTotalAmount(double totalAmount) {
        ValidationHelper.requireNonNegative(totalAmount, "Total amount");
    }

    public static void validateOrderDate(LocalDate date) {
        ValidationHelper.requireNotInFuture(date, "Order date");
    }

    public static String validateAndNormalizeStatus(String status) {
        ValidationHelper.requireNotBlank(status, "Status");
        String normalized = FormatHelper.trimAndUpper(status);
        ValidationHelper.requireInAllowedSet(normalized, ALLOWED_STATUSES, "Status");
        return normalized;
    }

    public static void validateQuantity(int quantity) {
        ValidationHelper.requireInRange(quantity, 1, 1000, "Quantity");
    }

    public static void validateUnitPrice(double unitPrice) {
        ValidationHelper.requireStrictlyPositive(unitPrice, "Unit price");
    }

    // Два обов'язкові обчислювані методи за варіантом:
    public static double lineTotal(OrderItem item) {
        ValidationHelper.requireNotNull(item, "OrderItem");
        return item.getQuantity() * item.getUnitPrice();
    }

    public static double priceDifference(OrderItem item) {
        ValidationHelper.requireNotNull(item, "OrderItem");
        return item.getUnitPrice() - item.getProduct().getPrice();
    }
}