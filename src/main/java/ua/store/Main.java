package ua.store;

import java.time.LocalDate;

import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.util.StoreUtils;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== 1. Створення record Customer і звернення до компонентів ===");
        Customer customer1 = new Customer("  Artem.CHUBEI@Gmail.COM  ", "Артем", "+380501112233");
        // Звернення до компонентів без слова 'get'
        System.out.println("email(): " + customer1.email());
        System.out.println("name():  " + customer1.name());
        System.out.println("phone(): " + customer1.phone());

        System.out.println("\n=== 2. Enum: values(), поля константи та valueOf у try/catch ===");
        System.out.println("Перебір усіх значень OrderStatus.values():");
        for (OrderStatus status : OrderStatus.values()) {
            System.out.println(" - " + status.name() + " | Опис: \"" + status.getDescription() 
                    + "\" | Можна скасувати: " + status.isCancellable());
        }

        try {
            System.out.println("\nСпроба викликати OrderStatus.valueOf(\"INVALID_STATUS\"):");
            OrderStatus.valueOf("INVALID_STATUS");
        } catch (IllegalArgumentException e) {
            System.out.println("  -> Перехоплено очікуваний виняток: " + e.getMessage());
        }

        System.out.println("\n=== 3. Робота двох switch-виразів у StoreUtils ===");
        Product laptop = Product.of("PROD-101", "Lenovo ThinkPad", 32000.0);
        Order order = new Order("ORD-0001", customer1, LocalDate.now(), OrderStatus.NEW, 32000.0);

        String priority = StoreUtils.getProcessingPriority(order.getStatus());
        double discount = StoreUtils.calculateOrderDiscountPercent(order.getTotalAmount());
        System.out.println("Switch 1 (пріоритет за статусом " + order.getStatus() + "): " + priority);
        System.out.println("Switch 2 (знижка на чек " + StoreUtils.formatMoney(order.getTotalAmount()) + "): " 
                + (discount * 100) + "%");

        System.out.println("\n=== 4. Record з невалідними даними (компактний конструктор) ===");
        try {
            new Customer("   ", "Іван", "+380");
        } catch (IllegalArgumentException e) {
            System.out.println("  -> Виняток із компактного конструктора record: " + e.getMessage());
        }

        System.out.println("\n=== 5. Порівняння двох однакових record (equals та hashCode) ===");
        Customer customer2 = new Customer("artem.chubei@gmail.com", "Артем", "+380501112233");
        System.out.println("customer1 == customer2: " + (customer1 == customer2) + " (різні екземпляри)");
        System.out.println("customer1.equals(customer2): " + customer1.equals(customer2) + " (згенерований equals)");
        System.out.println("customer1.hashCode() == customer2.hashCode(): " 
                + (customer1.hashCode() == customer2.hashCode()) + " (згенерований hashCode)");

        System.out.println("\n=== 6. Вивід record через згенерований toString() ===");
        System.out.println("customer1: " + customer1);
    }
}