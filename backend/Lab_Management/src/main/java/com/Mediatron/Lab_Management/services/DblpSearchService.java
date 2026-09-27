package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.Normalizer;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class DblpSearchService {

    private static final String DBLP_PUBLICATION_SEARCH_API = "https://dblp.org/search/publ/api";
    private static final String DBLP_PUBLICATION_SEARCH_HTML = "https://dblp.org/search/publ";
    private static final int MAX_RESULTS_PER_QUERY = 30;

    private final RestTemplate restTemplate;

    public DblpSearchService() {
        this.restTemplate = new RestTemplate();
    }

    public DblpSearchResult searchWorks(Set<String> candidateQueries, String firstName, String lastName) {
        LinkedHashMap<String, DblpWork> uniqueWorks = new LinkedHashMap<>();
        int rawCount = 0;
        List<String> errors = new ArrayList<>();

        for (String query : buildDblpQueries(candidateQueries, firstName, lastName)) {
            try {
                DblpSearchResult apiResult = searchWorksUsingApi(query, firstName, lastName);
                rawCount += apiResult.rawCount();
                mergeWorks(uniqueWorks, apiResult.works());
            } catch (RuntimeException ex) {
                errors.add("DBLP API query '" + query + "' failed: " + ex.getMessage());
            }
        }

        if (uniqueWorks.isEmpty()) {
            for (String query : buildDblpQueries(candidateQueries, firstName, lastName)) {
                try {
                    DblpSearchResult htmlResult = searchWorksUsingHtml(query, firstName, lastName);
                    rawCount += htmlResult.rawCount();
                    mergeWorks(uniqueWorks, htmlResult.works());
                } catch (RuntimeException ex) {
                    errors.add("DBLP HTML query '" + query + "' failed: " + ex.getMessage());
                }
            }
        }

        if (uniqueWorks.isEmpty() && !errors.isEmpty()) {
            throw new IllegalStateException(String.join(" | ", errors));
        }

        return new DblpSearchResult(new ArrayList<>(uniqueWorks.values()), rawCount);
    }

    private DblpSearchResult searchWorksUsingApi(String query, String firstName, String lastName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(DBLP_PUBLICATION_SEARCH_API)
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("h", MAX_RESULTS_PER_QUERY)
                .toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set(HttpHeaders.USER_AGENT, browserUserAgent());

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            JsonNode root = response.getBody();

            JsonNode hits = root == null
                    ? null
                    : root.path("result").path("hits").path("hit");

            if (hits == null || !hits.isArray()) {
                return new DblpSearchResult(List.of(), 0);
            }

            List<DblpWork> works = new ArrayList<>();

            for (JsonNode hit : iterableOf(hits)) {
                JsonNode info = hit.path("info");
                DblpWork work = mapApiHit(info, firstName, lastName);
                if (work != null) {
                    works.add(work);
                }
            }

            return new DblpSearchResult(works, hits.size());
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to query DBLP API: " + ex.getMessage(), ex);
        }
    }

    private DblpSearchResult searchWorksUsingHtml(String query, String firstName, String lastName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(DBLP_PUBLICATION_SEARCH_HTML)
                .queryParam("q", query)
                .toUriString();

        try {
            Document document = Jsoup.connect(url)
                    .userAgent(browserUserAgent())
                    .referrer("https://dblp.org/")
                    .timeout(15000)
                    .get();

            List<DblpWork> works = new ArrayList<>();

            for (Element entry : document.select("li.entry")) {
                DblpWork work = mapHtmlEntry(entry, firstName, lastName);
                if (work != null) {
                    works.add(work);
                }
            }

            return new DblpSearchResult(works, works.size());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to scrape DBLP HTML search: " + ex.getMessage(), ex);
        }
    }

    private DblpWork mapApiHit(JsonNode info, String firstName, String lastName) {
        if (info == null || info.isMissingNode()) {
            return null;
        }

        String title = cleanTitle(info.path("title").asText(null));
        if (title == null) {
            return null;
        }

        String authorsText = extractApiAuthors(info.path("authors").path("author"));
        if (!hasStrongAuthorMatch(authorsText, firstName, lastName)) {
            return null;
        }

        Integer year = parseYear(info.path("year").asText(null));
        if (!isReasonableYear(year)) {
            return null;
        }

        String doi = normalizeDoi(info.path("doi").asText(null));
        String ee = firstNonBlank(info.path("ee").asText(null), info.path("url").asText(null));
        String dblpUrl = cleanText(info.path("url").asText(null));
        String externalUrl = firstNonBlank(resolveUsefulExternalUrl(ee), resolveUsefulExternalUrl(dblpUrl));

        String venue = cleanText(info.path("venue").asText(null));
        String typeRaw = cleanText(info.path("type").asText(null));
        PublicationType type = mapDblpPublicationType(typeRaw, venue, title);

        String indexation = Stream.of("DBLP", venue, typeRaw)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" - "));

        String contextText = Stream.of(title, authorsText, venue, typeRaw, doi, ee, dblpUrl)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        return new DblpWork(
                title,
                authorsText,
                year,
                doi,
                externalUrl,
                null,
                indexation.isBlank() ? "DBLP" : indexation,
                type,
                contextText
        );
    }

    private DblpWork mapHtmlEntry(Element entry, String firstName, String lastName) {
        String title = cleanTitle(entry.selectFirst("span.title") != null
                ? entry.selectFirst("span.title").text()
                : null);
        if (title == null) {
            return null;
        }

        String authorsText = entry.select("span[itemprop=author] span[itemprop=name], cite a span[itemprop=name], cite a")
                .stream()
                .map(Element::text)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));

        if (authorsText.isBlank() || !hasStrongAuthorMatch(authorsText, firstName, lastName)) {
            return null;
        }

        Integer year = parseYear(entry.select("span[itemprop=datePublished]").text());
        if (!isReasonableYear(year)) {
            year = parseYear(entry.text());
        }

        String doi = null;
        String externalUrl = null;

        for (Element link : entry.select("a[href]")) {
            String href = cleanText(link.attr("abs:href"));
            if (href == null) {
                continue;
            }

            String normalizedHref = href.toLowerCase(Locale.ROOT);
            if (doi == null && normalizedHref.contains("doi.org/")) {
                doi = normalizeDoi(href);
            }
            if (externalUrl == null && !normalizedHref.contains("dblp.org")) {
                externalUrl = href;
            }
        }

        String venue = cleanText(entry.select("span[itemprop=isPartOf], span[itemprop=name]").text());
        PublicationType type = mapDblpPublicationType(entry.className(), venue, title);

        String indexation = Stream.of("DBLP", venue)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" - "));

        String contextText = Stream.of(title, authorsText, venue, doi, externalUrl, entry.text())
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        return new DblpWork(
                title,
                authorsText,
                year,
                doi,
                externalUrl,
                null,
                indexation.isBlank() ? "DBLP" : indexation,
                type,
                contextText
        );
    }

    private Set<String> buildDblpQueries(Set<String> candidateQueries, String firstName, String lastName) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        String cleanedFirst = cleanText(firstName);
        String cleanedLast = cleanText(lastName);

        if (cleanedFirst != null && cleanedLast != null) {
            addQuery(queries, cleanedFirst + " " + cleanedLast);
            addQuery(queries, cleanedLast + " " + cleanedFirst);
            addQuery(queries, cleanedFirst.charAt(0) + " " + cleanedLast);
            addQuery(queries, cleanedLast + " " + cleanedFirst.charAt(0));
        }

        if (candidateQueries != null) {
            candidateQueries.stream()
                    .map(this::cleanText)
                    .filter(Objects::nonNull)
                    .filter(query -> query.length() <= 80)
                    .filter(query -> normalize(query).contains(normalize(cleanedLast)))
                    .limit(8)
                    .forEach(query -> addQuery(queries, query));
        }

        return queries;
    }

    private void mergeWorks(LinkedHashMap<String, DblpWork> uniqueWorks, List<DblpWork> works) {
        for (DblpWork work : works) {
            String key = buildKey(work);
            uniqueWorks.putIfAbsent(key, work);
        }
    }

    private String buildKey(DblpWork work) {
        String doi = normalizeDoi(work.doi());
        if (doi != null) {
            return "doi|" + doi;
        }

        String title = normalize(work.title());
        if (work.publicationYear() != null) {
            return "titleYear|" + title + "|" + work.publicationYear();
        }
        return "title|" + title;
    }

    private boolean hasStrongAuthorMatch(String authorsText, String firstName, String lastName) {
        String normalizedAuthors = normalize(authorsText);
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);

        if (normalizedAuthors.isBlank() || normalizedFirst.isBlank() || normalizedLast.isBlank()) {
            return false;
        }

        String natural = normalizedFirst + " " + normalizedLast;
        String reversed = normalizedLast + " " + normalizedFirst;

        return normalizedAuthors.contains(natural)
                || normalizedAuthors.contains(reversed)
                || (normalizedAuthors.contains(normalizedFirst) && normalizedAuthors.contains(normalizedLast));
    }

    private String extractApiAuthors(JsonNode authorNode) {
        if (authorNode == null || authorNode.isMissingNode() || authorNode.isNull()) {
            return null;
        }

        if (authorNode.isTextual()) {
            return cleanText(authorNode.asText(null));
        }

        if (authorNode.isArray()) {
            return StreamSupport.stream(authorNode.spliterator(), false)
                    .map(node -> node.isTextual() ? node.asText(null) : node.path("text").asText(null))
                    .map(this::cleanText)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.joining(", "));
        }

        return cleanText(authorNode.path("text").asText(null));
    }

    private PublicationType mapDblpPublicationType(String rawType, String venue, String title) {
        String normalized = normalize(Stream.of(rawType, venue, title)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        if (normalized.contains("journal") || normalized.contains("article")) {
            return PublicationType.JOURNAL;
        }

        if (normalized.contains("book") || normalized.contains("chapter")) {
            return PublicationType.BOOK_CHAPTER;
        }

        if (normalized.contains("workshop")) {
            return PublicationType.WORKSHOP;
        }

        if (normalized.contains("conference")
                || normalized.contains("conf")
                || normalized.contains("proceedings")
                || normalized.contains("aina")
                || normalized.contains("iwcmc")) {
            return PublicationType.CONFERENCE_C;
        }

        return PublicationType.OTHER;
    }

    private boolean isReasonableYear(Integer year) {
        if (year == null) {
            return true;
        }
        int currentYear = Year.now().getValue();
        return year >= 1980 && year <= currentYear + 1;
    }

    private Integer parseYear(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(19|20)\\d{2}")
                .matcher(cleaned);

        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveUsefulExternalUrl(String url) {
        String cleaned = cleanText(url);
        if (cleaned == null) {
            return null;
        }

        if (cleaned.startsWith("https://doi.org/") || cleaned.startsWith("http://doi.org/")) {
            return cleaned;
        }

        if (cleaned.contains("doi.org/") || cleaned.contains("link.springer.com") || cleaned.contains("ieeexplore.ieee.org")) {
            return cleaned;
        }

        return cleaned;
    }

    private String normalizeDoi(String doi) {
        String cleaned = cleanText(doi);
        if (cleaned == null) {
            return null;
        }

        cleaned = cleaned
                .replace("https://doi.org/", "")
                .replace("http://doi.org/", "")
                .replace("https://dx.doi.org/", "")
                .replace("http://dx.doi.org/", "")
                .trim();

        if (!cleaned.startsWith("10.")) {
            int doiIndex = cleaned.indexOf("10.");
            if (doiIndex >= 0) {
                cleaned = cleaned.substring(doiIndex);
            }
        }

        return cleaned.isBlank() ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String cleanTitle(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }

        cleaned = Jsoup.parse(cleaned).text()
                .replaceAll("\\s+", " ")
                .replaceAll("\\.$", "")
                .trim();

        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? null : cleaned;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            String cleaned = cleanText(value);
            if (cleaned != null) {
                return cleaned;
            }
        }

        return null;
    }

    private void addQuery(Set<String> queries, String value) {
        String cleaned = cleanText(value);
        if (cleaned != null) {
            queries.add(cleaned.replace("\"", ""));
        }
    }

    private Iterable<JsonNode> iterableOf(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }
        return arrayNode::elements;
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

    private String browserUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36";
    }

    public record DblpSearchResult(
            List<DblpWork> works,
            int rawCount
    ) {
    }

    public record DblpWork(
            String title,
            String authorsText,
            Integer publicationYear,
            String doi,
            String externalUrl,
            String pdfUrl,
            String indexation,
            PublicationType type,
            String contextText
    ) {
    }
}
