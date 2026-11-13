package ua.store.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.model.Order;
import ua.store.model.OrderStatus;

/**
 * Domain repository for Order entities providing custom sorting, Stream API
 * search, grouping, and aggregation operations.
 */
public class OrderRepository extends GenericRepository<Order> {

    private static final Logger log = LoggerFactory.getLogger(OrderRepository.class);

    public OrderRepository() {
        super("orders", Order::getOrderId);
    }

    // === Domain sorting methods ===
    /**
     * Sorts orders by orderDate ascending, then by orderId.
     */
    public List<Order> sortByDateAsc() {
        List<Order> result = findAllSorted(
                Comparator.comparing(Order::getOrderDate)
                        .thenComparing(Order::getOrderId)
        );
        log.info("[orders] Sorted by orderDate ASC, count={}", result.size());
        return result;
    }

    /**
     * Multi-level sort: by status, then by totalAmount descending (reversed).
     */
    public List<Order> sortByStatusThenAmountDesc() {
        Comparator<Order> comparator = Comparator.comparing(Order::getStatus)
                .thenComparing(Comparator.comparingDouble(Order::getTotalAmount).reversed())
                .thenComparing(Order::getOrderId);
        List<Order> result = findAllSorted(comparator);
        log.info("[orders] Multi-level sort (status ASC, totalAmount DESC), count={}", result.size());
        return result;
    }

    // === Stream API search methods ===
    /**
     * 1. Search by exact or partial customer email match (case-insensitive).
     */
    public List<Order> findByCustomerEmailContaining(String emailPart) {
        Objects.requireNonNull(emailPart, "Email search part must not be null");
        String normalized = emailPart.trim().toLowerCase();

        List<Order> result = stream()
                .filter(o -> o.getCustomer().email().toLowerCase().contains(normalized))
                .toList();

        logSearchResult("customer email containing '" + emailPart + "'", result.size());
        return result;
    }

    /**
     * 2. Search by date range [startDate, endDate] inclusive.
     */
    public List<Order> findByDateRange(LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        List<Order> result = stream()
                .filter(o -> !o.getOrderDate().isBefore(startDate) && !o.getOrderDate().isAfter(endDate))
                .sorted(Comparator.comparing(Order::getOrderDate))
                .toList();

        logSearchResult("date range [" + startDate + " .. " + endDate + "]", result.size());
        return result;
    }

    /**
     * 3. Search by two conditions simultaneously: specific OrderStatus and
     * minimum totalAmount.
     */
    public List<Order> findByStatusAndMinAmount(OrderStatus status, double minAmount) {
        Objects.requireNonNull(status, "OrderStatus must not be null");

        List<Order> result = stream()
                .filter(o -> o.getStatus() == status && o.getTotalAmount() >= minAmount)
                .sorted()
                .toList();

        logSearchResult("status=" + status + " and minAmount=" + minAmount, result.size());
        return result;
    }

    /**
     * Extracts distinct customer emails from all orders (demonstrates map,
     * distinct, sorted).
     */
    public List<String> findDistinctCustomerEmails() {
        List<String> emails = stream()
                .map(o -> o.getCustomer().email())
                .distinct()
                .sorted()
                .toList();
        log.info("[orders] Extracted distinct customer emails, count={}", emails.size());
        return emails;
    }

    /**
     * Single object search returning Optional via findFirst.
     */
    public Optional<Order> findFirstByStatus(OrderStatus status) {
        Objects.requireNonNull(status, "OrderStatus must not be null");

        Optional<Order> found = stream()
                .filter(o -> o.getStatus() == status)
                .findFirst();

        if (found.isPresent()) {
            log.info("[orders] Found first order with status {}: {}", status, found.get().getOrderId());
        } else {
            log.warn("[orders] No order found with status {}", status);
        }
        return found;
    }

    // === Grouping and Aggregation ===
    /**
     * 1. Groups orders by their OrderStatus: Map<OrderStatus, List<Order>>.
     */
    public Map<OrderStatus, List<Order>> groupByStatus() {
        Map<OrderStatus, List<Order>> grouped = stream()
                .collect(Collectors.groupingBy(Order::getStatus));
        log.info("[orders] Grouped orders by status into {} groups", grouped.size());
        return grouped;
    }

    /**
     * 2. Counts orders per OrderStatus using downstream collector
     * Collectors.counting().
     */
    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> counts = stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
        log.info("[orders] Counted orders per status across {} statuses", counts.size());
        return counts;
    }

    /**
     * 3. Aggregates total revenue across all orders using mapToDouble().sum().
     * Safe on empty repository (returns 0.0).
     */
    public double calculateTotalRevenue() {
        double sum = stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
        log.info("[orders] Calculated total orders revenue: {}", sum);
        return sum;
    }

    private void logSearchResult(String criteria, int count) {
        if (count > 0) {
            log.info("[orders] Search by [{}] returned {} items", criteria, count);
        } else {
            log.warn("[orders] Search by [{}] returned 0 items (empty result)", criteria);
        }
    }
}
