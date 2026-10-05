package ua.store.model;

/**
 * Статус життєвого циклу замовлення.
 * Містить бізнес-прапорець можливості скасування та опис.
 */
public enum OrderStatus {
    NEW(true, "Створено, очікує оплати"),
    PAID(true, "Оплачено, готується до відправки"),
    SHIPPED(false, "Відправлено покупцю"),
    DELIVERED(false, "Доставлено й завершено"),
    CANCELLED(false, "Скасовано");

    private final boolean cancellable;
    private final String description;

    OrderStatus(boolean cancellable, String description) {
        this.cancellable = cancellable;
        this.description = description;
    }

    public boolean isCancellable() {
        return cancellable;
    }

    public String getDescription() {
        return description;
    }
}