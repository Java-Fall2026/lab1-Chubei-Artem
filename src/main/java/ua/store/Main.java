package ua.store;

import java.time.LocalDate;

import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderItem;
import ua.store.model.Product;
import ua.store.util.StoreUtils;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== 1. Створення через фабрику of(...) та конструктор ===");
        Customer customer1 = Customer.of("  Artem.CHUBEI@Gmail.COM  ", "Артем", "+380501112233");
        Product laptop = Product.of("PROD-101", "Lenovo ThinkPad", 32000.0);
        Order order1 = new Order("ORD-0001", customer1, LocalDate.now(), "new", 32000.0);
        OrderItem item1 = new OrderItem(order1, laptop, 1, 31000.0);

        System.out.println("Створено клієнта: " + customer1);
        System.out.println("Створено товар: " + laptop);
        System.out.println("Створено замовлення: " + order1);
        System.out.println("Створено позицію: " + item1);

        System.out.println("\n=== 2. Демонстрація нормалізації ===");
        System.out.println("Оригінальний email: '  Artem.CHUBEI@Gmail.COM  ' -> " + customer1.getEmail());
        System.out.println("Оригінальний статус: 'new' -> " + order1.getStatus());

        System.out.println("\n=== 3. Демонстрація перехоплення винятків (try/catch) ===");
        testException("Невалідний email (порожній):", () -> Customer.of("   ", "Іван", "+380"));
        testException("Невалідна кількість товару (1500 > 1000):", () -> new OrderItem(order1, laptop, 1500, 31000));
        testException("Невалідний статус замовлення:", () -> order1.setStatus("UNKNOWN_STATUS"));

        System.out.println("\n=== 4. Спроба зіпсувати коректний об'єкт через сеттер ===");
        System.out.println("Поточна ціна товару: " + laptop.getPrice());
        try {
            laptop.setPrice(-500.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Перехоплено виняток при зміні ціни: " + e.getMessage());
        }
        System.out.println("Ціна після невдалої зміни лишилася: " + laptop.getPrice());

        System.out.println("\n=== 5. Порівняння: == vs equals() vs hashCode() ===");
        Customer customer2 = Customer.of("artem.chubei@gmail.com", "Інше Ім'я", "+380999999999");
        Customer customer3 = Customer.of("other@gmail.com", "Артем", "+380501112233");

        System.out.println("customer1 == customer2: " + (customer1 == customer2) + " (різні посилання в пам'яті)");
        System.out.println("customer1.equals(customer2): " + customer1.equals(customer2) + " (однаковий email)");
        System.out.println("customer1.hashCode() == customer2.hashCode(): " + (customer1.hashCode() == customer2.hashCode()));
        System.out.println("customer1.equals(customer3): " + customer1.equals(customer3) + " (різні email)");

        System.out.println("\n=== 6. Виклик обчислюваних методів у StoreUtils ===");
        OrderItem item2 = new OrderItem(order1, laptop, 3, 30000.0);
        System.out.println("Загальна вартість позиції (3 шт * 30000.0): " + StoreUtils.lineTotal(item2));
        System.out.println("Різниця ціни позиції та поточної ціни товару (30000 - 32000): " + StoreUtils.priceDifference(item2));

        System.out.println("\n=== 7. Перевірка обмеження доступу (коментар) ===");
        // Наступні рядки не скомпілюються (помилка компіляції):
        // ua.store.util.ValidationHelper.requireNotBlank("test", "field");
        // Причина: ValidationHelper має доступ package-private, тому невидимий за межами пакета ua.store.util.

        // Customer customerPrivate = new Customer("a@b.com", "Test", "123");
        // Причина: конструктор Customer є private, створення можливе тільки через статичну фабрику of(...).

        // System.out.println(customer1.createdAt);
        // Причина: createdAt є protected в BaseEntity і видимий напряму тільки нащадкам (усередині класів моделі), але не в сторонньому Main.
    }

    private static void testException(String description, Runnable action) {
        try {
            action.run();
            System.out.println("ПОМИЛКА: виняток не спрацював!");
        } catch (IllegalArgumentException e) {
            System.out.println(description + "\n  -> " + e.getMessage());
        }
    }
}