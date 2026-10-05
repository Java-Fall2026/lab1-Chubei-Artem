package ua.store.util;

import java.time.LocalDate;

import ua.store.model.OrderItem;
import ua.store.model.OrderStatus;

public final class StoreUtils {

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

    public static void validateQuantity(int quantity) {
        ValidationHelper.requireInRange(quantity, ValidationHelper.MIN_QUANTITY, ValidationHelper.MAX_QUANTITY, "Quantity");
    }

    public static void validateUnitPrice(double unitPrice) {
        ValidationHelper.requireStrictlyPositive(unitPrice, "Unit price");
    }

    public static String formatMoney(double amount) {
        return FormatHelper.formatMoney(amount);
    }

    public static String capitalize(String text) {
        return FormatHelper.capitalize(text);
    }

    // Switch expression 1: exhaustive by enum constants without default
    public static String getProcessingPriority(OrderStatus status) {
        ValidationHelper.requireNotNull(status, "Status");
        return switch (status) {
            case NEW -> "High priority: payment verification pending";
            case PAID -> "Urgent: prepare warehouse packaging";
            case SHIPPED -> "Standard: in transit to customer";
            case DELIVERED -> "Low: completed";
            case CANCELLED -> "Archived: cancelled order";
        };
    }

    // Switch expression 2: business rule choice (discount calculation)
    public static double calculateOrderDiscountPercent(double totalAmount) {
        ValidationHelper.requireNonNegative(totalAmount, "Total amount");

        int tier = (totalAmount >= 50000) ? 3 :
                   (totalAmount >= 20000) ? 2 :
                   (totalAmount >= 5000)  ? 1 : 0;

        return switch (tier) {
            case 3 -> 0.15;
            case 2 -> 0.10;
            case 1 -> 0.05;
            case 0 -> 0.00;
            default -> throw new IllegalStateException("Unexpected discount tier: " + tier);
        };
    }

    public static double lineTotal(OrderItem item) {
        ValidationHelper.requireNotNull(item, "OrderItem");
        return item.getQuantity() * item.getUnitPrice();
    }

    public static double priceDifference(OrderItem item) {
        ValidationHelper.requireNotNull(item, "OrderItem");
        return item.getUnitPrice() - item.getProduct().getPrice();
    }
}