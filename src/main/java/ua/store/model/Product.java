package ua.store.model;

import java.util.Comparator;
import java.util.Objects;

import ua.common.BaseEntity;
import ua.store.util.StoreUtils;

public class Product extends BaseEntity implements Comparable<Product> {

    private static final Comparator<Product> NATURAL_ORDER = Comparator
            .comparingDouble(Product::getPrice)
            .thenComparing(Product::getSku);

    private final String sku;
    private final String title;
    private double price; // ✎

    private Product(String sku, String title, double price) {
        super();
        this.sku = StoreUtils.validateAndNormalizeString(sku, "Product SKU");
        this.title = StoreUtils.validateAndNormalizeString(title, "Product title");
        setPrice(price);
    }

    public static Product of(String sku, String title, double price) {
        return new Product(sku, title, price);
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public double getPrice() {
        return price;
    }

    public final void setPrice(double price) {
        StoreUtils.validatePrice(price);
        this.price = price;
    }

    /**
     * Natural order: by price ascending, then by unique SKU to stay consistent
     * with equals.
     */
    @Override
    public int compareTo(Product other) {
        Objects.requireNonNull(other, "Cannot compare Product with null");
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "Product{"
                + "sku='" + sku + '\''
                + ", title='" + title + '\''
                + ", price=" + price
                + ", createdAt=" + createdAt
                + '}';
    }
}