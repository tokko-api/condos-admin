package com.condos.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UserApiClientImpl implements UserApiClient {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${user.api.base-url}")
    private String userApiBaseUrl;

    @Override
    public UserRef getUser(String userId, String bearerToken) {
        String url = UriComponentsBuilder.fromHttpUrl(userApiBaseUrl)
                .pathSegment("users", userId)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);

        try {
            var res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode n = mapper.readTree(res.getBody());
            return new UserRef(
                    n.get("id").asText(),
                    n.hasNonNull("email") ? n.get("email").asText() : null,
                    n.hasNonNull("fullName") ? n.get("fullName").asText() : null);
        } catch (Exception e) {
            return null;
        }
    }
}
