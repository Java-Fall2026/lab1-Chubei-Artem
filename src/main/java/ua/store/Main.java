package ua.store;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.exception.DuplicateIdentityException;
import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.GenericRepository;
import ua.store.util.CustomerLoader;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Запуск головної демонстраційної програми ЛР4");

        // === 1. Три репозиторії різних типів та пошук за identity ===
        System.out.println("=== 1. Три репозиторії різних типів (record, ✎-клас, звичайний клас) ===");

        GenericRepository<Customer> customerRepo = new GenericRepository<>("CustomerRepo", Customer::email);
        GenericRepository<Order> orderRepo = new GenericRepository<>("OrderRepo", Order::getOrderId);
        GenericRepository<Product> productRepo = new GenericRepository<>("ProductRepo", Product::getSku);

        Customer customer1 = new Customer("artem.chubei@gmail.com", "Артем", "+380501112233");
        Customer customer2 = new Customer("olena.petrenko@ukr.net", "Олена", "+380672223344");
        customerRepo.add(customer1);
        customerRepo.add(customer2);

        Order order1 = new Order("ORD-2026-001", customer1, LocalDate.now(), OrderStatus.NEW, 12500.0);
        orderRepo.add(order1);

        Product product1 = Product.of("PROD-1092", "Gaming Laptop", 45000.0);
        productRepo.add(product1);

        System.out.println("Пошук наявного Product: " + productRepo.findByIdentity("PROD-1092").orElse(null));
        System.out.println("Пошук відсутнього Product: " + productRepo.findByIdentity("PROD-UNKNOWN").orElse(null));

        // === 2. Наповнення репозиторію з CSV-файла ЛР3 ===
        System.out.println("\n=== 2. Наповнення репозиторію клієнтів із CSV-файла ===");
        GenericRepository<Customer> fileCustomerRepo = new GenericRepository<>("CsvCustomerRepo", Customer::email);
        Path csvPath = Paths.get("data", "customers.csv");
        List<Customer> loadedFromCsv = CustomerLoader.loadCustomers(csvPath);

        System.out.println("Спроба додати всі зчитані записи у репозиторій (перевірка дублікатів з файлу):");
        for (Customer c : loadedFromCsv) {
            try {
                fileCustomerRepo.add(c);
            } catch (DuplicateIdentityException e) {
                // GenericRepository вже залогував WARN, тут лише вивід у консоль
                System.out.println("-> Дублікат відхилено: " + e.getMessage());
            }
        }
        System.out.println("Успішно збережено у репозиторії унікальних клієнтів: " + fileCustomerRepo.size());

        // === 3. Робота з дублікатами: add проти addIfAbsent ===
        System.out.println("\n=== 3. Контракт дублікатів: add проти addIfAbsent ===");
        Customer duplicateCustomer = new Customer("artem.chubei@gmail.com", "Артем Інший", "+380509999999");

        System.out.println("Спроба 1: додавання дубліката через add(item):");
        try {
            customerRepo.add(duplicateCustomer);
        } catch (DuplicateIdentityException e) {
            // GenericRepository вже залогував WARN, тут лише вивід у консоль
            System.out.println("-> Перехоплено очікуваний виняток: " + e.getMessage());
        }

        System.out.println("Спроба 2: додавання дубліката через addIfAbsent(item):");
        boolean wasAdded = customerRepo.addIfAbsent(duplicateCustomer);
        System.out.println("-> addIfAbsent повернув: " + wasAdded + " (розмір репозиторію: " + customerRepo.size() + ")");

        // === 4. Видалення наявного та відсутнього елемента ===
        System.out.println("\n=== 4. Видалення елементів за identity ===");
        boolean removedExisting = customerRepo.removeByIdentity("olena.petrenko@ukr.net");
        System.out.println("Видалення наявного клієнта: " + removedExisting + " (залишилось: " + customerRepo.size() + ")");

        boolean removedMissing = customerRepo.removeByIdentity("ghost@store.ua");
        System.out.println("Видалення відсутнього клієнта: " + removedMissing);

        // === 5. Логування подій усіх трьох рівнів ===
        System.out.println("\n=== 5. Логування подій трьох рівнів SLF4J ===");
        log.info("Рівень INFO: репозиторій успішно обробив усі запити");
        log.warn("Рівень WARN: попередження про спробу дублювання або відсутній елемент зафіксовано коректно");
        log.error("Рівень ERROR: демонстрація логування помилки без переривання програми");

        log.info("Демонстраційна програма ЛР4 успішно завершила роботу");
    }
}
