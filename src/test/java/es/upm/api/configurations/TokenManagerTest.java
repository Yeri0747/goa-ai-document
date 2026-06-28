package es.upm.api.configurations;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenManagerTest {

    private static final String TOKEN_URI = "http://localhost:8080/api/goa-user/oauth2/token";

    @Test
    void getTokenObtainsAccessTokenWhenMissing() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(tokenResponse("fresh-token", "3600"));
        })) {
            TokenManager tokenManager = new TokenManager("client-id", "client-secret", TOKEN_URI);

            assertThat(tokenManager.getToken()).isEqualTo("fresh-token");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getTokenReturnsCachedTokenWhenStillValid() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class)) {
            TokenManager tokenManager = new TokenManager("client-id", "client-secret", TOKEN_URI);
            ReflectionTestUtils.setField(tokenManager, "token", "cached-token");
            ReflectionTestUtils.setField(tokenManager, "expiry", Instant.now().plusSeconds(3600));

            assertThat(tokenManager.getToken()).isEqualTo("cached-token");
            assertThat(mocked.constructed()).isEmpty();
        }
    }

    @Test
    void getTokenRefreshesWhenExpiryIsNear() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(tokenResponse("renewed-token", "3600"));
        })) {
            TokenManager tokenManager = new TokenManager("client-id", "client-secret", TOKEN_URI);
            ReflectionTestUtils.setField(tokenManager, "token", "expired-token");
            ReflectionTestUtils.setField(tokenManager, "expiry", Instant.now().plusSeconds(30));

            assertThat(tokenManager.getToken()).isEqualTo("renewed-token");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void invalidateTokenForcesRefreshOnNextGet() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(tokenResponse("new-token", "3600"));
        })) {
            TokenManager tokenManager = new TokenManager("client-id", "client-secret", TOKEN_URI);
            ReflectionTestUtils.setField(tokenManager, "token", "old-token");
            ReflectionTestUtils.setField(tokenManager, "expiry", Instant.now().plusSeconds(3600));

            tokenManager.invalidateToken();

            assertThat(tokenManager.getToken()).isEqualTo("new-token");
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void getTokenUsesSameInstanceUntilRefreshNeeded() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (mock, context) -> {
            when(mock.postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class)))
                    .thenReturn(tokenResponse("shared-token", "3600"));
        })) {
            TokenManager tokenManager = new TokenManager("client-id", "client-secret", TOKEN_URI);

            assertThat(tokenManager.getToken()).isEqualTo("shared-token");
            assertThat(tokenManager.getToken()).isEqualTo("shared-token");
            assertThat(mocked.constructed()).hasSize(1);
            verify(mocked.constructed().get(0), times(1))
                    .postForEntity(eq(TOKEN_URI), any(HttpEntity.class), eq(Map.class));
        }
    }

    private ResponseEntity<Map> tokenResponse(String accessToken, String expiresIn) {
        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "expires_in", expiresIn
        ));
    }
}
