package ua.store;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.OrderRepository;
import ua.store.repository.ProductRepository;
import ua.store.repository.SortOrder;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Запуск демонстраційної програми ЛР5: Comparable, Comparator та Stream API");

        // === 1. Populate domain repositories ===
        System.out.println("=== 1. Наповнення доменних репозиторіїв ProductRepository та OrderRepository ===");
        ProductRepository productRepo = new ProductRepository();
        productRepo.add(Product.of("SKU-105", "Gaming Mouse", 1500.50));
        productRepo.add(Product.of("SKU-101", "Mechanical Keyboard", 3200.00));
        productRepo.add(Product.of("SKU-103", "Gaming Monitor", 12500.00));
        productRepo.add(Product.of("SKU-102", "USB-C Cable", 299.99));
        productRepo.add(Product.of("SKU-104", "Wireless Headset", 3200.00)); // Same price as SKU-101

        Customer c1 = new Customer("artem.chubei@gmail.com", "Артем", "+380501112233");
        Customer c2 = new Customer("olena.petrenko@ukr.net", "Олена", "+380672223344");

        OrderRepository orderRepo = new OrderRepository();
        orderRepo.add(new Order("ORD-003", c1, LocalDate.of(2026, 10, 15), OrderStatus.NEW, 4700.50));
        orderRepo.add(new Order("ORD-001", c2, LocalDate.of(2026, 10, 10), OrderStatus.PAID, 12500.00));
        orderRepo.add(new Order("ORD-004", c1, LocalDate.of(2026, 10, 20), OrderStatus.PAID, 1500.50));
        orderRepo.add(new Order("ORD-002", c2, LocalDate.of(2026, 10, 12), OrderStatus.DELIVERED, 3200.00));

        System.out.println("Завантажено товарів: " + productRepo.size() + ", замовлень: " + orderRepo.size());

        // === 2. Sorting ===
        System.out.println("\n=== 2. Сортування за природним порядком (Comparable) та доменними критеріями ===");
        List<Product> naturalProducts = new ArrayList<>(productRepo.findAll());
        Collections.sort(naturalProducts);
        System.out.println("Товари за природним порядком (ціна ASC, потім SKU):");
        naturalProducts.forEach(p -> System.out.println("  " + p.getSku() + " | " + p.getTitle() + " | " + p.getPrice() + " грн"));

        System.out.println("Багаторівневе сортування товарів (ціна DESC, потім назва ASC):");
        productRepo.sortByPriceDescThenTitleAsc()
                .forEach(p -> System.out.println("  " + p.getPrice() + " грн | " + p.getTitle() + " (" + p.getSku() + ")"));

        System.out.println("Багаторівневе сортування замовлень (статус ASC, потім сума DESC):");
        orderRepo.sortByStatusThenAmountDesc()
                .forEach(o -> System.out.println("  " + o.getStatus() + " | " + o.getTotalAmount() + " грн | " + o.getOrderId()));

        // === 3. findAllSortedByIdentity in both directions ===
        System.out.println("\n=== 3. Сортування за identity (ASC та DESC) ===");
        System.out.println("Товари за SKU (ASC): " + productRepo.findAllSortedByIdentity(SortOrder.ASC).stream().map(Product::getSku).toList());
        System.out.println("Товари за SKU (DESC): " + productRepo.findAllSortedByIdentity(SortOrder.DESC).stream().map(Product::getSku).toList());

        // === 4. TreeSet behavior with a custom comparator not aligned with equals ===
        System.out.println("\n=== 4. Демонстрація TreeSet із компаратором, не узгодженим з equals ===");
        Set<Product> priceOnlyTreeSet = new TreeSet<>(Comparator.comparingDouble(Product::getPrice));
        Product pKeyboard = Product.of("SKU-101", "Mechanical Keyboard", 3200.00);
        Product pHeadset = Product.of("SKU-104", "Wireless Headset", 3200.00);
        priceOnlyTreeSet.add(pKeyboard);
        priceOnlyTreeSet.add(pHeadset);
        System.out.println("Додано 2 різні об'єкти (equals=" + pKeyboard.equals(pHeadset) + ") з однаковою ціною 3200.00 грн.");
        System.out.println("Кількість елементів у TreeSet: " + priceOnlyTreeSet.size() + " -> залишився лише: " + priceOnlyTreeSet.iterator().next().getTitle());

        // === 5. Stream API search methods ===
        System.out.println("\n=== 5. Пошук через Stream API ===");
        System.out.println("Товари зі словом 'Gaming': " + productRepo.findByTitleContaining("gaming").size());
        System.out.println("Товари в діапазоні [1000 .. 4000] грн: " + productRepo.findByPriceRange(1000.0, 4000.0).size());
        System.out.println("Товари за префіксом 'Gaming' і ціною <= 15000 (limit 1): "
                + productRepo.findByTitlePrefixAndMaxPrice("Gaming", 15000.0, 1).size());
        System.out.println("Унікальні назви товарів (map + distinct + sorted): " + productRepo.findDistinctTitles());

        System.out.println("Замовлення клієнта 'artem': " + orderRepo.findByCustomerEmailContaining("artem").size());
        System.out.println("Замовлення за період [2026-10-11 .. 2026-10-18]: "
                + orderRepo.findByDateRange(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 18)).size());
        System.out.println("Замовлення зі статусом PAID та сумою >= 5000 грн: "
                + orderRepo.findByStatusAndMinAmount(OrderStatus.PAID, 5000.0).size());

        // === 6. Optional (without isPresent + get) ===
        System.out.println("\n=== 6. Робота з Optional (без isPresent + get) ===");
        String foundCheapTitle = productRepo.findFirstCheaperThan(500.0)
                .map(Product::getTitle)
                .orElse("Не знайдено");
        System.out.println("Перший товар дешевше 500 грн (через map + orElse): " + foundCheapTitle);

        String missingProductTitle = productRepo.findFirstCheaperThan(50.0)
                .map(Product::getTitle)
                .orElseGet(() -> "Товарів дешевше 50 грн у каталозі немає");
        System.out.println("Пошук товару дешевше 50 грн (через map + orElseGet): " + missingProductTitle);

        orderRepo.findFirstByStatus(OrderStatus.CANCELLED)
                .ifPresentOrElse(
                        o -> System.out.println("Знайдено скасоване замовлення: " + o.getOrderId()),
                        () -> System.out.println("Скасованих замовлень (CANCELLED) не виявлено (через ifPresentOrElse)")
                );

        // === 7. Grouping and Aggregation ===
        System.out.println("\n=== 7. Групування (groupingBy) та агрегація (reduce / sum) ===");
        Map<String, List<Product>> byPriceTier = productRepo.groupByPriceCategory();
        byPriceTier.forEach((tier, list) -> System.out.println("  Категорія " + tier + ": " + list.size() + " шт."));
        System.out.println("Загальна вартість каталогу товарів (reduce): " + productRepo.calculateTotalCatalogValue() + " грн");

        Map<OrderStatus, Long> countsByStatus = orderRepo.countByStatus();
        System.out.println("Кількість замовлень за статусами (groupingBy + counting): " + countsByStatus);
        System.out.println("Загальна виручка за всіма замовленнями (sum): " + orderRepo.calculateTotalRevenue() + " грн");

        // === 8. Stream<T> ===
        System.out.println("\n=== 8. Демонстрація одноразовості вже спожитого Stream<T> ===");
        Stream<Product> productStream = productRepo.stream();
        long firstCount = productStream.count();
        System.out.println("Перший прохід стріму успішний, елементів: " + firstCount);
        try {
            long secondCount = productStream.count();
            System.out.println("Другий прохід: " + secondCount);
        } catch (IllegalStateException e) {
            log.error("Перехоплено очікуваний виняток при повторному читанні стріму: {}", e.getMessage());
            System.out.println("-> Помилка повторного читання стріму перехоплена: " + e.getMessage());
        }

        log.info("Демонстраційна програма ЛР5 успішно завершила роботу");
    }
}
