package ua.store.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.store.exception.BadRequestException;
import ua.store.exception.UnsupportedMediaTypeException;
import ua.store.web.error.ApiError.FieldViolation;

/**
 * Helper handling JSON request deserialization, Jakarta Bean Validation, and
 * JSON response serialization with UTF-8 encoding.
 */
public class JsonSupport {

    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private final ObjectMapper mapper;
    private final Validator validator;

    public JsonSupport(ObjectMapper mapper, Validator validator) {
        this.mapper = Objects.requireNonNull(mapper, "ObjectMapper must not be null");
        this.validator = Objects.requireNonNull(validator, "Validator must not be null");
    }

    /**
     * Validates Content-Type, parses request JSON body into the target type,
     * and runs Jakarta Validation collecting all constraint violations.
     */
    public <T> T readValid(HttpServletRequest request, Class<T> type) throws IOException {
        Objects.requireNonNull(request, "HttpServletRequest must not be null");
        Objects.requireNonNull(type, "Target class type must not be null");

        validateJsonContentType(request.getContentType());

        byte[] rawBytes = request.getInputStream().readAllBytes();
        if (rawBytes.length == 0) {
            throw new BadRequestException("Тіло запиту не може бути порожнім");
        }

        T entity;
        try {
            entity = mapper.readValue(rawBytes, type);
        } catch (JacksonException e) {
            throw new BadRequestException("Некоректний формат або структура JSON у тілі запиту", e);
        }

        if (entity == null) {
            throw new BadRequestException("Тіло запиту не може бути null");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            List<FieldViolation> fieldViolations = violations.stream()
                    .sorted(Comparator.comparing((ConstraintViolation<T> v) -> v.getPropertyPath().toString())
                            .thenComparing(ConstraintViolation::getMessage))
                    .map(v -> {
                        String path = v.getPropertyPath().toString();
                        String fieldName = path.isBlank() ? "object" : path;
                        return new FieldViolation(fieldName, v.getMessage());
                    })
                    .toList();
            throw new BadRequestException("Тіло запиту не пройшло валідацію", fieldViolations);
        }

        return entity;
    }

    /**
     * Writes HTTP status, Content-Type application/json; charset=UTF-8, and
     * serialized JSON body.
     */
    public <T> void write(HttpServletResponse response, int status, T body) throws IOException {
        Objects.requireNonNull(response, "HttpServletResponse must not be null");
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(JSON_CONTENT_TYPE);
        mapper.writeValue(response.getWriter(), body);
    }

    private void validateJsonContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new UnsupportedMediaTypeException("Очікується заголовок Content-Type: application/json");
        }
        String mediaType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!"application/json".equals(mediaType)) {
            throw new UnsupportedMediaTypeException(
                    "Непідтримуваний тип вмісту '" + mediaType + "'. Очікується application/json"
            );
        }
    }
}
