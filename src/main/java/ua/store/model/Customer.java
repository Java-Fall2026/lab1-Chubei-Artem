package ua.store.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable customer record with Jakarta Validation constraints.
 */
public record Customer(
        @NotBlank(message = "Email клієнта не може бути порожнім")
        @Email(message = "Некоректний формат email клієнта")
        String email,
        @NotBlank(message = "Ім'я клієнта не може бути порожнім")
        String name,
        @NotBlank(message = "Телефон клієнта не може бути порожнім")
        String phone
        ) {

    public Customer {
        email = (email != null) ? email.trim().toLowerCase() : null;
        name = (name != null) ? name.trim() : null;
        phone = (phone != null) ? phone.trim() : null;
    }
}