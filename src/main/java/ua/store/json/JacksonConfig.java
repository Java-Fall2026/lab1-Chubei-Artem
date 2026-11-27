package ua.store.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Single shared factory for configuring Jackson ObjectMapper instances across
 * both FileStorage (Lab 6) and REST API JsonSupport (Lab 7).
 */
public final class JacksonConfig {

    private JacksonConfig() {
    }

    public static ObjectMapper createJsonMapper() {
        return configure(JsonMapper.builder().build());
    }

    public static ObjectMapper createYamlMapper() {
        return configure(YAMLMapper.builder().build());
    }

    public static ObjectMapper configure(ObjectMapper mapper) {
        return mapper
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
