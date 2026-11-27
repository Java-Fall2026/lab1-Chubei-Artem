package ua.store.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads application configuration from app.properties and resolves file paths.
 */
public final class AppConfig {

    private final Properties properties;

    public AppConfig(Properties properties) {
        this.properties = Objects.requireNonNull(properties, "Properties must not be null");
    }

    /**
     * Loads properties from the specified classpath resource.
     */
    public static AppConfig fromClasspath(String resourceName) throws IOException {
        Objects.requireNonNull(resourceName, "Resource name must not be null");
        Properties props = new Properties();
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Configuration resource not found on classpath: " + resourceName);
            }
            props.load(is);
        }
        return new AppConfig(props);
    }

    /**
     * Retrieves a required string property or throws an exception containing
     * the missing key name.
     */
    public String getRequiredString(String key) {
        Objects.requireNonNull(key, "Property key must not be null");
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing or blank required configuration key: '" + key + "'");
        }
        return value.trim();
    }

    /**
     * Retrieves a required file Path for the given configuration key.
     */
    public Path getRequiredPath(String key) {
        return Path.of(getRequiredString(key));
    }

    public int getRequiredInt(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required configuration key: " + key);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Configuration key '" + key + "' must be an integer, got: " + value, e);
        }
    }
}
