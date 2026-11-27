package ua.store.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.store.model.Order;
import ua.store.model.OrderStatus;

/**
 * Validates cross-field business rules between OrderStatus and totalAmount.
 */
public class OrderStatusAmountValidator implements ConstraintValidator<ValidOrderStatusAmount, Order> {

    @Override
    public boolean isValid(Order order, ConstraintValidatorContext context) {
        if (order == null || order.getStatus() == null) {
            return true;
        }

        OrderStatus status = order.getStatus();
        double amount = order.getTotalAmount();

        if (status == OrderStatus.CANCELLED) {
            return Double.compare(amount, 0.0) == 0;
        }

        if (status == OrderStatus.PAID || status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            return amount > 0.0;
        }

        return true;
    }
}
