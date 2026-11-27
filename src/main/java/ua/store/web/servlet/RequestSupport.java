package ua.store.web.servlet;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import ua.store.exception.BadRequestException;
import ua.store.exception.NotFoundException;
import ua.store.repository.SortOrder;

/**
 * Stateless helper for parsing REST paths, query parameters, sorting, and
 * pagination across domain servlets.
 */
public final class RequestSupport {

    private RequestSupport() {
    }

    /**
     * Parses servlet pathInfo into segments: - null or "/" -> empty list
     * (collection endpoint) - "/SKU-101" -> ["SKU-101"] (item or sub-route
     * endpoint) - "/SKU-101/extra" -> throws 404 NotFoundException
     */
    public static List<String> parseSegments(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return List.of();
        }
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String[] parts = trimmed.split("/", -1);
        if (parts.length > 1) {
            throw new NotFoundException("Ресурс за шляхом '" + req.getRequestURI() + "' не знайдено");
        }
        if (parts[0].isBlank()) {
            throw new BadRequestException("Ідентифікатор у шляху не може бути порожнім");
        }
        return List.of(parts[0]);
    }

    /**
     * Resolves a Comparator from the allowed sort fields map and applies
     * SortOrder.
     */
    public static <T> Comparator<T> resolveComparator(
            String sortParam,
            String orderParam,
            Map<String, Comparator<T>> comparators,
            Comparator<T> defaultComparator) {
        SortOrder order = parseSortOrder(orderParam);
        Comparator<T> base;
        if (sortParam == null || sortParam.isBlank()) {
            base = defaultComparator;
        } else {
            String key = sortParam.trim().toLowerCase(Locale.ROOT);
            base = comparators.get(key);
            if (base == null) {
                throw new BadRequestException(
                        "Невідоме поле сортування '" + sortParam + "'. Доступні: " + comparators.keySet()
                );
            }
        }
        return (order == SortOrder.DESC) ? base.reversed() : base;
    }

    public static SortOrder parseSortOrder(String orderParam) {
        if (orderParam == null || orderParam.isBlank()) {
            return SortOrder.ASC;
        }
        try {
            return SortOrder.valueOf(orderParam.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Некоректний напрямок сортування '" + orderParam + "'. Очікується ASC або DESC"
            );
        }
    }

    public static <T> List<T> paginate(List<T> items, String limitParam, String offsetParam) {
        int offset = parseNonNegativeInt(offsetParam, "offset", 0);
        int limit = parsePositiveInt(limitParam, "limit", Integer.MAX_VALUE);

        if (offset >= items.size()) {
            return List.of();
        }
        int toIndex = (int) Math.min((long) offset + limit, items.size());
        return items.subList(offset, toIndex);
    }

    public static double parseDoubleParam(String raw, String paramName) {
        try {
            double val = Double.parseDouble(raw.trim());
            if (Double.isNaN(val) || Double.isInfinite(val)) {
                throw new BadRequestException("Параметр '" + paramName + "' має бути скінченним числом");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Параметр '" + paramName + "' має бути числом, отримано: '" + raw + "'");
        }
    }

    private static int parseNonNegativeInt(String raw, String paramName, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < 0) {
                throw new BadRequestException("Параметр '" + paramName + "' не може бути від'ємним: " + val);
            }
            return val;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Параметр '" + paramName + "' має бути цілим числом, отримано: '" + raw + "'");
        }
    }

    private static int parsePositiveInt(String raw, String paramName, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val <= 0) {
                throw new BadRequestException("Параметр '" + paramName + "' має бути додатним: " + val);
            }
            return val;
        } catch (NumberFormatException e) {
            throw new BadRequestException("Параметр '" + paramName + "' має бути цілим числом, отримано: '" + raw + "'");
        }
    }
}
