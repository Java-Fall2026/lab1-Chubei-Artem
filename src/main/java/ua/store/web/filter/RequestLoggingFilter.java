package ua.store.web.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP request/response access logging filter choosing SLF4J log level by
 * resulting status code (2xx -> INFO, 4xx -> WARN, 5xx -> ERROR).
 */
public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        long startNanos = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            int status = resp.getStatus();
            String fullPath = buildFullPath(req);

            if (status >= 500) {
                log.error("{} {} -> {} ({} ms)", req.getMethod(), fullPath, status, durationMs);
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms)", req.getMethod(), fullPath, status, durationMs);
            } else {
                log.info("{} {} -> {} ({} ms)", req.getMethod(), fullPath, status, durationMs);
            }
        }
    }

    private String buildFullPath(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String query = req.getQueryString();
        return (query == null || query.isBlank()) ? uri : uri + "?" + query;
    }
}
