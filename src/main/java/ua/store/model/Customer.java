package ua.store.model;

import java.util.Objects;

import ua.common.BaseEntity;
import ua.store.util.StoreUtils;

public class Customer extends BaseEntity {

    private final String email;
    private final String name;
    private final String phone;

    // Конструктор private, оскільки клас створюється через of(...)
    private Customer(String email, String name, String phone) {
        super();
        this.email = StoreUtils.validateAndNormalizeEmail(email);
        this.name = StoreUtils.validateAndNormalizeString(name, "Customer name");
        this.phone = StoreUtils.validateAndNormalizeString(phone, "Customer phone");
    }

    public static Customer of(String email, String name, String phone) {
        return new Customer(email, name, phone);
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(email, customer.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}