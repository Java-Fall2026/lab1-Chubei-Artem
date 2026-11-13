package ua.store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.OrderRepository;
import ua.store.repository.ProductRepository;
import ua.store.repository.SortOrder;

class DomainRepositoriesStreamTest {

    private final ProductRepository productRepo = new ProductRepository();
    private final OrderRepository orderRepo = new OrderRepository();
    private final Customer sampleCustomer = new Customer("client@store.ua", "Client", "+380501112233");

    @Test
    @DisplayName("1. Natural order: Collections.sort produces expected sequence by price then SKU")
    void testNaturalOrderSorting() {
        Product p1 = Product.of("SKU-300", "Monitor", 8000.0);
        Product p2 = Product.of("SKU-100", "Mouse", 500.0);
        Product p3 = Product.of("SKU-200", "Keyboard", 1500.0);

        List<Product> list = new ArrayList<>(List.of(p1, p2, p3));
        Collections.sort(list);

        assertEquals(List.of(p2, p3, p1), list);
    }

    @Test
    @DisplayName("2. Multi-level comparator: elements with equal first key are ordered by second key")
    void testMultiLevelComparatorSorting() {
        Product p1 = Product.of("SKU-1", "Zebra Pad", 1000.0);
        Product p2 = Product.of("SKU-2", "Alpha Pad", 1000.0);
        Product p3 = Product.of("SKU-3", "Pro Stand", 2500.0);

        productRepo.addAll(List.of(p1, p2, p3));

        List<Product> sorted = productRepo.sortByPriceDescThenTitleAsc();

        assertAll("Multi-level sort: price DESC, then title ASC",
                () -> assertEquals("SKU-3", sorted.get(0).getSku()),
                () -> assertEquals("SKU-2", sorted.get(1).getSku()),
                () -> assertEquals("SKU-1", sorted.get(2).getSku())
        );
    }

    @Test
    @DisplayName("3. findAllSortedByIdentity in ASC and DESC returns mutually reversed lists")
    void testFindAllSortedByIdentityAscAndDesc() {
        productRepo.add(Product.of("SKU-B", "Item B", 100.0));
        productRepo.add(Product.of("SKU-A", "Item A", 200.0));
        productRepo.add(Product.of("SKU-C", "Item C", 300.0));

        List<Product> asc = productRepo.findAllSortedByIdentity(SortOrder.ASC);
        List<Product> desc = productRepo.findAllSortedByIdentity(SortOrder.DESC);

        List<Product> reversedAsc = new ArrayList<>(asc);
        Collections.reverse(reversedAsc);

        assertEquals(reversedAsc, desc);
    }

    @Test
    @DisplayName("4. Two objects with fractional price difference less than 1.0 are sorted properly")
    void testFractionalPriceDifferenceSorting() {
        Product cheaper = Product.of("SKU-10", "Adapter A", 100.15);
        Product moreExpensive = Product.of("SKU-20", "Adapter B", 100.85);

        productRepo.add(moreExpensive);
        productRepo.add(cheaper);

        List<Product> sorted = productRepo.findAllSorted(Comparator.naturalOrder());

        assertEquals(cheaper, sorted.get(0));
        assertEquals(moreExpensive, sorted.get(1));
        assertTrue(cheaper.compareTo(moreExpensive) < 0);
    }

    @Test
    @DisplayName("5. TreeSet with comparator inconsistent with equals treats distinct objects with same price as duplicates")
    void testTreeSetWithInconsistentComparator() {
        Product p1 = Product.of("SKU-01", "Item One", 999.0);
        Product p2 = Product.of("SKU-02", "Item Two", 999.0);

        assertNotEquals(p1, p2);

        Set<Product> treeSet = new TreeSet<>(Comparator.comparingDouble(Product::getPrice));
        assertTrue(treeSet.add(p1));
        assertFalse(treeSet.add(p2));
        assertEquals(1, treeSet.size());
    }

    @Test
    @DisplayName("6. Stream search methods filter accurately by text, range, and multiple conditions")
    void testSearchMethodsAccuracy() {
        productRepo.add(Product.of("SKU-1", "Gaming Mouse", 1200.0));
        productRepo.add(Product.of("SKU-2", "Gaming Chair", 9500.0));
        productRepo.add(Product.of("SKU-3", "Office Mouse", 400.0));

        List<Product> byText = productRepo.findByTitleContaining("mouse");
        List<Product> byRange = productRepo.findByPriceRange(1000.0, 5000.0);
        List<Product> byTwoCond = productRepo.findByTitlePrefixAndMaxPrice("Gaming", 5000.0, 5);

        assertAll("Product search methods",
                () -> assertEquals(2, byText.size()),
                () -> assertEquals(1, byRange.size()),
                () -> assertEquals("SKU-1", byRange.get(0).getSku()),
                () -> assertEquals(1, byTwoCond.size()),
                () -> assertEquals("SKU-1", byTwoCond.get(0).getSku())
        );
    }

    @Test
    @DisplayName("7. Single item search returns Optional.empty() when no elements match")
    void testFindSingleReturnsEmptyOptionalWhenNoMatch() {
        productRepo.add(Product.of("SKU-1", "Laptop", 35000.0));

        Optional<Product> result = productRepo.findFirstCheaperThan(100.0);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("8. groupingBy produces expected number of groups and expected group size")
    void testGroupingByStatusAndCount() {
        orderRepo.add(new Order("ORD-1", sampleCustomer, LocalDate.now(), OrderStatus.NEW, 500.0));
        orderRepo.add(new Order("ORD-2", sampleCustomer, LocalDate.now(), OrderStatus.PAID, 1500.0));
        orderRepo.add(new Order("ORD-3", sampleCustomer, LocalDate.now(), OrderStatus.PAID, 2500.0));

        Map<OrderStatus, List<Order>> groups = orderRepo.groupByStatus();
        Map<OrderStatus, Long> counts = orderRepo.countByStatus();

        assertAll("Grouping by status",
                () -> assertEquals(2, groups.size()),
                () -> assertEquals(2, groups.get(OrderStatus.PAID).size()),
                () -> assertEquals(1L, counts.get(OrderStatus.NEW)),
                () -> assertEquals(2L, counts.get(OrderStatus.PAID))
        );
    }

    @Test
    @DisplayName("9. Aggregation on empty repository does not fail and returns 0.0")
    void testAggregationOnEmptyRepository() {
        assertEquals(0.0, productRepo.calculateTotalCatalogValue());
        assertEquals(0.0, orderRepo.calculateTotalRevenue());
    }

    @Test
    @DisplayName("10. Reusing an already consumed Stream throws IllegalStateException")
    void testStreamReuseThrowsException() {
        productRepo.add(Product.of("SKU-1", "Phone", 20000.0));

        Stream<Product> stream = productRepo.stream();
        assertEquals(1L, stream.count());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                stream::count
        );
        assertNotNull(ex.getMessage());
    }
}
