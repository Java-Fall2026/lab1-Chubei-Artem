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

    // СВІТЧ-ВИРАЗ 1: по всіх константах enum OrderStatus (повний перебір без default)
    public static String getProcessingPriority(OrderStatus status) {
        ValidationHelper.requireNotNull(status, "Status");
        return switch (status) {
            case NEW -> "Високий пріоритет: очікується перевірка оплати";
            case PAID -> "Терміново: замовлення готове до збору на складі";
            case SHIPPED -> "Звичайний: посилка прямує до покупця";
            case DELIVERED -> "Низький: замовлення успішно завершено";
            case CANCELLED -> "В архів: замовлення скасовано";
        };
    }

    // СВІТЧ-ВИРАЗ 2: вибір бізнес-поведінки (розрахунок відсотка знижки)
    public static double calculateOrderDiscountPercent(double totalAmount) {
        ValidationHelper.requireNonNegative(totalAmount, "Total amount");

        int tier = (totalAmount >= 50000) ? 3 :
                   (totalAmount >= 20000) ? 2 :
                   (totalAmount >= 5000)  ? 1 : 0;

        return switch (tier) {
            case 3 -> 0.15; // 15% для чеків від 50 000 грн
            case 2 -> 0.10; // 10% для чеків від 20 000 грн
            case 1 -> 0.05; // 5% для чеків від 5 000 грн
            case 0 -> 0.00; // без знижки
            default -> throw new IllegalStateException("Невідома категорія знижки: " + tier);
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