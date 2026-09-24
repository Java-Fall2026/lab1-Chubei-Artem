package ua.store.model;

import java.time.LocalDate;
import java.util.Objects;

import ua.common.BaseEntity;
import ua.store.util.StoreUtils;

public class Order extends BaseEntity {

    private final String orderId;
    private final Customer customer;
    private final LocalDate orderDate;
    private String status;      // ✎
    private double totalAmount; // ✎

    public Order(String orderId, Customer customer, LocalDate orderDate, String status, double totalAmount) {
        super();
        this.orderId = StoreUtils.validateAndNormalizeString(orderId, "Order ID");
        StoreUtils.validateNotNull(customer, "Customer");
        this.customer = customer;
        StoreUtils.validateOrderDate(orderDate);
        this.orderDate = orderDate;
        setStatus(status);
        setTotalAmount(totalAmount);
    }

    public String getOrderId() {
        return orderId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public String getStatus() {
        return status;
    }

    public final void setStatus(String status) {
        this.status = StoreUtils.validateAndNormalizeStatus(status);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public final void setTotalAmount(double totalAmount) {
        StoreUtils.validateTotalAmount(totalAmount);
        this.totalAmount = totalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customer=" + customer.getEmail() +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                '}';
    }
}