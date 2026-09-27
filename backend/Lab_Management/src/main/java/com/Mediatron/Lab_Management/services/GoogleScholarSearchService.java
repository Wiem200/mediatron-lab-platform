package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleScholarSearchService {

    private static final String GOOGLE_SCHOLAR_SEARCH_URL = "https://scholar.google.com/scholar";
    private static final String GOOGLE_SCHOLAR_PROFILE_URL = "https://scholar.google.com/citations";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0 Safari/537.36";
    private static final int TIMEOUT_MS = 15000;
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern DOI_PATTERN = Pattern.compile("(10\\.\\d{4,9}/[-._;()/:A-Z0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern USER_PARAM_PATTERN = Pattern.compile("(?:\\?|&)user=([^&]+)");

    public GoogleScholarSearchResult searchWorks(Member member, Set<String> candidateQueries) {
        String scholarUrl = cleanText(member.getGoogleScholarUrl());
        String firstName = cleanText(member.getFirstName());
        String lastName = cleanText(member.getLastName());
        String fallbackAuthorName = buildFullName(firstName, lastName);

        List<GoogleScholarWork> aggregatedWorks = new ArrayList<>();
        LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
        List<String> sourcesUsed = new ArrayList<>();

        if (scholarUrl != null) {
            GoogleScholarSearchResult profileResult = searchFromProfileUrl(scholarUrl, fallbackAuthorName);
            if (profileResult != null && !profileResult.works().isEmpty()) {
                addUniqueWorks(aggregatedWorks, seenKeys, profileResult.works());
                sourcesUsed.add("profile");
            }
        }

        for (String query : candidateQueries) {
            List<GoogleScholarWork> works = searchFromQuery(query, firstName, lastName, fallbackAuthorName);
            if (!works.isEmpty()) {
                addUniqueWorks(aggregatedWorks, seenKeys, works);
                sourcesUsed.add(query);
            }
        }

        if (aggregatedWorks.isEmpty()) {
            return null;
        }

        return new GoogleScholarSearchResult(
                String.join(" | ", sourcesUsed),
                aggregatedWorks,
                "Google Scholar aggregated profile/search scraping used"
        );
    }

    private void addUniqueWorks(List<GoogleScholarWork> target, Set<String> seenKeys, List<GoogleScholarWork> works) {
        for (GoogleScholarWork work : works) {
            String key = buildWorkKey(work);
            if (seenKeys.add(key)) {
                target.add(work);
            }
        }
    }

    private String buildWorkKey(GoogleScholarWork work) {
        String doi = normalize(work.doi());
        if (!doi.isBlank()) {
            return "doi|" + doi;
        }

        String title = normalize(work.title());
        String year = work.publicationYear() == null ? "" : work.publicationYear().toString();
        return "titleYear|" + title + "|" + year;
    }

    private GoogleScholarSearchResult searchFromProfileUrl(String scholarUrl, String fallbackAuthorName) {
        String userId = extractUserIdFromScholarUrl(scholarUrl);
        if (userId == null) {
            return null;
        }

        try {
            Connection.Response response = Jsoup.connect(GOOGLE_SCHOLAR_PROFILE_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .data("hl", "en")
                    .data("user", userId)
                    .data("view_op", "list_works")
                    .data("sortby", "pubdate")
                    .data("pagesize", "100")
                    .execute();

            Document document = response.parse();
            ensureScholarNotBlocked(response, document, "Google Scholar profile");

            Elements rows = document.select("tr.gsc_a_tr");
            List<GoogleScholarWork> works = new ArrayList<>();

            for (Element row : rows) {
                GoogleScholarWork work = mapProfileRow(row, fallbackAuthorName);
                if (work != null) {
                    works.add(work);
                }
            }

            if (works.isEmpty()) {
                return null;
            }

            return new GoogleScholarSearchResult(
                    scholarUrl,
                    works,
                    "Google Scholar profile used"
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to scrape Google Scholar profile: " + ex.getMessage(), ex);
        }
    }

    private List<GoogleScholarWork> searchFromQuery(
            String query,
            String firstName,
            String lastName,
            String fallbackAuthorName
    ) {
        try {
            Connection.Response response = Jsoup.connect(GOOGLE_SCHOLAR_SEARCH_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreHttpErrors(true)
                    .data("hl", "en")
                    .data("q", query)
                    .execute();

            Document document = response.parse();
            ensureScholarNotBlocked(response, document, "Google Scholar search");

            Elements resultCards = document.select("div.gs_r.gs_or.gs_scl, div.gs_scl.gs_or.gs_r");
            List<GoogleScholarWork> works = new ArrayList<>();

            for (Element card : resultCards) {
                GoogleScholarWork work = mapSearchResult(card, fallbackAuthorName, query);
                if (work == null) {
                    continue;
                }

                if (googleScholarAuthorsMatch(work.authorsText(), firstName, lastName)) {
                    works.add(work);
                }
            }

            return works;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to scrape Google Scholar search results: " + ex.getMessage(), ex);
        }
    }

    private GoogleScholarWork mapProfileRow(Element row, String fallbackAuthorName) {
        Element titleLink = row.selectFirst("a.gsc_a_at");
        if (titleLink == null) {
            return null;
        }

        String title = cleanText(titleLink.text());
        if (title == null) {
            return null;
        }

        Elements meta = row.select("td.gsc_a_t .gs_gray");
        String authorsText = meta.size() > 0 ? cleanText(meta.get(0).text()) : fallbackAuthorName;
        String indexation = meta.size() > 1 ? cleanText(meta.get(1).text()) : "Google Scholar";
        Integer year = extractYear(
                meta.size() > 1 ? meta.get(1).text() : null,
                row.selectFirst("td.gsc_a_y span") != null ? row.selectFirst("td.gsc_a_y span").text() : null
        );

        String scholarDetailUrl = absoluteUrl(titleLink, "href");

        return new GoogleScholarWork(
                title,
                year,
                null,
                authorsText != null ? authorsText : fallbackAuthorName,
                scholarDetailUrl,
                null,
                indexation != null ? indexation : "Google Scholar",
                inferPublicationType(title, authorsText, indexation),
                joinContext(title, authorsText, indexation, scholarDetailUrl)
        );
    }

    private GoogleScholarWork mapSearchResult(Element card, String fallbackAuthorName, String queryUsed) {
        Element titleElement = card.selectFirst("h3.gs_rt");
        if (titleElement == null) {
            return null;
        }

        String title = cleanText(stripBracketPrefix(titleElement.text()));
        if (title == null) {
            return null;
        }

        Element titleLink = titleElement.selectFirst("a");
        String externalUrl = titleLink != null ? absoluteUrl(titleLink, "href") : null;

        Element pdfLink = card.selectFirst("div.gs_or_ggsm a");
        String pdfUrl = pdfLink != null ? absoluteUrl(pdfLink, "href") : null;

        String authorsLine = cleanText(textOf(card.selectFirst("div.gs_a")));
        String snippet = cleanText(textOf(card.selectFirst("div.gs_rs, div.gs_rs.gs_scl")));
        String authorsText = extractScholarAuthorsText(authorsLine, fallbackAuthorName);
        String indexation = extractScholarIndexation(authorsLine);
        Integer year = extractYear(authorsLine, snippet);
        String doi = extractDoi(externalUrl, authorsLine, snippet, title);

        return new GoogleScholarWork(
                title,
                year,
                doi,
                authorsText,
                externalUrl,
                pdfUrl,
                indexation,
                inferPublicationType(title, authorsLine, snippet),
                joinContext(queryUsed, title, authorsLine, snippet, externalUrl, pdfUrl)
        );
    }

    private void ensureScholarNotBlocked(Connection.Response response, Document document, String sourceName) {
        if (response.statusCode() == 429) {
            throw new IllegalStateException(sourceName + " is rate-limited (429 Too Many Requests).");
        }

        String title = normalize(cleanText(document.title()));
        String body = normalize(cleanText(document.body() != null ? document.body().text() : null));

        boolean looksBlocked =
                title.contains("sorry") ||
                        body.contains("unusual traffic") ||
                        body.contains("not a robot") ||
                        body.contains("detected unusual traffic");

        if (looksBlocked) {
            throw new IllegalStateException(sourceName + " blocked the scraper request.");
        }
    }

    private boolean googleScholarAuthorsMatch(String authorsText, String firstName, String lastName) {
        String normalizedAuthors = normalize(authorsText);
        String normalizedFirst = normalize(firstName);
        String normalizedLast = normalize(lastName);

        if (normalizedAuthors.isBlank() || normalizedLast.isBlank()) {
            return false;
        }

        boolean containsFull =
                !normalizedFirst.isBlank() &&
                        normalizedAuthors.contains(normalizedFirst) &&
                        normalizedAuthors.contains(normalizedLast);

        boolean exactNatural = normalizedAuthors.contains(normalizedFirst + " " + normalizedLast);
        boolean exactReversed = normalizedAuthors.contains(normalizedLast + " " + normalizedFirst);

        boolean initialLast = false;
        if (!normalizedFirst.isBlank()) {
            String initial = normalizedFirst.substring(0, 1);
            initialLast =
                    normalizedAuthors.contains(initial + " " + normalizedLast) ||
                            normalizedAuthors.contains(normalizedLast + " " + initial);
        }

        return containsFull || exactNatural || exactReversed || initialLast;
    }

    private String extractScholarAuthorsText(String authorsLine, String fallbackAuthorName) {
        if (authorsLine == null || authorsLine.isBlank()) {
            return fallbackAuthorName;
        }

        String[] parts = authorsLine.split("\\s-\\s", 2);
        String authors = cleanText(parts[0]);
        return authors != null ? authors : fallbackAuthorName;
    }

    private String extractScholarIndexation(String authorsLine) {
        if (authorsLine == null || authorsLine.isBlank()) {
            return "Google Scholar";
        }

        String[] parts = authorsLine.split("\\s-\\s");
        if (parts.length >= 2) {
            String candidate = cleanText(parts[1]);
            if (candidate != null) {
                return candidate;
            }
        }

        return "Google Scholar";
    }

    private Integer extractYear(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            Matcher matcher = YEAR_PATTERN.matcher(value);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private String extractDoi(String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }

            Matcher matcher = DOI_PATTERN.matcher(value);
            if (matcher.find()) {
                return cleanText(matcher.group(1));
            }
        }
        return null;
    }

    private PublicationType inferPublicationType(String... values) {
        String joined = normalize(String.join(" ", sanitizeValues(values)));

        if (joined.contains("journal")) {
            return PublicationType.JOURNAL;
        }
        if (joined.contains("conference") || joined.contains("proceedings") || joined.contains("symposium")) {
            return PublicationType.CONFERENCE_C;
        }
        if (joined.contains("workshop")) {
            return PublicationType.WORKSHOP;
        }
        if (joined.contains("chapter") || joined.contains("book")) {
            return PublicationType.BOOK_CHAPTER;
        }

        return PublicationType.OTHER;
    }

    private String joinContext(String... values) {
        return String.join(" ", sanitizeValues(values));
    }

    private List<String> sanitizeValues(String... values) {
        List<String> cleaned = new ArrayList<>();
        for (String value : values) {
            String text = cleanText(value);
            if (text != null) {
                cleaned.add(text);
            }
        }
        return cleaned;
    }

    private String extractUserIdFromScholarUrl(String scholarUrl) {
        Matcher matcher = USER_PARAM_PATTERN.matcher(scholarUrl);
        if (!matcher.find()) {
            return null;
        }

        try {
            return URLDecoder.decode(matcher.group(1), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            return matcher.group(1);
        }
    }

    private String stripBracketPrefix(String title) {
        if (title == null) {
            return null;
        }
        return title.replaceFirst("^\\[[^\\]]+\\]\\s*", "").trim();
    }

    private String absoluteUrl(Element element, String attribute) {
        String absolute = cleanText(element.absUrl(attribute));
        if (absolute != null) {
            return absolute;
        }

        return cleanText(element.attr(attribute));
    }

    private String textOf(Element element) {
        return element == null ? null : element.text();
    }

    private String buildFullName(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
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

    public record GoogleScholarSearchResult(
            String queryUsed,
            List<GoogleScholarWork> works,
            String message
    ) {
    }

    public record GoogleScholarWork(
            String title,
            Integer publicationYear,
            String doi,
            String authorsText,
            String externalUrl,
            String pdfUrl,
            String indexation,
            PublicationType type,
            String contextText
    ) {
    }
}
