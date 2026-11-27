package ua.store.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ua.store.json.JacksonConfig;
import ua.store.model.Customer;
import ua.store.model.Order;
import ua.store.model.OrderStatus;
import ua.store.model.Product;
import ua.store.repository.OrderRepository;
import ua.store.repository.ProductRepository;

class RestApiIntegrationTest {

    private ProductRepository productRepo;
    private OrderRepository orderRepo;
    private ApiServer server;
    private HttpClient client;
    private ObjectMapper mapper;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        productRepo = new ProductRepository();
        orderRepo = new OrderRepository();

        productRepo.add(Product.of("SKU-101", "Mechanical Keyboard", 3200.0));
        productRepo.add(Product.of("SKU-102", "Wireless Mouse", 1500.0));
        productRepo.add(Product.of("SKU-103", "Gaming Monitor 27", 9500.0));

        Customer customer = new Customer("artem@store.ua", "Artem", "+380501112233");
        orderRepo.add(new Order("ORD-001", customer, LocalDate.of(2026, 11, 1), OrderStatus.PAID, 3200.0));
        orderRepo.add(new Order("ORD-002", customer, LocalDate.of(2026, 11, 2), OrderStatus.CANCELLED, 0.0));

        server = new ApiServer(0, productRepo, orderRepo);
        server.start();
        assertTrue(server.port() > 0, "Expected dynamically assigned port > 0");

