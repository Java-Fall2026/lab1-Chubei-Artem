package ua.store.model;

import java.util.Comparator;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ua.common.BaseEntity;

@JsonIgnoreProperties({"createdAt"})
public class Product extends BaseEntity implements Comparable<Product> {

    private static final Comparator<Product> NATURAL_ORDER = Comparator
            .comparingDouble(Product::getPrice)
            .thenComparing(Product::getSku);

    @NotBlank(message = "Артикул товару (SKU) не може бути порожнім")
    @Pattern(regexp = "^SKU-\\d{3,}$", message = "Артикул має відповідати формату SKU-XXX (мінімум 3 цифри)")
    private final String sku;

    @NotBlank(message = "Назва товару не може бути порожньою")
    @Size(min = 2, max = 100, message = "Назва товару повинна містити від 2 до 100 символів")
    private final String title;

    @Positive(message = "Ціна товару має бути суворо більшою за 0")
    private double price;

    @JsonCreator
    public Product(
            @JsonProperty("sku") String sku,
            @JsonProperty("title") String title,
            @JsonProperty("price") double price) {
        super();
        this.sku = (sku != null) ? sku.trim() : null;
        this.title = (title != null) ? title.trim() : null;
        this.price = price;
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
