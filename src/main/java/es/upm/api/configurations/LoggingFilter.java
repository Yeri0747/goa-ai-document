package es.upm.api.configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Profile({"dev"})
public class LoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LogManager.getLogger(LoggingFilter.class);


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.debug(">>>>> {} {}", request.getMethod(), request.getRequestURI());
        Map<String, String> headerMap = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(name -> name, request::getHeader));
        log.debug("     Headers:{}", headerMap);
        Map<String, String> parameterMap = Collections.list(request.getParameterNames()).stream()
                .collect(Collectors.toMap(name -> name, request::getParameter));
        log.debug("     Parameters:{}", parameterMap);
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            log.error("Error during filter processing", e);
            throw e;
        }
        byte[] requestArray = wrappedRequest.getContentAsByteArray();
        String requestBody = new String(requestArray, wrappedRequest.getCharacterEncoding());
        log.debug("     Body (request): {}", requestBody);
        byte[] responseArray = wrappedResponse.getContentAsByteArray();
        String responseBody = new String(responseArray, response.getCharacterEncoding());
        log.debug("     Body (response): {}", responseBody);
        wrappedResponse.copyBodyToResponse();
    }
}

