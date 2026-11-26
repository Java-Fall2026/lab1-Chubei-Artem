package ua.store;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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

public class Main {

        private static final Logger log = LoggerFactory.getLogger(Main.class);

        public static void main(String[] args) {
                log.info("Запуск демонстраційної програми ЛР6: Файли, серіалізація та валідація");

                // === 1. Reading configuration from app.properties ===
                System.out.println("=== 1. Читання конфігурації з app.properties ===");
                AppConfig config;
                try {
                        config = AppConfig.fromClasspath("app.properties");
                } catch (IOException e) {
                        log.error("Критична помилка завантаження конфігурації: {}", e.getMessage());
                        return;
                }

                Path productsPath = config.getRequiredPath("data.products.file");
                Path ordersPath = config.getRequiredPath("data.orders.file");
                Path brokenPath = config.getRequiredPath("data.broken.file");
                Path updatedProductsPath = config.getRequiredPath("data.output.products.file");

                System.out.println("Шлях до товарів (JSON): " + productsPath);
                System.out.println("Шлях до замовлень (YAML): " + ordersPath);
                System.out.println("Шлях до пошкодженого файлу: " + brokenPath);
                System.out.println("Шлях для збереження оновленого каталогу: " + updatedProductsPath);

                // === 2. Loading JSON and YAML files with validation ===
                System.out.println("\n=== 2. Завантаження файлів даних із валідацією та відбраковкою ===");
                ProductRepository productRepo = new ProductRepository();
                OrderRepository orderRepo = new OrderRepository();

                FileStorage<Product> productStorage = new FileStorage<>(Product.class);
                FileStorage<Order> orderStorage = new FileStorage<>(Order.class);

                DataLoader<Product> productLoader = new DataLoader<>(productStorage,
                                DataLoader.createDefaultValidator());
                DataLoader<Order> orderLoader = new DataLoader<>(orderStorage, DataLoader.createDefaultValidator());

                LoadResult productsResult = productLoader.loadInto(productsPath, productRepo);
                System.out.printf("Файл %s -> прочитано: %d, додано: %d, відкинуто: %d%n",
                                productsPath, productsResult.totalRead(), productsResult.addedCount(),
                                productsResult.rejectedCount());

                LoadResult ordersResult = orderLoader.loadInto(ordersPath, orderRepo);
                System.out.printf("Файл %s -> прочитано: %d, додано: %d, відкинуто: %d%n",
                                ordersPath, ordersResult.totalRead(), ordersResult.addedCount(),
                                ordersResult.rejectedCount());

                // === 3. Printing rejected records line by line ===
                System.out.println("\n=== 3. Список відкинутих записів ===");
                System.out.println("Відкинуті записи товарів (" + productsResult.rejectedCount() + "):");
                productsResult.rejected().forEach(r -> System.out.println("  " + r));

                System.out.println("Відкинуті записи замовлень (" + ordersResult.rejectedCount() + "):");
                ordersResult.rejected().forEach(r -> System.out.println("  " + r));

                // === 4. Attempting to load broken.json ===
                System.out.println("\n=== 4. Спроба завантажити синтаксично пошкоджений файл broken.json ===");
                LoadResult brokenResult = productLoader.loadInto(brokenPath, productRepo);
                System.out.printf("Файл %s -> прочитано: %d, додано: %d, відкинуто: %d%n",
                                brokenPath, brokenResult.totalRead(), brokenResult.addedCount(),
                                brokenResult.rejectedCount());
                brokenResult.rejected().forEach(r -> System.out.println("  " + r));

                // === 5. Modifying repository content and saving to a new file ===
                System.out.println("\n=== 5. Зміна вмісту репозиторію товарів та збереження у новий YAML-файл ===");
                productRepo.removeByIdentity("SKU-104");
                productRepo.findByIdentity("SKU-101").ifPresent(p -> p.setPrice(3499.00));
                productRepo.add(Product.of("SKU-200", "Mechanical Numpad Wireless", 1450.00));

                List<Product> updatedProducts = productRepo.findAll();
                try {
                        productStorage.save(updatedProductsPath, updatedProducts);
                        log.info("Оновлений каталог товарів ({} шт.) успішно збережено у файл '{}'",
                                        updatedProducts.size(), updatedProductsPath);
                        System.out.println("Збережено товарів у " + updatedProductsPath + ": " + updatedProducts.size()
                                        + " шт.");
                } catch (IOException e) {
                        log.error("Не вдалося зберегти оновлений каталог: {}", e.getMessage());
                        return;
                }

                // === 6. Reloading the saved file and verifying consistency ===
                System.out.println("\n=== 6. Повторне завантаження збереженого файлу та звірка вмісту ===");
                ProductRepository reloadedRepo = new ProductRepository();
                LoadResult reloadResult = productLoader.loadInto(updatedProductsPath, reloadedRepo);
                System.out.printf("Повторне завантаження %s -> прочитано: %d, додано: %d, відкинуто: %d%n",
                                updatedProductsPath, reloadResult.totalRead(), reloadResult.addedCount(),
                                reloadResult.rejectedCount());

                boolean contentMatches = productRepo.findAll().equals(reloadedRepo.findAll());
                System.out.println("Вміст репозиторіїв до та після збереження повністю збігається: " + contentMatches);

                log.info("Демонстраційна програма ЛР6 успішно завершила роботу");
        }
}
