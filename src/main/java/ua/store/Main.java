package ua.store;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.util.CustomerLoader;
import ua.store.util.StoreUtils;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Запуск головної демонстраційної програми ЛР3");

        System.out.println("=== 1. Зчитування CSV з коректними та некоректними рядками ===");
        Path csvPath = Paths.get("data", "customers.csv");
        List<Customer> customers = CustomerLoader.loadCustomers(csvPath);

        System.out.println("Успішно завантажено клієнтів: " + customers.size());
        for (Customer c : customers) {
            System.out.println(" -> " + c);
        }

        System.out.println("\n=== 2. Спроба зчитати відсутній файл (програма не падає) ===");
        Path missingPath = Paths.get("data", "missing_customers.csv");
        List<Customer> missingList = CustomerLoader.loadCustomers(missingPath);
        System.out.println("Результат для неіснуючого файлу: " + missingList.size() + " записів");

        System.out.println("\n=== 3. Некоректне значення в сеттері змінюваного класу ===");
        if (!customers.isEmpty()) {
            Customer firstCustomer = customers.getFirst();
            Order order = new Order("ORD-2026-001", firstCustomer, LocalDate.now(), OrderStatus.NEW, 12500.0);
            System.out.println("Створено початкове замовлення: " + order);

            try {
                System.out.println("Спроба встановити від'ємну суму (-500.0) через сеттер:");
                order.setTotalAmount(-500.0);
            } catch (IllegalArgumentException e) {
                log.warn("Перехоплено порушення бізнес-правил у сеттері Order: {}", e.getMessage());
                System.out.println("Перехоплено виняток: " + e.getMessage());
            }
            System.out.println("Сума після спроби псування залишилася: " + StoreUtils.formatMoney(order.getTotalAmount()));
        }

        System.out.println("\n=== 4. Логування подій усіх трьох рівнів ===");
        log.info("Рівень INFO: операція виконана успішно");
        log.warn("Рівень WARN: попередження про пропущений запис або некритичний збій");
        log.error("Рівень ERROR: повідомлення про системну помилку введення-виведення");

        log.info("Демонстраційна програма завершила роботу");
    }
}