        baseUrl = "http://localhost:" + server.port();
        client = HttpClient.newHttpClient();
        mapper = JacksonConfig.createJsonMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    @DisplayName("1. GET /api/products returns 200, expected array size, and UTF-8 JSON Content-Type")
    void testGetProductsCollection() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        String contentType = resp.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.contains("application/json"));
        assertTrue(contentType.toLowerCase().contains("charset=utf-8"));

        JsonNode array = mapper.readTree(resp.body());
        assertTrue(array.isArray());
        assertEquals(3, array.size());
    }

    @Test
    @DisplayName("2. GET /api/products/{id} returns 200 when found and 404 ApiError when missing")
    void testGetProductByIdFoundAndNotFound() throws Exception {
        HttpResponse<String> okResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-101")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, okResp.statusCode());
        JsonNode found = mapper.readTree(okResp.body());
        assertEquals("SKU-101", found.get("sku").asText());

        HttpResponse<String> notFoundResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-999")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(404, notFoundResp.statusCode());
        JsonNode err = mapper.readTree(notFoundResp.body());
        assertAll(
                () -> assertEquals(404, err.get("status").asInt()),
                () -> assertEquals("Not Found", err.get("error").asText()),
                () -> assertEquals("/api/products/SKU-999", err.get("path").asText()),
                () -> assertTrue(err.get("violations").isEmpty())
        );
    }

    @Test
    @DisplayName("3. POST /api/products returns 201 with Location header and GET at Location returns created item")
    void testPostValidProductAndFollowLocation() throws Exception {
        String payload = """
                {
                  "sku": "SKU-200",
                  "title": "USB-C Hub",
                  "price": 1250.0
                }
                """;
        HttpRequest postReq = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> postResp = client.send(postReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(201, postResp.statusCode());

        String location = postResp.headers().firstValue("Location").orElse(null);
        assertNotNull(location);
        assertEquals("/api/products/SKU-200", location);

        HttpResponse<String> getResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + location)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, getResp.statusCode());
        JsonNode fetched = mapper.readTree(getResp.body());
        assertEquals("USB-C Hub", fetched.get("title").asText());
        assertEquals(1250.0, fetched.get("price").asDouble());
    }

    @Test
    @DisplayName("4. POST invalid Order returns 400 with all validation violations including custom @ValidOrderStatusAmount")
    void testPostInvalidOrderReturnsAllViolations() throws Exception {
        int initialSize = orderRepo.size();
        // Violates: orderId (@Pattern), customer.email (@Email), and cross-field @ValidOrderStatusAmount (PAID with 0.0)
        String invalidOrderJson = """
                {
                  "orderId": "BAD-ID",
                  "customer": {
                    "email": "not-an-email",
                    "name": "Artem",
                    "phone": "+380501112233"
                  },
                  "orderDate": "2026-11-01",
                  "status": "PAID",
                  "totalAmount": 0.0
                }
                """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidOrderJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
        assertEquals(initialSize, orderRepo.size(), "Invalid entity must not be added to repository");

        JsonNode err = mapper.readTree(resp.body());
        assertEquals(400, err.get("status").asInt());
        assertEquals("Bad Request", err.get("error").asText());
        JsonNode violations = err.get("violations");
        assertTrue(violations.size() >= 2, "Expected multiple validation violations");
        assertTrue(
                violations.toString().contains("orderId") && violations.toString().contains("object"),
                "Expected both field violation (orderId) and custom class-level violation (object), got: " + violations
        );
    }

    @Test
    @DisplayName("5. POST duplicate identity returns 409 Conflict without modifying repository")
    void testPostDuplicateReturns409() throws Exception {
        int initialSize = productRepo.size();
        String duplicateJson = """
                {
                  "sku": "SKU-101",
                  "title": "Duplicate Keyboard",
                  "price": 2999.0
                }
                """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(duplicateJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(409, resp.statusCode());
        assertEquals(initialSize, productRepo.size());

        JsonNode err = mapper.readTree(resp.body());
        assertEquals(409, err.get("status").asInt());
        assertEquals("Conflict", err.get("error").asText());
    }

    @Test
    @DisplayName("6. POST malformed JSON returns 400 Bad Request rather than 500")
    void testPostMalformedJsonReturns400() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{ \"sku\": \"SKU-500\", broken }", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
        JsonNode err = mapper.readTree(resp.body());
        assertEquals(400, err.get("status").asInt());
    }

    @Test
    @DisplayName("7. POST with Content-Type text/plain returns 415 Unsupported Media Type")
    void testPostWrongContentTypeReturns415() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("sku=SKU-500", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode());
        JsonNode err = mapper.readTree(resp.body());
        assertEquals(415, err.get("status").asInt());
        assertEquals("Unsupported Media Type", err.get("error").asText());
    }

    @Test
    @DisplayName("8. PUT updates existing item (200), returns 404 when missing, and 400 on identity mismatch")
    void testPutUpdateNotFoundAndIdentityMismatch() throws Exception {
        // 200 OK
        String updateJson = """
                {
                  "sku": "SKU-101",
                  "title": "Mechanical Keyboard Pro",
                  "price": 3900.0
                }
                """;
        HttpResponse<String> okResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-101"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(updateJson, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, okResp.statusCode());
        assertEquals(3900.0, productRepo.findByIdentity("SKU-101").orElseThrow().getPrice());

        // 404 Not Found
        String missingJson = """
                {
                  "sku": "SKU-777",
                  "title": "Ghost Item",
                  "price": 500.0
                }
                """;
        HttpResponse<String> notFoundResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-777"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(missingJson, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(404, notFoundResp.statusCode());

        // 400 Identity Mismatch
        HttpResponse<String> mismatchResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-102"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(updateJson, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, mismatchResp.statusCode());
    }

    @Test
    @DisplayName("9. DELETE removes item with 204 No Content and subsequent GET returns 404")
    void testDeleteItemThenGetReturns404() throws Exception {
        HttpResponse<Void> delResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-102")).DELETE().build(),
                HttpResponse.BodyHandlers.discarding()
        );
        assertEquals(204, delResp.statusCode());

        HttpResponse<String> getResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-102")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(404, getResp.statusCode());
    }

    @Test
    @DisplayName("10. Unsupported HTTP method returns 405 Method Not Allowed in ApiError format")
    void testUnsupportedMethodReturns405ApiError() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(405, resp.statusCode());
        JsonNode err = mapper.readTree(resp.body());
        assertEquals(405, err.get("status").asInt());
        assertEquals("Method Not Allowed", err.get("error").asText());
    }

    @Test
    @DisplayName("11. Unknown route /api/unknown returns 404 ApiError JSON instead of HTML")
    void testUnknownRouteReturns404ApiError() throws Exception {
        HttpResponse<String> resp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/unknown")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(404, resp.statusCode());
        JsonNode err = mapper.readTree(resp.body());
        assertEquals(404, err.get("status").asInt());
        assertEquals("Not Found", err.get("error").asText());
    }

    @Test
    @DisplayName("12. Sorting order=ASC and order=DESC return reversed lists; unknown sort returns 400")
    void testSortingAscDescAndInvalidSort() throws Exception {
        HttpResponse<String> ascResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products?sort=price&order=ASC")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        HttpResponse<String> descResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products?sort=price&order=DESC")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, ascResp.statusCode());
        assertEquals(200, descResp.statusCode());

        JsonNode ascArr = mapper.readTree(ascResp.body());
        JsonNode descArr = mapper.readTree(descResp.body());
        assertEquals(3, ascArr.size());
        assertEquals(ascArr.get(0).get("sku").asText(), descArr.get(2).get("sku").asText());
        assertEquals(ascArr.get(2).get("sku").asText(), descArr.get(0).get("sku").asText());

        HttpResponse<String> badSortResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products?sort=unknownField")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, badSortResp.statusCode());
    }

    @Test
    @DisplayName("13. Range filtering, pagination, and aggregation endpoints return expected results")
    void testFilteringPaginationAndStats() throws Exception {
        HttpResponse<String> filteredResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products?minPrice=1000&maxPrice=5000&sort=price&order=DESC&limit=1&offset=1")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, filteredResp.statusCode());
        JsonNode paged = mapper.readTree(filteredResp.body());
        assertEquals(1, paged.size());
        assertEquals("SKU-102", paged.get(0).get("sku").asText());

        HttpResponse<String> statsResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/stats")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, statsResp.statusCode());
        JsonNode stats = mapper.readTree(statsResp.body());
        assertEquals(3, stats.get("totalCount").asInt());
        assertEquals(14200.0, stats.get("totalCatalogValue").asDouble());
    }

    @Test
    @DisplayName("14. Cyrillic text survives POST and GET round-trip in UTF-8 without corruption")
    void testCyrillicRoundTripUtf8() throws Exception {
        String ukrainianTitle = "Механічна клавіатура з українською розкладкою";
        String jsonBody = """
                {
                  "sku": "SKU-300",
                  "title": "%s",
                  "price": 4200.0
                }
                """.formatted(ukrainianTitle);

        HttpResponse<String> postResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(201, postResp.statusCode());

        HttpResponse<String> getResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/products/SKU-300")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, getResp.statusCode());
        JsonNode fetched = mapper.readTree(getResp.body());
        assertEquals(ukrainianTitle, fetched.get("title").asText());
    }

    @Test
    @DisplayName("15. Concurrent POST requests with identical identity yield strictly one 201 and rest 409")
    void testConcurrentPostsWithSameIdentity() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        String payload = """
                {
                  "sku": "SKU-900",
                  "title": "Concurrent Race Product",
                  "price": 1999.0
                }
                """;

        try {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> {
                    startGate.await();
                    HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/products"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                            .build();
                    return client.send(req, HttpResponse.BodyHandlers.discarding()).statusCode();
                });
            }

            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(pool.submit(task));
            }
            startGate.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }

            long createdCount = statuses.stream().filter(s -> s == 201).count();
            long conflictCount = statuses.stream().filter(s -> s == 409).count();

            assertAll(
                    () -> assertEquals(1L, createdCount, "Exactly one concurrent POST must succeed with 201"),
                    () -> assertEquals(threads - 1L, conflictCount, "All other concurrent POSTs must fail with 409"),
                    () -> assertFalse(productRepo.findByIdentity("SKU-900").isEmpty())
            );
        } finally {
            pool.shutdownNow();
        }
    }
}
