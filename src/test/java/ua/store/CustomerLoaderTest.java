package ua.store;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.store.exception.InvalidDataException;
import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderItem;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.storage.DataLoader;
import ua.store.util.CustomerLoader;
import ua.store.util.StoreUtils;

class CustomerLoaderTest {

    private final Validator validator = DataLoader.createDefaultValidator();

    @Test
    @DisplayName("Should successfully parse a valid CSV line into Customer record")
    void testParseValidCustomer() throws InvalidDataException {
        String line = "artem.chubei@gmail.com,Артем,+380501112233";
        Customer customer = CustomerLoader.parseCustomerFromLine(line, 2);

        assertAll("Valid customer components",
                () -> assertNotNull(customer),
                () -> assertEquals("artem.chubei@gmail.com", customer.email()),
                () -> assertEquals("Артем", customer.name()),
                () -> assertEquals("+380501112233", customer.phone())
        );
    }

    @Test
    @DisplayName("Should throw InvalidDataException when column count is invalid")
    void testParseExceptionWhenColumnsMissing() {
        String invalidLine = "ivan@gmail.com,Іван"; // only 2 columns

        InvalidDataException exception = assertThrows(
                InvalidDataException.class,
                () -> CustomerLoader.parseCustomerFromLine(invalidLine, 4)
        );

        assertTrue(exception.getMessage().contains("expected 3 columns"));
        assertTrue(exception.getMessage().contains("Line 4"));
    }

    @Test
    @DisplayName("Should detect blank email via Jakarta Validation after parsing CSV line")
    void testParseBlankEmailDetectedByValidator() throws InvalidDataException {
        String invalidLine = ",Максим,+380931234567";
        Customer customer = CustomerLoader.parseCustomerFromLine(invalidLine, 5);

        Set<ConstraintViolation<Customer>> violations = validator.validate(customer);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Should correctly calculate line total for OrderItem")
    void testCalculatedMethodLineTotal() {
        Customer customer = new Customer("test@store.ua", "Test", "+38000");
        Order order = new Order("ORD-001", customer, LocalDate.now(), OrderStatus.NEW, 500.0);
        Product product = Product.of("SKU-001", "Mouse", 250.0);
        OrderItem item = new OrderItem(order, product, 3, 250.0);

        double total = StoreUtils.lineTotal(item);
        assertEquals(750.0, total, 0.001);
    }

    @Test
    @DisplayName("Should correctly calculate price difference for OrderItem")
    void testCalculatedMethodPriceDifference() {
        Customer customer = new Customer("test@store.ua", "Test", "+38000");
        Order order = new Order("ORD-001", customer, LocalDate.now(), OrderStatus.NEW, 500.0);
        Product product = Product.of("SKU-001", "Mouse", 300.0);
        OrderItem item = new OrderItem(order, product, 1, 260.0);

        double diff = StoreUtils.priceDifference(item);
        assertEquals(-40.0, diff, 0.001);
    }

    @Test
    @DisplayName("Should detect negative totalAmount via Jakarta Validation when modifying Order")
    void testOrderInvalidSetterDetectedByValidator() {
        Customer customer = new Customer("test@store.ua", "Test", "+38000");
        Order order = new Order("ORD-001", customer, LocalDate.now(), OrderStatus.NEW, 500.0);

        order.setTotalAmount(-100.0);
        Set<ConstraintViolation<Order>> violations = validator.validate(order);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("totalAmount")));
    }
}
