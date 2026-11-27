package ua.store.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Cross-field validation constraint ensuring Order status and totalAmount are
 * consistent.
 */
@Documented
@Constraint(validatedBy = OrderStatusAmountValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrderStatusAmount {

    String message() default "Сума замовлення не узгоджується зі статусом (для PAID/SHIPPED/DELIVERED сума має бути > 0, для CANCELLED — дорівнювати 0)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
