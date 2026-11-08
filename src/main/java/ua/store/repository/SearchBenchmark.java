package ua.store.repository;

import java.util.List;
import java.util.Optional;

import ua.store.model.Product;

/**
 * Benchmark comparing lookup performance between Map (GenericRepository) and
 * linear search in List on 100,000 items.
 */
public class SearchBenchmark {

    private static final int ITEM_COUNT = 100_000;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURE_RUNS = 7;

    public static void main(String[] args) {
        System.out.println("=== Starting Search Benchmark (100,000 items) ===");

        GenericRepository<Product> repository = new GenericRepository<>("ProductBenchmarkRepo", Product::getSku);

        System.out.println("Generating 100,000 Product items...");
        for (int i = 0; i < ITEM_COUNT; i++) {
            String sku = String.format("SKU-%06d", i);
            repository.add(Product.of(sku, "Product " + i, 10.0 + (i % 100)));
        }

        // Take snapshot list once before measurements to avoid measuring list copying
        List<Product> itemsList = repository.findAll();

        String firstIdentity = "SKU-000000";
        String lastIdentity = String.format("SKU-%06d", ITEM_COUNT - 1);
        String missingIdentity = "SKU-MISSING-999999";

        // Warmup runs
        for (int i = 0; i < WARMUP_RUNS; i++) {
            repository.findByIdentity(firstIdentity);
            repository.findByIdentity(lastIdentity);
            repository.findByIdentity(missingIdentity);

            linearSearch(itemsList, firstIdentity, Product::getSku);
            linearSearch(itemsList, lastIdentity, Product::getSku);
            linearSearch(itemsList, missingIdentity, Product::getSku);
        }

        // Benchmark runs
        long mapFirstTime = measureMapSearch(repository, firstIdentity);
        long listFirstTime = measureListSearch(itemsList, firstIdentity);

        long mapLastTime = measureMapSearch(repository, lastIdentity);
        long listLastTime = measureListSearch(itemsList, lastIdentity);

        long mapMissingTime = measureMapSearch(repository, missingIdentity);
        long listMissingTime = measureListSearch(itemsList, missingIdentity);

        System.out.println("\n--- Результати замірів часу пошуку (наносекунди / нс) ---");
        System.out.printf("%-20s | %-18s | %-18s | %-12s%n", "Сценарій", "Map (Repository)", "List (Linear)", "Прискорення");
        System.out.println("---------------------------------------------------------------------------");
        printRow("Перший елемент", mapFirstTime, listFirstTime);
        printRow("Останній елемент", mapLastTime, listLastTime);
        printRow("Відсутній елемент", mapMissingTime, listMissingTime);
    }

    private static <E> Optional<E> linearSearch(
            List<E> items, String identity, IdentityExtractor<E> extractor) {
        for (E item : items) {
            if (identity.equals(extractor.identityOf(item))) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static long measureMapSearch(GenericRepository<Product> repo, String identity) {
        long minTime = Long.MAX_VALUE;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            repo.findByIdentity(identity);
            long elapsed = System.nanoTime() - start;
            minTime = Math.min(minTime, elapsed);
        }
        return minTime;
    }

    private static long measureListSearch(List<Product> items, String identity) {
        long minTime = Long.MAX_VALUE;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            linearSearch(items, identity, Product::getSku);
            long elapsed = System.nanoTime() - start;
            minTime = Math.min(minTime, elapsed);
        }
        return minTime;
    }

    private static void printRow(String scenario, long mapNs, long listNs) {
        double ratio = (double) listNs / Math.max(1, mapNs);
        System.out.printf("%-20s | %15d нс | %15d нс | %10.1fx%n", scenario, mapNs, listNs, ratio);
    }
}
