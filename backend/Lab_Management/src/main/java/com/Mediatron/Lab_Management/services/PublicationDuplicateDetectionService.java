package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PublicationDuplicateDetectionService {

    private static final double TITLE_SIMILARITY_THRESHOLD = 0.88;

    private final PublicationRepository publicationRepository;

    public PublicationDuplicateDetectionService(PublicationRepository publicationRepository) {
        this.publicationRepository = publicationRepository;
    }

    public Publication findExistingPublication(String title, Integer publicationYear, String doi) {
        String normalizedDoi = normalizeDoi(doi);
        if (normalizedDoi != null) {
            Optional<Publication> byDoi = publicationRepository.findByDoi(normalizedDoi);
            if (byDoi.isPresent()) {
                return byDoi.get();
            }
        }

        String cleanedTitle = cleanCandidateTitle(title);
        if (cleanedTitle == null) {
            return null;
        }

        if (publicationYear != null) {
            Optional<Publication> exact = publicationRepository.findByTitleIgnoreCaseAndPublicationYear(cleanedTitle, publicationYear);
            if (exact.isPresent()) {
                return exact.get();
            }

            Publication fuzzySameYear = findBestTitleMatch(cleanedTitle, publicationRepository.findByPublicationYear(publicationYear));
            if (fuzzySameYear != null) {
                return fuzzySameYear;
            }
        }

        return null;
    }

    private Publication findBestTitleMatch(String incomingTitle, List<Publication> candidates) {
        String normalizedIncoming = normalizeTitle(incomingTitle);
        if (normalizedIncoming.isBlank()) {
            return null;
        }

        Publication bestMatch = null;
        double bestScore = 0.0;

        for (Publication candidate : candidates) {
            String candidateTitle = cleanCandidateTitle(candidate.getTitle());
            if (candidateTitle == null) {
                continue;
            }

            String normalizedCandidate = normalizeTitle(candidateTitle);
            if (normalizedCandidate.isBlank()) {
                continue;
            }

            if (normalizedIncoming.equals(normalizedCandidate)) {
                return candidate;
            }

            if (isStrongContainmentMatch(normalizedIncoming, normalizedCandidate)) {
                return candidate;
            }

            double score = similarity(normalizedIncoming, normalizedCandidate);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }

        return bestScore >= TITLE_SIMILARITY_THRESHOLD ? bestMatch : null;
    }

    private boolean isStrongContainmentMatch(String first, String second) {
        String shorter = first.length() <= second.length() ? first : second;
        String longer = first.length() > second.length() ? first : second;

        if (shorter.length() < 25) {
            return false;
        }

        return longer.contains(shorter);
    }

    private double similarity(String first, String second) {
        double characterSimilarity = characterSimilarity(first, second);
        double tokenSimilarity = tokenSimilarity(first, second);
        return Math.max(characterSimilarity, tokenSimilarity);
    }

    private double characterSimilarity(String first, String second) {
        int maxLength = Math.max(first.length(), second.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(first, second);
        return 1.0 - ((double) distance / maxLength);
    }

    private double tokenSimilarity(String first, String second) {
        Set<String> firstTokens = tokenize(first);
        Set<String> secondTokens = tokenize(second);

        if (firstTokens.isEmpty() || secondTokens.isEmpty()) {
            return 0.0;
        }

        long intersection = firstTokens.stream()
                .filter(secondTokens::contains)
                .count();

        long union = Stream.concat(firstTokens.stream(), secondTokens.stream())
                .distinct()
                .count();

        return union == 0 ? 0.0 : (double) intersection / union;
    }

    private Set<String> tokenize(String value) {
        return Stream.of(value.split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }

    private int levenshteinDistance(String first, String second) {
        int[][] dp = new int[first.length() + 1][second.length() + 1];

        for (int i = 0; i <= first.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= second.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= first.length(); i++) {
            for (int j = 1; j <= second.length(); j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1
                        ),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[first.length()][second.length()];
    }

    private String cleanCandidateTitle(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }

        String result = cleaned
                .replaceFirst("^\\[[^\\]]+\\]\\s*", "")
                .replaceAll("^\"|\"$", "")
                .trim();

        return result.isBlank() ? null : result;
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeTitle(String value) {
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

        return cleaned.isBlank() ? null : cleaned.toLowerCase(Locale.ROOT);
    }
}