package ua.store.web;

import java.util.EnumSet;
import java.util.Objects;

import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import ua.store.exception.NotFoundException;
import ua.store.json.JacksonConfig;
import ua.store.json.JsonSupport;
import ua.store.repository.OrderRepository;
import ua.store.repository.ProductRepository;
import ua.store.web.filter.ErrorHandlingFilter;
import ua.store.web.filter.RequestLoggingFilter;
import ua.store.web.servlet.OrderServlet;
import ua.store.web.servlet.ProductServlet;

/**
 * Embedded Jetty 12 server exposing ProductRepository and OrderRepository as
 * JSON REST endpoints.
 */
public class ApiServer implements AutoCloseable {

    private final Server server;
    private final ServerConnector connector;
    private final ValidatorFactory validatorFactory;

    public ApiServer(int port, ProductRepository products, OrderRepository orders) {
        Objects.requireNonNull(products, "ProductRepository must not be null");
        Objects.requireNonNull(orders, "OrderRepository must not be null");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Server port must be between 0 and 65535, got: " + port);
        }

        this.server = new Server();
        this.connector = new ServerConnector(this.server);
        this.connector.setPort(port);
        this.server.addConnector(this.connector);

        this.validatorFactory = Validation.buildDefaultValidatorFactory();
        JsonSupport jsonSupport = new JsonSupport(
                JacksonConfig.createJsonMapper(),
                this.validatorFactory.getValidator()
        );

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        // 1. Outer filter: logs every request with final status and duration
        context.addFilter(
                new FilterHolder(new RequestLoggingFilter()),
                "/*",
                EnumSet.of(DispatcherType.REQUEST)
        );
        // 2. Inner filter: converts all exceptions into ApiError JSON responses
        context.addFilter(
                new FilterHolder(new ErrorHandlingFilter(jsonSupport)),
                "/*",
                EnumSet.of(DispatcherType.REQUEST)
        );

        // Domain servlets
        context.addServlet(new ServletHolder(new ProductServlet(products, jsonSupport)), "/api/products/*");
        context.addServlet(new ServletHolder(new OrderServlet(orders, jsonSupport)), "/api/orders/*");

        // Fallback servlet ensuring unknown paths (e.g. /api/unknown) return ApiError 404 instead of Jetty HTML
        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) {
                throw new NotFoundException("Маршрут '" + req.getRequestURI() + "' не знайдено");
            }
        }), "/");

        this.server.setHandler(context);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        try {
            server.stop();
        } finally {
            validatorFactory.close();
        }
    }

    public int port() {
        return connector.getLocalPort();
    }

    @Override
    public void close() throws Exception {
        stop();
    }
}
