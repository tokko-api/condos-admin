package com.condos.billing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoardApiClientImpl implements BoardApiClient {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${board.api.base-url}")
    private String boardApiBaseUrl;

    @Override
    public List<UnitRef> listActiveUnitIds(String boardId, String bearerToken) {
        String url = UriComponentsBuilder.fromHttpUrl(boardApiBaseUrl)
                .pathSegment("boards", boardId, "units")
                .queryParam("size", 500)
                .queryParam("includeInactive", false)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);

        try {
            var res = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            JsonNode root = mapper.readTree(res.getBody());
            JsonNode content = root.has("content") ? root.get("content") : root;

            List<UnitRef> units = new ArrayList<>();
            if (content.isArray()) {
                for (JsonNode n : content) {
                    units.add(new UnitRef(n.get("id").asText(), n.get("identifier").asText()));
                }
            }
            return units;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo obtener las unidades de board-api para boardId=" + boardId + ": " + e.getMessage());
        }
    }
}
