package es.upm.api.configurations;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingFilterTest {

    private final LoggingFilter loggingFilter = new LoggingFilter();

    @Test
    void doFilterLogsRequestAndCopiesResponseBody() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/document-ai/documents");
        request.addHeader("X-Test-Header", "value");
        request.setParameter("autoclassify", "true");
        request.setContent("request-body".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = (req, res) -> res.getWriter().write("response-body");

        loggingFilter.doFilter(request, response, filterChain);

        assertThat(response.getContentAsString()).isEqualTo("response-body");
    }

    @Test
    void doFilterRethrowsExceptionFromChain() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/document-ai/documents/doc-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (req, res) -> {
            throw new RuntimeException("filter chain failed");
        };

        assertThrows(RuntimeException.class, () -> loggingFilter.doFilter(request, response, filterChain));
    }
}
