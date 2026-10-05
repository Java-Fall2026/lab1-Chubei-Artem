package ua.store.model;

import ua.store.util.StoreUtils;

/**
 * Незмінна сутність покупця (record).
 * Усі компоненти є private final за замовчуванням.
 */
public record Customer(String email, String name, String phone) {

    public Customer {
        email = StoreUtils.validateAndNormalizeEmail(email);
        name = StoreUtils.validateAndNormalizeString(name, "Customer name");
        phone = StoreUtils.validateAndNormalizeString(phone, "Customer phone");
    }
}