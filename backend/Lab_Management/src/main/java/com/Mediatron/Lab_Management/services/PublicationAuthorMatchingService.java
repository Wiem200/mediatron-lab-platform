package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PublicationAuthorMatchingService {

    private final MemberRepository memberRepository;

    @Value("${mediatron.publications.author-match-threshold:0.70}")
    private double authorMatchThreshold;

    public PublicationAuthorMatchingService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Member> findMatchingMembersForAuthors(String authorsText) {
        String normalizedAuthorsText = normalize(authorsText);

        if (normalizedAuthorsText.isBlank()) {
            return List.of();
        }

        List<String> authorNames = splitAuthorNames(authorsText);
        List<Member> members = memberRepository.findAllByOrderByLastNameAscFirstNameAsc();
        List<Member> matches = new ArrayList<>();

        for (Member member : members) {
            if (member == null || member.getId() == null) {
                continue;
            }

            double score = scoreMemberAgainstAuthors(member, authorNames, normalizedAuthorsText);

            if (score >= authorMatchThreshold) {
                matches.add(member);
            }
        }

        return matches;
    }

    public double scoreMemberAgainstAuthors(Member member, String authorsText) {
        if (member == null) {
            return 0.0;
        }

        return scoreMemberAgainstAuthors(member, splitAuthorNames(authorsText), normalize(authorsText));
    }

    private double scoreMemberAgainstAuthors(
            Member member,
            List<String> authorNames,
            String normalizedAuthorsText
    ) {
        String firstName = normalize(member.getFirstName());
        String lastName = normalize(member.getLastName());

        if (firstName.isBlank() || lastName.isBlank()) {
            return 0.0;
        }

        String naturalName = normalize(firstName + " " + lastName);
        String reversedName = normalize(lastName + " " + firstName);
        String initialLastName = firstName.substring(0, 1) + " " + lastName;
        String lastNameInitial = lastName + " " + firstName.substring(0, 1);

        double bestScore = 0.0;

        if (normalizedAuthorsText.equals(naturalName) || normalizedAuthorsText.equals(reversedName)) {
            bestScore = Math.max(bestScore, 1.0);
        }

        if (containsSeparatedName(normalizedAuthorsText, naturalName)
                || containsSeparatedName(normalizedAuthorsText, reversedName)) {
            bestScore = Math.max(bestScore, 0.97);
        }

        if (containsSeparatedName(normalizedAuthorsText, initialLastName)
                || containsSeparatedName(normalizedAuthorsText, lastNameInitial)) {
            bestScore = Math.max(bestScore, 0.82);
        }

        if (normalizedAuthorsText.contains(firstName) && normalizedAuthorsText.contains(lastName)) {
            bestScore = Math.max(bestScore, 0.78);
        }

        for (String rawAuthorName : authorNames) {
            String authorName = normalize(rawAuthorName);

            if (authorName.isBlank()) {
                continue;
            }

            bestScore = Math.max(bestScore, scoreSingleAuthorName(firstName, lastName, naturalName, reversedName, authorName));
        }

        return bestScore;
    }

    private double scoreSingleAuthorName(
            String firstName,
            String lastName,
            String naturalName,
            String reversedName,
            String authorName
    ) {
        if (authorName.equals(naturalName) || authorName.equals(reversedName)) {
            return 1.0;
        }

        if (authorName.contains(naturalName) || authorName.contains(reversedName)) {
            return 0.97;
        }

        String initialLastName = firstName.substring(0, 1) + " " + lastName;
        String lastNameInitial = lastName + " " + firstName.substring(0, 1);

        if (authorName.equals(initialLastName) || authorName.equals(lastNameInitial)
                || authorName.contains(initialLastName) || authorName.contains(lastNameInitial)) {
            return 0.84;
        }

        List<String> authorTokens = tokens(authorName);
        List<String> naturalTokens = tokens(naturalName);
        List<String> reversedTokens = tokens(reversedName);

        double naturalSimilarity = levenshteinSimilarity(authorName, naturalName);
        double reversedSimilarity = levenshteinSimilarity(authorName, reversedName);
        double tokenSimilarity = Math.max(
                tokenOverlapScore(authorTokens, naturalTokens),
                tokenOverlapScore(authorTokens, reversedTokens)
        );

        double best = Math.max(Math.max(naturalSimilarity, reversedSimilarity), tokenSimilarity);

        boolean hasLastName = authorTokens.contains(lastName);
        boolean hasFirstName = authorTokens.contains(firstName);
        boolean hasFirstInitial = authorTokens.contains(firstName.substring(0, 1));

        if (hasLastName && hasFirstName) {
            best = Math.max(best, 0.90);
        } else if (hasLastName && hasFirstInitial) {
            best = Math.max(best, 0.80);
        }

        return best;
    }

    private List<String> splitAuthorNames(String authorsText) {
        String cleaned = cleanText(authorsText);

        if (cleaned == null) {
            return List.of();
        }

        String normalizedSeparators = cleaned
                .replace(";", ",")
                .replace("|", ",")
                .replace(" · ", ",")
                .replace(" and ", ",")
                .replace(" And ", ",")
                .replace(" AND ", ",");

        return Arrays.stream(normalizedSeparators.split(","))
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .filter(value -> value.length() >= 2)
                .distinct()
                .toList();
    }

    private boolean containsSeparatedName(String normalizedText, String normalizedName) {
        if (normalizedText == null || normalizedName == null
                || normalizedText.isBlank() || normalizedName.isBlank()) {
            return false;
        }

        return (" " + normalizedText + " ").contains(" " + normalizedName + " ");
    }

    private double tokenOverlapScore(List<String> authorTokens, List<String> memberTokens) {
        if (authorTokens.isEmpty() || memberTokens.isEmpty()) {
            return 0.0;
        }

        Set<String> authorSet = authorTokens.stream()
                .filter(token -> token.length() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> memberSet = memberTokens.stream()
                .filter(token -> token.length() > 1)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (authorSet.isEmpty() || memberSet.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new LinkedHashSet<>(authorSet);
        intersection.retainAll(memberSet);

        return (double) intersection.size() / (double) memberSet.size();
    }

    private List<String> tokens(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizedText.split("\\s+"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private double levenshteinSimilarity(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);

        if (a.isBlank() || b.isBlank()) {
            return 0.0;
        }

        if (a.equals(b)) {
            return 1.0;
        }

        int distance = levenshteinDistance(a, b);
        int maxLength = Math.max(a.length(), b.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) distance / (double) maxLength);
    }

    private int levenshteinDistance(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];

        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;

            for (int j = 1; j <= right.length(); j++) {
                int substitutionCost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;

                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + substitutionCost
                );
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[right.length()];
    }

    private String normalize(String value) {
        String cleaned = cleanText(value);

        if (cleaned == null) {
            return "";
        }

        return Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank() ? null : cleaned;
    }
}
