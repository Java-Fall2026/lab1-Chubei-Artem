package ua.store.model;

import ua.store.util.StoreUtils;

/**
 * Immutable customer record.
 * Components are private final by default.
 */
public record Customer(String email, String name, String phone) {

    public Customer {
        email = StoreUtils.validateAndNormalizeEmail(email);
        name = StoreUtils.validateAndNormalizeString(name, "Customer name");
        phone = StoreUtils.validateAndNormalizeString(phone, "Customer phone");
    }
}