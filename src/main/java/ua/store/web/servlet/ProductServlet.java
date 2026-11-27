package ua.store.web.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
import ua.store.model.Product;
import ua.store.repository.ProductRepository;

/**
 * REST servlet exposing ProductRepository under /api/products/*.
 */
public class ProductServlet extends HttpServlet {

    private static final Set<String> ALLOWED_PARAMS = Set.of(
            "title", "minPrice", "maxPrice", "sort", "order", "limit", "offset"
    );

    private static final Map<String, Comparator<Product>> SORT_COMPARATORS = Map.of(
            "sku", Comparator.comparing(Product::getSku),
            "title", Comparator.comparing(Product::getTitle, String.CASE_INSENSITIVE_ORDER).thenComparing(Product::getSku),
            "price", Comparator.comparingDouble(Product::getPrice).thenComparing(Product::getSku)
    );

    private final ProductRepository repository;
    private final JsonSupport json;

    public ProductServlet(ProductRepository repository, JsonSupport json) {
        this.repository = Objects.requireNonNull(repository, "ProductRepository must not be null");
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
        Product product = repository.findByIdentity(segment)
                .orElseThrow(() -> new NotFoundException("Товар із артикулом '" + segment + "' не знайдено"));
        json.write(resp, 200, product);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> segments = RequestSupport.parseSegments(req);
        if (!segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод POST не підтримується для ресурсу '" + req.getRequestURI() + "'");
        }
        Product created = json.readValid(req, Product.class);
        repository.add(created);

        String sku = repository.identityOf(created);
        String encodedSku = URLEncoder.encode(sku, StandardCharsets.UTF_8);
        resp.setHeader("Location", req.getContextPath() + req.getServletPath() + "/" + encodedSku);
        json.write(resp, 201, created);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<String> segments = RequestSupport.parseSegments(req);
        if (segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод PUT не підтримується для колекції '" + req.getRequestURI() + "'");
        }
        String pathSku = segments.get(0);
        Product updated = json.readValid(req, Product.class);
        String bodySku = repository.identityOf(updated);

        if (!pathSku.equals(bodySku)) {
            throw new BadRequestException(
                    "Артикул у тілі запиту ('" + bodySku + "') не збігається з артикулом у шляху ('" + pathSku + "')"
            );
        }
        if (!repository.replace(updated)) {
            throw new NotFoundException("Товар із артикулом '" + pathSku + "' не знайдено");
        }
        json.write(resp, 200, updated);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        List<String> segments = RequestSupport.parseSegments(req);
        if (segments.isEmpty()) {
            throw new MethodNotAllowedException("Метод DELETE не підтримується для колекції '" + req.getRequestURI() + "'");
        }
        String pathSku = segments.get(0);
        if (!repository.removeByIdentity(pathSku)) {
            throw new NotFoundException("Товар із артикулом '" + pathSku + "' не знайдено");
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

        String titleParam = req.getParameter("title");
        String minPriceParam = req.getParameter("minPrice");
        String maxPriceParam = req.getParameter("maxPrice");
        String sortParam = req.getParameter("sort");
        String orderParam = req.getParameter("order");
        String limitParam = req.getParameter("limit");
        String offsetParam = req.getParameter("offset");

        Comparator<Product> comparator = RequestSupport.resolveComparator(
                sortParam,
                orderParam,
                SORT_COMPARATORS,
                Comparator.comparing(Product::getSku)
        );
        List<Product> result = repository.findAllSorted(comparator);

        if (titleParam != null) {
            if (titleParam.isBlank()) {
                throw new BadRequestException("Параметр 'title' не може бути порожнім");
            }
            Set<String> matchingSkus = repository.findByTitleContaining(titleParam).stream()
                    .map(Product::getSku)
                    .collect(Collectors.toSet());
            result = result.stream()
                    .filter(p -> matchingSkus.contains(p.getSku()))
                    .toList();
        }

        if (minPriceParam != null || maxPriceParam != null) {
            double minPrice = (minPriceParam != null)
                    ? RequestSupport.parseDoubleParam(minPriceParam, "minPrice")
                    : 0.0;
            double maxPrice = (maxPriceParam != null)
                    ? RequestSupport.parseDoubleParam(maxPriceParam, "maxPrice")
                    : Double.MAX_VALUE;
            if (minPrice < 0.0 || maxPrice < 0.0 || minPrice > maxPrice) {
                throw new BadRequestException("Некоректний діапазон цін: minPrice=" + minPrice + ", maxPrice=" + maxPrice);
            }
            Set<String> inRangeSkus = repository.findByPriceRange(minPrice, maxPrice).stream()
                    .map(Product::getSku)
                    .collect(Collectors.toSet());
            result = result.stream()
                    .filter(p -> inRangeSkus.contains(p.getSku()))
                    .toList();
        }

        List<Product> paged = RequestSupport.paginate(result, limitParam, offsetParam);
        json.write(resp, 200, paged);
    }

    private void handleGetStats(HttpServletResponse resp) throws IOException {
        Map<String, Integer> countByTier = new LinkedHashMap<>();
        repository.groupByPriceCategory().forEach((tier, items) -> countByTier.put(tier, items.size()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", repository.size());
        stats.put("totalCatalogValue", repository.calculateTotalCatalogValue());
        stats.put("countByPriceCategory", countByTier);

        json.write(resp, 200, stats);
    }
}
