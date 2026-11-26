package ua.store.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import ua.common.BaseEntity;
import ua.store.validation.ValidOrderStatusAmount;

@ValidOrderStatusAmount
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order extends BaseEntity implements Comparable<Order> {

    private static final Comparator<Order> NATURAL_ORDER = Comparator
            .comparingDouble(Order::getTotalAmount)
            .thenComparing(Order::getOrderId);

    @NotBlank(message = "Номер замовлення не може бути порожнім")
    @Pattern(regexp = "^ORD-\\d{3,}$", message = "Номер замовлення має відповідати формату ORD-XXX (мінімум 3 цифри)")
    private final String orderId;

    @NotNull(message = "Клієнт замовлення обов'язковий")
    @Valid
    private final Customer customer;

    @NotNull(message = "Дата замовлення обов'язкова")
    @PastOrPresent(message = "Дата замовлення не може бути у майбутньому")
    private final LocalDate orderDate;

    @NotNull(message = "Статус замовлення обов'язковий")
    private OrderStatus status;

    @PositiveOrZero(message = "Загальна сума замовлення не може бути від'ємною")
    private double totalAmount;

    @JsonCreator
    public Order(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("customer") Customer customer,
            @JsonProperty("orderDate") LocalDate orderDate,
            @JsonProperty("status") OrderStatus status,
            @JsonProperty("totalAmount") double totalAmount) {
        super();
        this.orderId = (orderId != null) ? orderId.trim() : null;
        this.customer = customer;
        this.orderDate = orderDate;
        this.status = status;
        this.totalAmount = totalAmount;
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

    public OrderStatus getStatus() {
        return status;
    }

    public final void setStatus(OrderStatus status) {
        this.status = status;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public final void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * Natural order: by totalAmount ascending, then by unique orderId to stay
     * consistent with equals.
     */
    @Override
    public int compareTo(Order other) {
        Objects.requireNonNull(other, "Cannot compare Order with null");
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return "Order{"
                + "orderId='" + orderId + '\''
                + ", customer=" + (customer != null ? customer.email() : "null")
                + ", orderDate=" + orderDate
                + ", status=" + status
                + ", totalAmount=" + totalAmount
                + ", createdAt=" + createdAt
                + '}';
    }
}