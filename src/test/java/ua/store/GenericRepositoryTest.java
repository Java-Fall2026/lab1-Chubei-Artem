package ua.store;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ua.store.exception.DuplicateIdentityException;
import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.GenericRepository;

class GenericRepositoryTest {

    private final GenericRepository<Customer> customerRepo
            = new GenericRepository<>("CustomerRepo", Customer::email);

    @Test
    @DisplayName("add and findByIdentity should successfully store and retrieve an item")
    void testAddAndFindByIdentity() {
        Customer customer = new Customer("alice@store.ua", "Alice", "+380501112233");
        customerRepo.add(customer);

        Optional<Customer> found = customerRepo.findByIdentity("alice@store.ua");

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().name());
    }

    @Test
    @DisplayName("findByIdentity should return empty Optional for missing identity")
    void testFindByIdentityMissing() {
        Optional<Customer> found = customerRepo.findByIdentity("ghost@store.ua");
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("add should throw DuplicateIdentityException when adding duplicate identity")
    void testAddDuplicateThrowsException() {
        Customer c1 = new Customer("duplicate@store.ua", "First", "+380501111111");
        Customer c2 = new Customer("duplicate@store.ua", "Second", "+380502222222");

        customerRepo.add(c1);

        DuplicateIdentityException ex = assertThrows(
                DuplicateIdentityException.class,
                () -> customerRepo.add(c2)
        );

        assertTrue(ex.getMessage().contains("duplicate@store.ua"));
        assertTrue(ex.getMessage().contains("CustomerRepo"));
    }

    @Test
    @DisplayName("addIfAbsent should return false and not alter size on duplicate identity")
    void testAddIfAbsentOnDuplicate() {
        Customer c1 = new Customer("test@store.ua", "Alice", "+380501112233");
        Customer c2 = new Customer("test@store.ua", "Bob", "+380509998877");

        assertTrue(customerRepo.addIfAbsent(c1));
        assertEquals(1, customerRepo.size());

        assertFalse(customerRepo.addIfAbsent(c2));
        assertEquals(1, customerRepo.size());
    }

    @Test
    @DisplayName("removeByIdentity should return true for present item and false for missing")
    void testRemoveByIdentity() {
        Customer customer = new Customer("bob@store.ua", "Bob", "+380503334455");
        customerRepo.add(customer);

        assertTrue(customerRepo.removeByIdentity("bob@store.ua"));
        assertEquals(0, customerRepo.size());
        assertFalse(customerRepo.removeByIdentity("bob@store.ua"));
    }

    @Test
    @DisplayName("findAll should return unmodifiable collection preventing repository mutation")
    void testFindAllIsUnmodifiable() {
        Customer customer = new Customer("carol@store.ua", "Carol", "+380504445566");
        customerRepo.add(customer);

        List<Customer> snapshot = customerRepo.findAll();
        assertEquals(1, snapshot.size());

        UnsupportedOperationException ex = assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.add(new Customer("d@store.ua", "D", "+38000"))
        );
        assertNotNull(ex);
        assertEquals(1, customerRepo.size());
    }

    @Test
    @DisplayName("GenericRepository should work with different entity types using method references")
    void testRepositoryWorksWithDifferentTypes() {
        GenericRepository<Product> productRepo = new GenericRepository<>("ProductRepo", Product::getSku);
        Product product = Product.of("SKU-99", "Keyboard", 1200.0);
        productRepo.add(product);

        GenericRepository<Order> orderRepo = new GenericRepository<>("OrderRepo", Order::getOrderId);
        Customer customer = new Customer("artem@store.ua", "Artem", "+380501112233");
        Order order = new Order("ORD-777", customer, LocalDate.now(), OrderStatus.NEW, 1200.0);
        orderRepo.add(order);

        assertAll("Repositories with different types",
                () -> assertTrue(productRepo.findByIdentity("SKU-99").isPresent()),
                () -> assertEquals("Keyboard", productRepo.findByIdentity("SKU-99").get().getTitle()),
                () -> assertTrue(orderRepo.findByIdentity("ORD-777").isPresent()),
                () -> assertEquals(1200.0, orderRepo.findByIdentity("ORD-777").get().getTotalAmount())
        );
    }

    @Test
    @DisplayName("add should throw IllegalArgumentException for null item or blank identity")
    void testAddValidationErrors() {
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> customerRepo.add(null)
        );
        assertNotNull(ex1.getMessage());

        GenericRepository<Customer> brokenExtractorRepo = new GenericRepository<>("BrokenRepo", c -> "   ");
        Customer customer = new Customer("test@store.ua", "Valid", "+380500000000");

        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> brokenExtractorRepo.add(customer)
        );
        assertNotNull(ex2.getMessage());
    }
}
