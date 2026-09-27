package com.Mediatron.Lab_Management.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.StreamSupport;

@Service
public class CrossRefSearchService {

    private static final String CROSSREF_BASE_URL = "https://api.crossref.org/works";
    private static final int ROWS = 20;

    private final RestTemplate restTemplate;

    @Value("${CROSSREF_MAILTO:}")
    private String crossRefMailto;

    public CrossRefSearchService() {
        this.restTemplate = new RestTemplate();
    }

    public CrossRefSearchResult searchWorks(Set<String> candidateQueries, String firstName, String lastName) {
        ArrayNode allMatchingItems = JsonNodeFactory.instance.arrayNode();

        for (String query : candidateQueries) {
            JsonNode items = searchWorksByQuery(query);
            if (items == null || !items.isArray() || items.isEmpty()) {
                continue;
            }

            for (JsonNode item : items) {
                if (itemHasStrongAuthorMatch(item.path("author"), firstName, lastName)) {
                    allMatchingItems.add(item);
                }
            }
        }

        if (allMatchingItems.isEmpty()) {
            return null;
        }

        return new CrossRefSearchResult("aggregated", allMatchingItems);
    }

    private JsonNode searchWorksByQuery(String query) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(CROSSREF_BASE_URL)
                    .queryParam("query.author", query)
                    .queryParam("rows", ROWS)
                    .queryParam("select", "DOI,title,author,issued,URL,type,container-title");

            if (crossRefMailto != null && !crossRefMailto.isBlank()) {
                builder.queryParam("mailto", crossRefMailto.trim());
            }

            String url = builder.toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String userAgent = (crossRefMailto != null && !crossRefMailto.isBlank())
                    ? "MediatronBot/1.0 (mailto:" + crossRefMailto.trim() + ")"
                    : "MediatronBot/1.0";
            headers.set("User-Agent", userAgent);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            return body != null ? body.path("message").path("items") : JsonNodeFactory.instance.arrayNode();
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 429) {
                throw new IllegalStateException(
                        "CrossRef rate limit reached (429 Too Many Requests). Wait a few minutes and retry."
                );
            }

            throw new IllegalStateException(
                    "Failed to search papers on CrossRef: HTTP " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to search papers on CrossRef: " + ex.getMessage(), ex);
        }
    }

    private boolean itemHasStrongAuthorMatch(JsonNode authorsNode, String firstName, String lastName) {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return false;
        }

        return StreamSupport.stream(authorsNode.spliterator(), false)
                .map(this::buildCrossRefAuthorDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .anyMatch(authorName -> isStrongAuthorMatch(firstName, lastName, authorName));
    }

    private String buildCrossRefAuthorDisplayName(JsonNode authorNode) {
        String given = cleanText(authorNode.path("given").asText(null));
        String family = cleanText(authorNode.path("family").asText(null));

        if (given != null && family != null) {
            return given + " " + family;
        }

        if (family != null) {
            return family;
        }

        return given;
    }

    private boolean isStrongAuthorMatch(String firstName, String lastName, String authorName) {
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);
        String normalizedAuthor = normalize(authorName);

        boolean exactNatural = normalizedAuthor.equals(normalizedFirst + " " + normalizedLast);
        boolean exactReversed = normalizedAuthor.equals(normalizedLast + " " + normalizedFirst);
        boolean containsBothNames = !normalizedFirst.isBlank()
                && !normalizedLast.isBlank()
                && normalizedAuthor.contains(normalizedFirst)
                && normalizedAuthor.contains(normalizedLast);

        return exactNatural || exactReversed || containsBothNames;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record CrossRefSearchResult(String queryUsed, JsonNode items) {
    }
}
