package ua.store.web.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.store.exception.BadRequestException;
import ua.store.exception.MethodNotAllowedException;
import ua.store.exception.NotFoundException;
import ua.store.json.JsonSupport;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.repository.OrderRepository;

/**
 * REST servlet exposing OrderRepository under /api/orders/*.
 */
public class OrderServlet extends HttpServlet {

    private static final Set<String> ALLOWED_PARAMS = Set.of(
            "email", "status", "fromDate", "toDate", "sort", "order", "limit", "offset"
    );

    private static final Map<String, Comparator<Order>> SORT_COMPARATORS = Map.of(
            "orderid", Comparator.comparing(Order::getOrderId),
            "orderdate", Comparator.comparing(Order::getOrderDate).thenComparing(Order::getOrderId),
            "totalamount", Comparator.comparingDouble(Order::getTotalAmount).thenComparing(Order::getOrderId),
            "status", Comparator.comparing(Order::getStatus).thenComparing(Order::getOrderId)
    );

    private final OrderRepository repository;
    private final JsonSupport json;

    public OrderServlet(OrderRepository repository, JsonSupport json) {
        this.repository = Objects.requireNonNull(repository, "OrderRepository must not be null");
        this.json = Objects.requireNonNull(json, "JsonSupport must not be null");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> segments = RequestSupport.parseSegments(req);
        if (segments.isEmpty()) {
            handleGetCollection(req, resp);
            return;
        }
        String segment = segments.get(0);
        if ("stats".equals(segment)) {
            handleGetStats(resp);
            return;
        }
        Order order = repository.findByIdentity(segment)
                .orElseThrow(() -> new NotFoundException("Замовлення з номером '" + segment + "' не знайдено"));
        json.write(resp, 200, order);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> segments = RequestSupport.parseSegments(req);
        if (!segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод POST не підтримується для ресурсу '" + req.getRequestURI() + "'");
        }
        Order created = json.readValid(req, Order.class);
        repository.add(created);

        String orderId = repository.identityOf(created);
        String encodedId = URLEncoder.encode(orderId, StandardCharsets.UTF_8);
        resp.setHeader("Location", req.getContextPath() + req.getServletPath() + "/" + encodedId);
        json.write(resp, 201, created);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> segments = RequestSupport.parseSegments(req);
        if (segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод PUT не підтримується для колекції '" + req.getRequestURI() + "'");
        }
        String pathId = segments.get(0);
        Order updated = json.readValid(req, Order.class);
        String bodyId = repository.identityOf(updated);

        if (!pathId.equals(bodyId)) {
            throw new BadRequestException(
                    "Номер замовлення у тілі ('" + bodyId + "') не збігається з номером у шляху ('" + pathId + "')"
            );
        }
        if (!repository.replace(updated)) {
            throw new NotFoundException("Замовлення з номером '" + pathId + "' не знайдено");
        }
        json.write(resp, 200, updated);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        List<String> segments = RequestSupport.parseSegments(req);
        if (segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод DELETE не підтримується для колекції '" + req.getRequestURI() + "'");
        }
        String pathId = segments.get(0);
        if (!repository.removeByIdentity(pathId)) {
            throw new NotFoundException("Замовлення з номером '" + pathId + "' не знайдено");
        }
        resp.setStatus(204);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws jakarta.servlet.ServletException, IOException {
        String method = req.getMethod();
        if (!Set.of("GET", "POST", "PUT", "DELETE").contains(method)) {
            throw new MethodNotAllowedException("HTTP-метод '" + method + "' не підтримується");
        }
        super.service(req, resp);
    }

    private void handleGetCollection(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        for (String paramName : req.getParameterMap().keySet()) {
            if (!ALLOWED_PARAMS.contains(paramName)) {
                throw new BadRequestException("Невідомий параметр запиту: '" + paramName + "'");
            }
        }

        String emailParam = req.getParameter("email");
        String statusParam = req.getParameter("status");
        String fromDateParam = req.getParameter("fromDate");
        String toDateParam = req.getParameter("toDate");
        String sortParam = req.getParameter("sort");
        String orderParam = req.getParameter("order");
        String limitParam = req.getParameter("limit");
        String offsetParam = req.getParameter("offset");

        Comparator<Order> comparator = RequestSupport.resolveComparator(
                sortParam,
                orderParam,
                SORT_COMPARATORS,
                Comparator.comparing(Order::getOrderId)
        );
        List<Order> result = repository.findAllSorted(comparator);

        if (emailParam != null) {
            if (emailParam.isBlank()) {
                throw new BadRequestException("Параметр 'email' не може бути порожнім");
            }
            Set<String> matchingIds = repository.findByCustomerEmailContaining(emailParam).stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toSet());
            result = result.stream()
                    .filter(o -> matchingIds.contains(o.getOrderId()))
                    .toList();
        }

        if (statusParam != null) {
            OrderStatus status = parseOrderStatus(statusParam);
            result = result.stream()
                    .filter(o -> o.getStatus() == status)
                    .toList();
        }

        if (fromDateParam != null || toDateParam != null) {
            LocalDate from = (fromDateParam != null) ? parseDateParam(fromDateParam, "fromDate") : LocalDate.MIN;
            LocalDate to = (toDateParam != null) ? parseDateParam(toDateParam, "toDate") : LocalDate.MAX;
            if (from.isAfter(to)) {
                throw new BadRequestException("Дата початку 'fromDate' не може бути пізнішою за 'toDate'");
            }
            Set<String> inDateIds = repository.findByDateRange(from, to).stream()
                    .map(Order::getOrderId)
                    .collect(Collectors.toSet());
            result = result.stream()
                    .filter(o -> inDateIds.contains(o.getOrderId()))
                    .toList();
        }

        List<Order> paged = RequestSupport.paginate(result, limitParam, offsetParam);
        json.write(resp, 200, paged);
    }

    private void handleGetStats(HttpServletResponse resp) throws IOException {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", repository.size());
        stats.put("totalRevenue", repository.calculateTotalRevenue());
        stats.put("countByStatus", repository.countByStatus());

        json.write(resp, 200, stats);
    }

    private OrderStatus parseOrderStatus(String raw) {
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некоректний статус замовлення: '" + raw + "'");
        }
    }

    private LocalDate parseDateParam(String raw, String paramName) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Параметр '" + paramName + "' має бути у форматі YYYY-MM-DD, отримано: '" + raw + "'");
        }
    }
}
