package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.PublicationType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ResearchGateSearchService {

    private static final String RESEARCHGATE_BASE_URL = "https://www.researchgate.net";
    private static final String RESEARCHGATE_PUBLICATION_SEARCH_URL = "https://www.researchgate.net/search/publication";
    private static final String RESEARCHGATE_RESEARCHER_SEARCH_URL = "https://www.researchgate.net/search/researcher";

    private static final int MAX_QUERIES = 12;
    private static final int MAX_PROFILE_PAGES = 4;
    private static final int MAX_PUBLICATION_LINKS_PER_PAGE = 25;

    private static final Pattern DOI_PATTERN = Pattern.compile(
            "10\\.\\d{4,9}/[^\\s\\\"<>]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");

    public ResearchGateSearchResult searchWorks(Set<String> candidateQueries, String firstName, String lastName) {
        LinkedHashMap<String, ResearchGateWork> uniqueWorks = new LinkedHashMap<>();
        LinkedHashSet<String> profileUrls = new LinkedHashSet<>();
        List<String> errors = new ArrayList<>();

        int rawCount = 0;

        List<String> queries = buildResearchGateQueries(candidateQueries, firstName, lastName);

        for (String query : queries) {
            try {
                ResearchGateSearchResult result = searchPublicationPage(query, firstName, lastName);
                rawCount += result.rawCount();
                mergeWorks(uniqueWorks, result.works());
            } catch (RuntimeException ex) {
                errors.add("ResearchGate publication query '" + query + "' failed: " + ex.getMessage());
            }

            try {
                profileUrls.addAll(searchResearcherPages(query, firstName, lastName));
            } catch (RuntimeException ex) {
                errors.add("ResearchGate researcher query '" + query + "' failed: " + ex.getMessage());
            }
        }

        int profilesVisited = 0;

        for (String profileUrl : profileUrls) {
            if (profilesVisited >= MAX_PROFILE_PAGES) {
                break;
            }

            profilesVisited++;

            try {
                ResearchGateSearchResult result = scrapeAuthorPage(profileUrl, firstName, lastName);
                rawCount += result.rawCount();
                mergeWorks(uniqueWorks, result.works());
            } catch (RuntimeException ex) {
                errors.add("ResearchGate author page '" + profileUrl + "' failed: " + ex.getMessage());
            }
        }

        if (uniqueWorks.isEmpty() && !errors.isEmpty()) {
            throw new IllegalStateException(String.join(" | ", errors));
        }

        return new ResearchGateSearchResult(new ArrayList<>(uniqueWorks.values()), rawCount);
    }

    private ResearchGateSearchResult searchPublicationPage(String query, String firstName, String lastName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(RESEARCHGATE_PUBLICATION_SEARCH_URL)
                .queryParam("q", query)
                .toUriString();

        Document document = fetchHtml(url, RESEARCHGATE_BASE_URL + "/");
        List<ResearchGateWork> works = extractPublicationWorks(document, firstName, lastName, query, url);

        return new ResearchGateSearchResult(works, works.size());
    }

    private List<String> searchResearcherPages(String query, String firstName, String lastName) {
        String url = UriComponentsBuilder
                .fromHttpUrl(RESEARCHGATE_RESEARCHER_SEARCH_URL)
                .queryParam("q", query)
                .toUriString();

        Document document = fetchHtml(url, RESEARCHGATE_BASE_URL + "/");

        LinkedHashSet<String> urls = new LinkedHashSet<>();

        for (Element link : document.select("a[href]")) {
            String href = cleanText(link.attr("abs:href"));

            if (href == null || !isResearchGateAuthorUrl(href)) {
                continue;
            }

            String context = surroundingText(link);

            if (!hasStrongAuthorMatch(context, firstName, lastName)
                    && !hasStrongAuthorMatch(link.text(), firstName, lastName)
                    && !authorUrlLooksLikeMember(href, firstName, lastName)) {
                continue;
            }

            urls.add(stripUrlFragment(href));
        }

        return urls.stream()
                .limit(MAX_PROFILE_PAGES)
                .toList();
    }

    private ResearchGateSearchResult scrapeAuthorPage(String authorUrl, String firstName, String lastName) {
        Document document = fetchHtml(authorUrl, RESEARCHGATE_BASE_URL + "/");
        List<ResearchGateWork> works = extractPublicationWorks(document, firstName, lastName, authorUrl, authorUrl);

        return new ResearchGateSearchResult(works, works.size());
    }

    private List<ResearchGateWork> extractPublicationWorks(
            Document document,
            String firstName,
            String lastName,
            String queryOrPage,
            String pageUrl
    ) {
        LinkedHashMap<String, ResearchGateWork> works = new LinkedHashMap<>();

        int inspected = 0;

        for (Element link : document.select("a[href*=/publication/]")) {
            if (inspected >= MAX_PUBLICATION_LINKS_PER_PAGE) {
                break;
            }

            inspected++;

            ResearchGateWork work = mapPublicationLink(link, firstName, lastName, queryOrPage, pageUrl);

            if (work != null) {
                works.putIfAbsent(buildWorkKey(work), work);
            }
        }

        return new ArrayList<>(works.values());
    }

    private ResearchGateWork mapPublicationLink(
            Element link,
            String firstName,
            String lastName,
            String queryOrPage,
            String pageUrl
    ) {
        String href = cleanText(link.attr("abs:href"));

        if (href == null || !href.contains("/publication/")) {
            return null;
        }

        String title = cleanTitle(link.text());

        Element card = nearestUsefulContainer(link);
        String context = cleanText(card != null
                ? card.text()
                : link.parent() != null ? link.parent().text() : link.text());

        if (title == null) {
            title = cleanTitle(extractTitleFromUrl(href));
        }

        if (title == null || !hasStrongAuthorMatch(context, firstName, lastName)) {
            return null;
        }

        Integer year = parseYear(context);
        String doi = normalizeDoi(firstNonBlank(extractDoi(context), extractDoi(href)));
        String authorsText = extractAuthorsText(context, firstName, lastName);
        String publicationKind = extractPublicationKind(context);
        PublicationType type = mapResearchGatePublicationType(publicationKind, context, title);

        String indexation = Stream.of("ResearchGate", publicationKind, extractVenueHint(context))
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" - "));

        String contextText = Stream.of(title, authorsText, publicationKind, doi, href, pageUrl, queryOrPage, context)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));

        return new ResearchGateWork(
                title,
                authorsText,
                year,
                doi,
                stripUrlFragment(href),
                null,
                indexation.isBlank() ? "ResearchGate" : indexation,
                type,
                contextText
        );
    }

    private Document fetchHtml(String url, String referrer) {
        try {
            Document document = Jsoup.connect(url)
                    .userAgent(browserUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9,fr;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .referrer(referrer)
                    .followRedirects(true)
                    .timeout(20000)
                    .get();

            String text = normalize(document.text());

            if (text.contains("please verify that you are a human")
                    || text.contains("access denied")
                    || text.contains("enable cookies")
                    || text.contains("captcha")) {
                throw new IllegalStateException("ResearchGate blocked the scraper request.");
            }

            return document;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }

            throw new IllegalStateException("Failed to scrape ResearchGate: " + ex.getMessage(), ex);
        }
    }

    private List<String> buildResearchGateQueries(Set<String> candidateQueries, String firstName, String lastName) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        String fullName = cleanText(Stream.of(firstName, lastName)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        if (fullName != null) {
            queries.add(fullName);
            queries.add("\"" + fullName + "\"");
            queries.add(fullName + " University of Carthage");
            queries.add(fullName + " MEDIATRON");
            queries.add(fullName + " SUPCOM");
            queries.add(fullName + " Higher School of Communication of Tunis");
            queries.add(fullName + " Higher School of Communications of Tunis");
            queries.add(fullName + " publications");
            queries.add(fullName + " research");
        }

        if (candidateQueries != null) {
            candidateQueries.stream()
                    .map(this::cleanText)
                    .filter(Objects::nonNull)
                    .filter(query -> query.length() >= 4)
                    .filter(query -> normalize(query).contains(normalize(firstName))
                            || normalize(query).contains(normalize(lastName)))
                    .limit(8)
                    .forEach(queries::add);
        }

        return queries.stream()
                .limit(MAX_QUERIES)
                .toList();
    }

    private Element nearestUsefulContainer(Element element) {
        Element current = element;

        for (int depth = 0; depth < 8 && current != null; depth++) {
            String text = current.text();
            String className = current.className();

            if (text != null && text.length() > 80 && text.length() < 5000) {
                String normalizedClass = normalize(className);

                if (normalizedClass.contains("nova")
                        || normalizedClass.contains("item")
                        || normalizedClass.contains("publication")
                        || normalizedClass.contains("result")
                        || normalizedClass.contains("card")) {
                    return current;
                }
            }

            current = current.parent();
        }

        return element.parent();
    }

    private String surroundingText(Element element) {
        Element container = nearestUsefulContainer(element);
        return cleanText(container != null ? container.text() : element.text());
    }

    private boolean isResearchGateAuthorUrl(String href) {
        String normalized = href.toLowerCase(Locale.ROOT);

        return normalized.startsWith(RESEARCHGATE_BASE_URL)
                && (normalized.contains("/profile/") || normalized.contains("/scientific-contributions/"));
    }

    private boolean authorUrlLooksLikeMember(String href, String firstName, String lastName) {
        String normalizedHref = normalize(href);
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);

        return !normalizedFirst.isBlank()
                && !normalizedLast.isBlank()
                && normalizedHref.contains(normalizedFirst)
                && normalizedHref.contains(normalizedLast);
    }

    private boolean hasStrongAuthorMatch(String text, String firstName, String lastName) {
        String normalizedText = normalize(text);
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);

        if (normalizedText.isBlank() || normalizedFirst.isBlank() || normalizedLast.isBlank()) {
            return false;
        }

        String natural = normalizedFirst + " " + normalizedLast;
        String reversed = normalizedLast + " " + normalizedFirst;

        return normalizedText.contains(natural)
                || normalizedText.contains(reversed)
                || (normalizedText.contains(normalizedFirst) && normalizedText.contains(normalizedLast));
    }

    private String extractAuthorsText(String context, String firstName, String lastName) {
        String cleaned = cleanText(context);

        String fallback = cleanText(Stream.of(firstName, lastName)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        if (cleaned == null) {
            return fallback;
        }

        String[] separators = {
                "Abstract",
                "Conference",
                "Article",
                "Chapter",
                "Book",
                "Read more",
                "DOI"
        };

        String candidate = cleaned;

        for (String separator : separators) {
            int index = indexOfIgnoreCase(candidate, separator);

            if (index > 0) {
                candidate = candidate.substring(0, index).trim();
            }
        }

        candidate = candidate
                .replaceAll("(?i)^Publication\\s*", "")
                .replaceAll("(?i)^Chapter\\s*", "")
                .replaceAll("(?i)^Article\\s*", "")
                .replaceAll("(?i)^Conference Paper\\s*", "")
                .trim();

        if (candidate.length() > 350) {
            candidate = candidate.substring(0, 350).trim();
        }

        return candidate.isBlank() ? fallback : candidate;
    }

    private String extractPublicationKind(String context) {
        String normalized = normalize(context);

        if (normalized.contains("conference paper")) {
            return "Conference paper";
        }

        if (normalized.contains("chapter")) {
            return "Chapter";
        }

        if (normalized.contains("article")) {
            return "Article";
        }

        if (normalized.contains("book")) {
            return "Book";
        }

        if (normalized.contains("preprint")) {
            return "Preprint";
        }

        return null;
    }

    private String extractVenueHint(String context) {
        String cleaned = cleanText(context);

        if (cleaned == null) {
            return null;
        }

        List<String> knownVenues = List.of(
                "Advances in Intelligent Systems and Computing",
                "Web, Artificial Intelligence and Network Applications",
                "AINA",
                "IWCMC",
                "Springer",
                "IEEE",
                "ResearchGate"
        );

        String normalizedContext = normalize(cleaned);

        for (String venue : knownVenues) {
            if (normalizedContext.contains(normalize(venue))) {
                return venue;
            }
        }

        return null;
    }

    private PublicationType mapResearchGatePublicationType(String kind, String context, String title) {
        String normalized = normalize(Stream.of(kind, context, title)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        if (normalized.contains("chapter")) {
            return PublicationType.BOOK_CHAPTER;
        }

        if (normalized.contains("conference") || normalized.contains("proceedings") || normalized.contains("workshop")) {
            return PublicationType.WORKSHOP;
        }

        if (normalized.contains("journal") || normalized.contains("article")) {
            return PublicationType.JOURNAL;
        }

        return PublicationType.OTHER;
    }

    private Integer parseYear(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        Matcher matcher = YEAR_PATTERN.matcher(cleaned);
        Integer best = null;
        int currentYear = Year.now().getValue();

        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));

            if (year >= 1980 && year <= currentYear + 1) {
                best = year;
            }
        }

        return best;
    }

    private String extractDoi(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        Matcher matcher = DOI_PATTERN.matcher(cleaned);

        if (matcher.find()) {
            return matcher.group();
        }

        return null;
    }

    private String normalizeDoi(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        cleaned = cleaned
                .replace("https://doi.org/", "")
                .replace("http://doi.org/", "")
                .replace("https://dx.doi.org/", "")
                .replace("http://dx.doi.org/", "")
                .replaceAll("[.,;)]$", "")
                .trim();

        return cleaned.isBlank() ? null : cleaned.toLowerCase(Locale.ROOT);
    }

    private String extractTitleFromUrl(String href) {
        String cleaned = cleanText(href);

        if (cleaned == null) {
            return null;
        }

        int index = cleaned.indexOf("/publication/");

        if (index < 0) {
            return null;
        }

        String tail = cleaned.substring(index + "/publication/".length());

        int slash = tail.indexOf('/');

        if (slash >= 0) {
            tail = tail.substring(slash + 1);
        }

        tail = tail.replace('-', ' ').replace('_', ' ');

        return tail;
    }

    private String cleanTitle(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return null;
        }

        cleaned = cleaned
                .replaceAll("(?i)^\\[?publication\\]?\\s*", "")
                .replaceAll("(?i)^\\[?article\\]?\\s*", "")
                .replaceAll("(?i)^\\[?chapter\\]?\\s*", "")
                .replaceAll("(?i)^\\[?conference paper\\]?\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();

        String normalized = normalize(cleaned);

        if (cleaned.length() < 8
                || normalized.equals("read more")
                || normalized.equals("show abstract")
                || normalized.equals("download citation")) {
            return null;
        }

        return cleaned;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value
                .replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();

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

    private int indexOfIgnoreCase(String text, String search) {
        if (text == null || search == null) {
            return -1;
        }

        return text.toLowerCase(Locale.ROOT).indexOf(search.toLowerCase(Locale.ROOT));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = cleanText(value);

            if (cleaned != null) {
                return cleaned;
            }
        }

        return null;
    }

    private String stripUrlFragment(String url) {
        String cleaned = cleanText(url);

        if (cleaned == null) {
            return null;
        }

        int hash = cleaned.indexOf('#');

        return hash >= 0 ? cleaned.substring(0, hash) : cleaned;
    }

    private String buildWorkKey(ResearchGateWork work) {
        String doi = normalizeDoi(work.doi());

        if (doi != null) {
            return "doi|" + doi;
        }

        String title = normalize(work.title());
        Integer year = work.publicationYear();

        if (!title.isBlank() && year != null) {
            return "titleYear|" + title + "|" + year;
        }

        return "title|" + title;
    }

    private void mergeWorks(LinkedHashMap<String, ResearchGateWork> uniqueWorks, List<ResearchGateWork> works) {
        if (works == null) {
            return;
        }

        for (ResearchGateWork work : works) {
            if (work == null || cleanTitle(work.title()) == null) {
                continue;
            }

            uniqueWorks.putIfAbsent(buildWorkKey(work), work);
        }
    }

    private String browserUserAgent() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/124.0.0.0 Safari/537.36";
    }

    public record ResearchGateSearchResult(
            List<ResearchGateWork> works,
            int rawCount
    ) {
    }

    public record ResearchGateWork(
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