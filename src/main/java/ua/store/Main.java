package ua.store;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.config.AppConfig;
import ua.store.model.Order;
import ua.store.model.Product;
import ua.store.repository.OrderRepository;
import ua.store.repository.ProductRepository;
import ua.store.storage.DataLoader;
import ua.store.storage.FileStorage;
import ua.store.storage.LoadResult;
import ua.store.web.ApiServer;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting Lab 7 REST API application");

        AppConfig config;
        try {
            config = AppConfig.fromClasspath("app.properties");
        } catch (IOException e) {
            log.error("Failed to load app.properties: {}", e.getMessage());
            return;
        }

        int serverPort = config.getRequiredInt("server.port");
        Path productsInputPath = config.getRequiredPath("data.products.file");
        Path ordersInputPath = config.getRequiredPath("data.orders.file");
        Path productsOutputPath = config.getRequiredPath("data.output.products.file");
        Path ordersOutputPath = config.getRequiredPath("data.output.orders.file");

        ProductRepository productRepo = new ProductRepository();
        OrderRepository orderRepo = new OrderRepository();

        System.out.println("=== 1. Завантаження початкових даних із файлів ===");
        try (DataLoader<Product> productLoader = new DataLoader<>(Product.class); DataLoader<Order> orderLoader = new DataLoader<>(Order.class)) {

            LoadResult productsResult = productLoader.loadInto(productsInputPath, productRepo);
            System.out.printf("Товари (%s) -> прочитано: %d, додано: %d, відкинуто: %d%n",
                    productsInputPath, productsResult.totalRead(), productsResult.addedCount(), productsResult.rejectedCount());

            LoadResult ordersResult = orderLoader.loadInto(ordersInputPath, orderRepo);
            System.out.printf("Замовлення (%s) -> прочитано: %d, додано: %d, відкинуто: %d%n",
                    ordersInputPath, ordersResult.totalRead(), ordersResult.addedCount(), ordersResult.rejectedCount());
        }

        ApiServer apiServer = new ApiServer(serverPort, productRepo, orderRepo);
        FileStorage<Product> productStorage = new FileStorage<>(Product.class);
        FileStorage<Order> orderStorage = new FileStorage<>(Order.class);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown signal received: stopping server and persisting repositories");
            try {
                apiServer.stop();
            } catch (Exception e) {
                log.error("Error while stopping ApiServer: {}", e.getMessage(), e);
            }
            try {
                productStorage.save(productsOutputPath, productRepo.findAll());
                orderStorage.save(ordersOutputPath, orderRepo.findAll());
                log.info("Saved {} products to '{}' and {} orders to '{}'",
                        productRepo.size(), productsOutputPath, orderRepo.size(), ordersOutputPath);
            } catch (IOException e) {
                log.error("Failed to persist repositories on shutdown: {}", e.getMessage(), e);
            }
        }, "api-shutdown-hook"));

        System.out.println("\n=== 2. Запуск HTTP REST API сервера ===");
        try {
            apiServer.start();
            String baseUrl = "http://localhost:" + apiServer.port();
            log.info("REST API server started at {}", baseUrl);
            System.out.println("Сервер запущено та доступний за адресою: " + baseUrl);
            System.out.println("Ендпоінти: " + baseUrl + "/api/products та " + baseUrl + "/api/orders");
            System.out.println("Натисніть Ctrl+C для зупинки сервера та збереження даних.");
        } catch (Exception e) {
            log.error("Failed to start ApiServer: {}", e.getMessage(), e);
        }
    }
}
