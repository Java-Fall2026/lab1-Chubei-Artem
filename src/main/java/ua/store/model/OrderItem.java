package ua.store.model;

import java.util.Objects;

import ua.common.BaseEntity;
import ua.store.util.StoreUtils;

public class OrderItem extends BaseEntity {

    private final Order order;
    private final Product product;
    private int quantity;        // ✎
    private final double unitPrice;

    public OrderItem(Order order, Product product, int quantity, double unitPrice) {
        super();
        StoreUtils.validateNotNull(order, "Order");
        StoreUtils.validateNotNull(product, "Product");
        this.order = order;
        this.product = product;
        setQuantity(quantity);
        StoreUtils.validateUnitPrice(unitPrice);
        this.unitPrice = unitPrice;
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public final void setQuantity(int quantity) {
        StoreUtils.validateQuantity(quantity);
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    // 🔑❓: ідентичність визначається парою (order + product)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return Objects.equals(order, orderItem.order) &&
                Objects.equals(product, orderItem.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(order, product);
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "orderId=" + order.getOrderId() +
                ", productSku=" + product.getSku() +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", createdAt=" + createdAt +
                '}';
    }
}