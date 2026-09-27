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
public class OpenAlexSearchService {

    private static final String OPENALEX_BASE_URL = "https://api.openalex.org";
    private static final int AUTHOR_LIMIT = 10;
    private static final int WORK_LIMIT = 20;

    private final RestTemplate restTemplate;

    @Value("${OPENALEX_MAILTO:}")
    private String openAlexMailto;

    public OpenAlexSearchService() {
        this.restTemplate = new RestTemplate();
    }

    public OpenAlexSearchResult searchWorks(Set<String> candidateQueries, String firstName, String lastName) {
        ArrayNode allMatchingWorks = JsonNodeFactory.instance.arrayNode();
        String matchedAuthorName = null;

        for (String query : candidateQueries) {
            JsonNode authors = searchAuthors(query);
            AuthorMatch bestAuthor = findBestAuthor(authors, firstName, lastName, query);

            if (bestAuthor != null) {
                JsonNode works = fetchWorksByAuthor(bestAuthor.authorId());
                ArrayNode matchingWorks = filterMatchingWorks(works, firstName, lastName);

                for (JsonNode work : matchingWorks) {
                    allMatchingWorks.add(work);
                }
                matchedAuthorName = bestAuthor.authorName();
            }

            JsonNode directWorks = searchWorksByQuery(query);
            ArrayNode directMatchingWorks = filterMatchingWorks(directWorks, firstName, lastName);

            for (JsonNode work : directMatchingWorks) {
                allMatchingWorks.add(work);
            }
        }

        if (allMatchingWorks.isEmpty()) {
            return null;
        }

        return new OpenAlexSearchResult("aggregated", allMatchingWorks, matchedAuthorName);
    }

    private JsonNode searchAuthors(String query) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(OPENALEX_BASE_URL + "/authors")
                    .queryParam("search", query)
                    .queryParam("per_page", AUTHOR_LIMIT)
                    .queryParam("page", 1);

            if (openAlexMailto != null && !openAlexMailto.isBlank()) {
                builder.queryParam("mailto", openAlexMailto.trim());
            }

