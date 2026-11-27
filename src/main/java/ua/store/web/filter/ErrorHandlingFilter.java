package ua.store.web.filter;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ua.store.exception.ApiException;
import ua.store.exception.DuplicateIdentityException;
import ua.store.json.JsonSupport;
import ua.store.web.error.ApiError;

/**
 * Central exception-to-HTTP-response filter converting all thrown exceptions
 * into standardized ApiError JSON responses.
 */
public class ErrorHandlingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingFilter.class);

    private final JsonSupport jsonSupport;

    public ErrorHandlingFilter(JsonSupport jsonSupport) {
        this.jsonSupport = Objects.requireNonNull(jsonSupport, "JsonSupport must not be null");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();

        try {
            chain.doFilter(request, response);
        } catch (ApiException e) {
            ApiError error = new ApiError(
                    e.getStatus(),
                    e.getError(),
                    e.getMessage(),
                    path,
                    e.getViolations()
            );
            jsonSupport.write(resp, e.getStatus(), error);
        } catch (DuplicateIdentityException e) {
            ApiError error = ApiError.of(
                    409,
                    "Conflict",
                    "Сутність із таким ідентифікатором вже існує",
                    path
            );
            jsonSupport.write(resp, 409, error);
        } catch (IllegalArgumentException e) {
            ApiError error = ApiError.of(
                    400,
                    "Bad Request",
                    "Некоректні вхідні дані запиту",
                    path
            );
            jsonSupport.write(resp, 400, error);
        } catch (Exception e) {
            log.error("Unhandled server error while processing {} {}", req.getMethod(), path, e);
            ApiError error = ApiError.of(
                    500,
                    "Internal Server Error",
                    "Внутрішня помилка сервера",
                    path
            );
            jsonSupport.write(resp, 500, error);
        }
    }
}
