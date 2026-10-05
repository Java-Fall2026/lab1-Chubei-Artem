package ua.store.model;

/**
 * Order lifecycle status.
 * Contains business flag indicating if cancellation is permitted.
 */
public enum OrderStatus {
    NEW(true, "Order created, waiting for payment"),
    PAID(true, "Paid, preparing for packaging"),
    SHIPPED(false, "Dispatched and in transit"),
    DELIVERED(false, "Delivered successfully"),
    CANCELLED(false, "Order cancelled");

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