package ua.store.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Generic file storage supporting both JSON and YAML formats with ISO-8601 date
 * serialization.
 *
 * @param <T> entity type stored in files
 */
public class FileStorage<T> {

    private final Class<T> type;
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public FileStorage(Class<T> type) {
        this.type = Objects.requireNonNull(type, "Entity class type must not be null");
        this.jsonMapper = configureMapper(JsonMapper.builder().build());
        this.yamlMapper = configureMapper(YAMLMapper.builder().build());
    }

    private static ObjectMapper configureMapper(ObjectMapper mapper) {
        return mapper
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    private ObjectMapper mapperFor(Path file) {
        DataFormat format = DataFormat.fromPath(file);
        return switch (format) {
            case JSON ->
                jsonMapper;
            case YAML ->
                yamlMapper;
        };
    }

    /**
     * Loads a list of entities from a JSON or YAML file.
     */
    public List<T> load(Path file) throws IOException {
        Objects.requireNonNull(file, "Input file path must not be null");
        ObjectMapper mapper = mapperFor(file);

        if (!Files.exists(file)) {
            throw new IOException("Data file does not exist: " + file);
        }
        if (Files.size(file) == 0L) {
            return Collections.emptyList();
        }

        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, type);
        List<T> items = mapper.readValue(file.toFile(), listType);
        return items != null ? items : Collections.emptyList();
    }

    /**
     * Saves a list of entities to a JSON or YAML file.
     */
    public void save(Path file, List<T> items) throws IOException {
        Objects.requireNonNull(file, "Output file path must not be null");
        Objects.requireNonNull(items, "Items list must not be null");
        ObjectMapper mapper = mapperFor(file);

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapper.writeValue(file.toFile(), items);
    }
}
