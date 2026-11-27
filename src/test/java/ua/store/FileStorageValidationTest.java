package ua.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.store.config.AppConfig;
import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.ProductRepository;
import ua.store.storage.DataLoader;
import ua.store.storage.FileStorage;
import ua.store.storage.LoadResult;

class FileStorageValidationTest {

    private final Validator validator = DataLoader.createDefaultValidator();

    @Test
    @DisplayName("1. JSON: save and load return equal list of products")
    void testJsonSaveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        Path jsonFile = tempDir.resolve("products.json");
        FileStorage<Product> storage = new FileStorage<>(Product.class);

        List<Product> original = List.of(
                Product.of("SKU-101", "Mechanical Keyboard", 3200.0),
                Product.of("SKU-102", "Wireless Mouse", 1500.5)
        );

        storage.save(jsonFile, original);
        List<Product> loaded = storage.load(jsonFile);

        assertEquals(original, loaded);
        assertEquals(3200.0, loaded.get(0).getPrice());
    }

    @Test
    @DisplayName("2. YAML: save and load return equal list of orders")
    void testYamlSaveAndLoadRoundTrip(@TempDir Path tempDir) throws IOException {
        Path yamlFile = tempDir.resolve("orders.yaml");
        FileStorage<Order> storage = new FileStorage<>(Order.class);

        Customer customer = new Customer("artem@store.ua", "Artem", "+380501112233");
        List<Order> original = List.of(
                new Order("ORD-001", customer, LocalDate.of(2026, 11, 2), OrderStatus.PAID, 3200.0),
                new Order("ORD-002", customer, LocalDate.of(2026, 11, 5), OrderStatus.CANCELLED, 0.0)
        );

        storage.save(yamlFile, original);
        List<Order> loaded = storage.load(yamlFile);

        assertEquals(original, loaded);
        assertEquals(LocalDate.of(2026, 11, 2), loaded.get(0).getOrderDate());
    }

    @Test
    @DisplayName("3. Saved dates are serialized as ISO-8601 strings rather than numeric arrays")
    void testDateSerializedAsIsoString(@TempDir Path tempDir) throws IOException {
        Path jsonFile = tempDir.resolve("orders.json");
        FileStorage<Order> storage = new FileStorage<>(Order.class);

        Customer customer = new Customer("client@store.ua", "Client", "+380501112233");
        Order order = new Order("ORD-100", customer, LocalDate.of(2026, 11, 2), OrderStatus.NEW, 500.0);

        storage.save(jsonFile, List.of(order));
        String rawJson = Files.readString(jsonFile);

        assertTrue(rawJson.contains("\"2026-11-02\""), "Expected ISO date string in JSON output");
        assertFalse(rawJson.contains("[2026,11,2]"), "Date must not be serialized as array of numbers");
    }

    @Test
    @DisplayName("4. Unsupported file extension throws IllegalArgumentException")
    void testUnsupportedFileExtensionThrows(@TempDir Path tempDir) {
        Path xmlFile = tempDir.resolve("products.xml");
        FileStorage<Product> storage = new FileStorage<>(Product.class);

        IllegalArgumentException loadEx = assertThrows(IllegalArgumentException.class, () -> storage.load(xmlFile));
        IllegalArgumentException saveEx = assertThrows(IllegalArgumentException.class, () -> storage.save(xmlFile, List.of()));
        assertTrue(loadEx.getMessage().contains(".xml"));
        assertTrue(saveEx.getMessage().contains(".xml"));
    }

    public static Stream<Arguments> invalidEntitiesProvider() {
        Customer validCustomer = new Customer("valid@store.ua", "Valid Name", "+380501112233");
        return Stream.of(
                // Product constraints: @NotBlank / @Pattern on sku, @Size on title, @Positive on price
                Arguments.of(Product.of("   ", "Valid Title", 100.0), "sku"),
                Arguments.of(Product.of("INVALID", "Valid Title", 100.0), "sku"),
                Arguments.of(Product.of("SKU-101", "A", 100.0), "title"),
                Arguments.of(Product.of("SKU-101", "Valid Title", -10.0), "price"),
                // Order & Customer constraints: @Email, @NotNull, @PastOrPresent, @PositiveOrZero
                Arguments.of(new Order("ORD-001", new Customer("bad-email", "Name", "+38050"),
                        LocalDate.of(2026, 10, 10), OrderStatus.NEW, 100.0), "customer.email"),
                Arguments.of(new Order("ORD-001", null,
                        LocalDate.of(2026, 10, 10), OrderStatus.NEW, 100.0), "customer"),
                Arguments.of(new Order("ORD-001", validCustomer,
                        LocalDate.now().plusDays(10), OrderStatus.NEW, 100.0), "orderDate"),
                Arguments.of(new Order("ORD-001", validCustomer,
                        LocalDate.of(2026, 10, 10), OrderStatus.NEW, -50.0), "totalAmount")
        );
    }

    @ParameterizedTest(name = "5. Standard annotation violation detected on field: {1}")
    @MethodSource("invalidEntitiesProvider")
    @DisplayName("5. Standard Jakarta Validation annotations catch invalid values on expected fields")
    void testStandardValidationAnnotations(Object entity, String expectedField) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);

        assertFalse(violations.isEmpty(), "Expected validation violations for invalid entity");
        boolean hasExpectedField = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(expectedField));
        assertTrue(hasExpectedField, "Expected violation on field '" + expectedField + "', actual: " + violations);
    }

    @Test
    @DisplayName("6. Custom cross-field annotation @ValidOrderStatusAmount validates status vs totalAmount")
    void testCustomCrossFieldAnnotation() {
        Customer customer = new Customer("client@store.ua", "Client", "+380501112233");

        Order validPaid = new Order("ORD-010", customer, LocalDate.of(2026, 10, 15), OrderStatus.PAID, 1500.0);
        Order validCancelled = new Order("ORD-011", customer, LocalDate.of(2026, 10, 15), OrderStatus.CANCELLED, 0.0);
        Order invalidPaidZero = new Order("ORD-012", customer, LocalDate.of(2026, 10, 15), OrderStatus.PAID, 0.0);
        Order invalidCancelledPositive = new Order("ORD-013", customer, LocalDate.of(2026, 10, 15), OrderStatus.CANCELLED, 500.0);

        assertAll("Cross-field @ValidOrderStatusAmount rules",
                () -> assertTrue(validator.validate(validPaid).isEmpty()),
                () -> assertTrue(validator.validate(validCancelled).isEmpty()),
                () -> assertFalse(validator.validate(invalidPaidZero).isEmpty()),
                () -> assertFalse(validator.validate(invalidCancelledPositive).isEmpty())
        );
    }

    @Test
    @DisplayName("7. DataLoader adds only valid records and groups all validation violations per record in LoadResult")
    void testDataLoaderFiltersInvalidRecords(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("invalid_products.json");
        FileStorage<Product> storage = new FileStorage<>(Product.class);

        List<Product> items = List.of(
                Product.of("SKU-101", "Valid Keyboard", 2500.0),
                Product.of("BAD-SKU", "Invalid SKU", 1000.0),
                Product.of("SKU-102", "X", -5.0), // Violates 2 rules: title @Size and price @Positive
                Product.of("SKU-103", "Valid Monitor", 9000.0)
        );
        storage.save(file, items);

        ProductRepository repo = new ProductRepository();
        LoadResult result;
        try (DataLoader<Product> loader = new DataLoader<>(storage, validator)) {
            result = loader.loadInto(file, repo);
        }

        assertAll("Invalid records filtering verification",
                () -> assertEquals(4, result.totalRead()),
                () -> assertEquals(2, result.addedCount()),
                () -> assertEquals(2, repo.size()),
                () -> assertEquals(2, result.rejectedCount()),
                () -> assertEquals(2, result.rejected().get(1).errors().size())
        );
    }

    @Test
    @DisplayName("8. DataLoader captures duplicate identity in LoadResult without throwing")
    void testDataLoaderRejectsDuplicateIdentity(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("duplicate_products.json");
        FileStorage<Product> storage = new FileStorage<>(Product.class);

        List<Product> items = List.of(
                Product.of("SKU-101", "Original Keyboard", 2500.0),
                Product.of("SKU-101", "Duplicate Keyboard", 2600.0)
        );
        storage.save(file, items);

        ProductRepository repo = new ProductRepository();
        LoadResult result;
        try (DataLoader<Product> loader = new DataLoader<>(storage, validator)) {
            result = loader.loadInto(file, repo);
        }

        assertAll("Duplicate identity handling verification",
                () -> assertEquals(2, result.totalRead()),
                () -> assertEquals(1, result.addedCount()),
                () -> assertEquals(1, repo.size()),
                () -> assertEquals(1, result.rejectedCount()),
                () -> assertTrue(result.rejected().get(0).errors().get(0).startsWith("duplicate:"))
        );
    }

    @Test
    @DisplayName("9. Corrupted JSON and missing files do not crash DataLoader and are reported in LoadResult")
    void testBrokenAndMissingFilesDoNotCrashLoader(@TempDir Path tempDir) throws IOException {
        Path brokenFile = tempDir.resolve("broken.json");
        Files.writeString(brokenFile, "[ { \"sku\": \"SKU-101\", \"title\": \"Broken\" ");
        Path missingFile = tempDir.resolve("does_not_exist.yaml");

        ProductRepository repo = new ProductRepository();
        LoadResult brokenResult;
        LoadResult missingResult;
        try (DataLoader<Product> loader = new DataLoader<>(Product.class)) {
            brokenResult = loader.loadInto(brokenFile, repo);
            missingResult = loader.loadInto(missingFile, repo);
        }

        assertAll("Safe failure handling on unreadable files",
                () -> assertEquals(0, brokenResult.totalRead()),
                () -> assertEquals(0, brokenResult.addedCount()),
                () -> assertEquals(1, brokenResult.rejectedCount()),
                () -> assertEquals(0, missingResult.totalRead()),
                () -> assertEquals(1, missingResult.rejectedCount()),
                () -> assertEquals(0, repo.size())
        );
    }

    @Test
    @DisplayName("10. Missing key in Properties throws exception containing the missing key name")
    void testMissingPropertyKeyThrowsWithKeyName() {
        Properties props = new Properties();
        props.setProperty("existing.key", "data/products.json");
        AppConfig config = new AppConfig(props);

        String missingKey = "data.orders.file";
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> config.getRequiredPath(missingKey)
        );

        assertTrue(ex.getMessage().contains(missingKey),
                "Exception message must include the missing key name");
    }
}
