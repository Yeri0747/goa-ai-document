package es.upm.api.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Component
public class TokenManager {  //TODO utilizar el estandar y casi resuelve esta gestion
    public static final String SCOPE_PROFILE = "profile";
    public static final String ROLE_URL_TOKEN = "url_token";
    private final String apiClientId;
    private final String apiClientSecret;
    private final String tokenUri;

    private String token;
    private Instant expiry;

    public TokenManager(@Value("${spring.security.oauth2.api-client-id}") String apiClientId,
                        @Value("${spring.security.oauth2.api-client-secret}") String apiClientSecret,
                        @Value("${spring.security.oauth2.token-uri}") String tokenUri
    ) {
        this.apiClientId = apiClientId;
        this.apiClientSecret = apiClientSecret;
        this.tokenUri = tokenUri;
        this.token = null;
    }

    private void obtainAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String auth = apiClientId + ":" + apiClientSecret;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);

        MultiValueMap<String, String> credentialsBody = new LinkedMultiValueMap<>();
        credentialsBody.add("grant_type", "client_credentials");
        credentialsBody.add("scope", SCOPE_PROFILE);
        credentialsBody.add("role", ROLE_URL_TOKEN);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(credentialsBody, headers);

        Map<?, ?> responseBody = Objects.requireNonNull(
                new RestTemplate().postForEntity(this.tokenUri, request, Map.class).getBody()
        );
        this.token = responseBody.get("access_token").toString();
        this.expiry = Instant.now().plusSeconds(Long.parseLong(responseBody.get("expires_in").toString()));
    }

    public synchronized void invalidateToken() {
        this.token = null;
        this.expiry = Instant.now();
    }

    public synchronized String getToken() {
        if (token == null || Instant.now().isAfter(expiry.minus(Duration.ofMinutes(1)))) {
            this.obtainAccessToken();
        }
        return token;
    }
}