            String url = builder.toUriString();
            JsonNode root = getJson(url, "search authors on OpenAlex");
            return root != null ? root.path("results") : JsonNodeFactory.instance.arrayNode();
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 429) {
                throw new IllegalStateException("OpenAlex rate limit reached (429 Too Many Requests). Wait a bit and retry.");
            }
            throw new IllegalStateException(
                    "Failed to search authors on OpenAlex: HTTP " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to search authors on OpenAlex: " + ex.getMessage(), ex);
        }
    }

    private JsonNode fetchWorksByAuthor(String authorId) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(OPENALEX_BASE_URL + "/works")
                    .queryParam("filter", "authorships.author.id:" + authorId)
                    .queryParam("per_page", WORK_LIMIT)
                    .queryParam("page", 1);

            if (openAlexMailto != null && !openAlexMailto.isBlank()) {
                builder.queryParam("mailto", openAlexMailto.trim());
            }

            String url = builder.toUriString();
            JsonNode root = getJson(url, "fetch works by author on OpenAlex");
            return root != null ? root.path("results") : JsonNodeFactory.instance.arrayNode();
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 429) {
                throw new IllegalStateException("OpenAlex rate limit reached (429 Too Many Requests). Wait a bit and retry.");
            }
            throw new IllegalStateException(
                    "Failed to fetch works by author on OpenAlex: HTTP " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch works by author on OpenAlex: " + ex.getMessage(), ex);
        }
    }

    private JsonNode searchWorksByQuery(String query) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(OPENALEX_BASE_URL + "/works")
                    .queryParam("search", query)
                    .queryParam("per_page", WORK_LIMIT)
                    .queryParam("page", 1);

            if (openAlexMailto != null && !openAlexMailto.isBlank()) {
                builder.queryParam("mailto", openAlexMailto.trim());
            }

            String url = builder.toUriString();
            JsonNode root = getJson(url, "search works on OpenAlex");
            return root != null ? root.path("results") : JsonNodeFactory.instance.arrayNode();
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 429) {
                throw new IllegalStateException("OpenAlex rate limit reached (429 Too Many Requests). Wait a bit and retry.");
            }
            throw new IllegalStateException(
                    "Failed to search works on OpenAlex: HTTP " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to search works on OpenAlex: " + ex.getMessage(), ex);
        }
    }

    private JsonNode getJson(String url, String actionDescription) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            String userAgent = (openAlexMailto != null && !openAlexMailto.isBlank())
                    ? "MediatronBot/1.0 (mailto:" + openAlexMailto.trim() + ")"
                    : "MediatronBot/1.0";
            headers.set("User-Agent", userAgent);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Failed to " + actionDescription + ": HTTP " + ex.getRawStatusCode() + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to " + actionDescription + ": " + ex.getMessage(), ex);
        }
    }

    private AuthorMatch findBestAuthor(JsonNode authorsNode, String firstName, String lastName, String queryUsed) {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return null;
        }

        AuthorMatch bestMatch = null;
        int bestScore = Integer.MIN_VALUE;

        for (JsonNode authorNode : authorsNode) {
            String authorId = cleanText(authorNode.path("id").asText(null));
            String authorName = cleanText(authorNode.path("display_name").asText(null));

            if (authorId == null || authorName == null) {
                continue;
            }

            if (!isStrongAuthorMatch(firstName, lastName, authorName, queryUsed)) {
                continue;
            }

            int score = scoreAuthorName(firstName, lastName, authorName, queryUsed);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = new AuthorMatch(authorId, queryUsed, authorName);
            }
        }

        return bestMatch;
    }

    private ArrayNode filterMatchingWorks(JsonNode worksNode, String firstName, String lastName) {
        ArrayNode matchingWorks = JsonNodeFactory.instance.arrayNode();

        if (worksNode == null || !worksNode.isArray() || worksNode.isEmpty()) {
            return matchingWorks;
        }

        for (JsonNode workNode : worksNode) {
            if (workHasStrongAuthorMatch(workNode.path("authorships"), firstName, lastName)) {
                matchingWorks.add(workNode);
            }
        }

        return matchingWorks;
    }

    private boolean workHasStrongAuthorMatch(JsonNode authorshipsNode, String firstName, String lastName) {
        if (authorshipsNode == null || !authorshipsNode.isArray() || authorshipsNode.isEmpty()) {
            return false;
        }

        return StreamSupport.stream(authorshipsNode.spliterator(), false)
                .map(authorship -> cleanText(authorship.path("author").path("display_name").asText(null)))
                .filter(name -> name != null && !name.isBlank())
                .anyMatch(authorName -> isStrongPaperAuthorMatch(firstName, lastName, authorName));
    }

    private boolean isStrongAuthorMatch(String firstName, String lastName, String authorName, String queryUsed) {
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);
        String normalizedAuthor = normalize(authorName);
        String normalizedQuery = normalize(queryUsed);

        boolean exactNatural = normalizedAuthor.equals(normalizedFirst + " " + normalizedLast);
        boolean exactReversed = normalizedAuthor.equals(normalizedLast + " " + normalizedFirst);
        boolean exactQuery = !normalizedQuery.isBlank() && normalizedAuthor.equals(normalizedQuery);
        boolean containsBothNames = !normalizedFirst.isBlank()
                && !normalizedLast.isBlank()
                && normalizedAuthor.contains(normalizedFirst)
                && normalizedAuthor.contains(normalizedLast);

        return exactNatural || exactReversed || exactQuery || containsBothNames;
    }

    private boolean isStrongPaperAuthorMatch(String firstName, String lastName, String authorName) {
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

    private int scoreAuthorName(String firstName, String lastName, String authorName, String queryUsed) {
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);
        String normalizedAuthor = normalize(authorName);
        String normalizedQuery = normalize(queryUsed);

        int score = 0;

        if (normalizedAuthor.equals(normalizedFirst + " " + normalizedLast)) {
            score += 200;
        }

        if (normalizedAuthor.equals(normalizedLast + " " + normalizedFirst)) {
            score += 200;
        }

        if (!normalizedQuery.isBlank() && normalizedAuthor.equals(normalizedQuery)) {
            score += 180;
        }

        if (!normalizedFirst.isBlank() && !normalizedLast.isBlank()
                && normalizedAuthor.contains(normalizedFirst)
                && normalizedAuthor.contains(normalizedLast)) {
            score += 120;
        }

        if (normalizedAuthor.startsWith(normalizedLast + " ")) {
            score += 15;
        }

        if (normalizedAuthor.startsWith(normalizedFirst + " ")) {
            score += 10;
        }

        return score;
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

    private record AuthorMatch(String authorId, String queryUsed, String authorName) {
    }

    public record OpenAlexSearchResult(String queryUsed, JsonNode works, String authorName) {
    }
}
