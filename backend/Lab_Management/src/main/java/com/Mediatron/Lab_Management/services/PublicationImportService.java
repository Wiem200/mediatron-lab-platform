package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.PublicationImportResult;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.MemberPublication;
import com.Mediatron.Lab_Management.data_models.MemberPublicationId;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.data_models.PublicationType;
import com.Mediatron.Lab_Management.exceptions.DuplicatePublicationDoiException;
import com.Mediatron.Lab_Management.repositories.MemberPublicationRepository;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Service
public class PublicationImportService {

    private static final String SEMANTIC_SCHOLAR_BASE_URL = "https://api.semanticscholar.org/graph/v1";
    private static final int AUTHOR_SEARCH_LIMIT = 10;
    private static final int PAPER_LIMIT = 20;

    private static final Set<String> INVALID_TITLE_LABELS = Set.of(
            "author names",
            "authors",
            "author",
            "cited by",
            "related articles",
            "all versions",
            "view all",
            "articles",
            "citations",
            "scholar articles",
            "google scholar",
            "abstract",
            "references"
    );

    private static final Set<String> AFFILIATION_KEYWORDS = Set.of(
            "university",
            "universite",
            "universitee",
            "faculty",
            "department",
            "departement",
            "school",
            "college",
            "laboratory",
            "laboratoire",
            "institute",
            "institut",
            "center",
            "centre",
            "innovation",
            "innovations",
            "mobility",
            "qatar",
            "france",
            "tunisia",
            "uae",
            "new zealand",
            "research center",
            "research centre"
    );

    private final MemberRepository memberRepository;
    private final MemberPublicationRepository memberPublicationRepository;
    private final PublicationRepository publicationRepository;
    private final PublicationDuplicateDetectionService publicationDuplicateDetectionService;
    private final CrossRefSearchService crossRefSearchService;
    private final OpenAlexSearchService openAlexSearchService;
    private final DblpSearchService dblpSearchService;
    private final ResearchGateSearchService researchGateSearchService;
    private final GoogleScholarSearchService googleScholarSearchService;
    private final MemberPublicationNotificationService memberPublicationNotificationService;
    private final PublicationAuthorMatchingService publicationAuthorMatchingService;
    private final RestTemplate restTemplate;

    @Value("${SEMANTIC_SCHOLAR_API_KEY:}")
    private String semanticScholarApiKey;

    public PublicationImportService(
            MemberRepository memberRepository,
            MemberPublicationRepository memberPublicationRepository,
            PublicationRepository publicationRepository,
            PublicationDuplicateDetectionService publicationDuplicateDetectionService,
            CrossRefSearchService crossRefSearchService,
            OpenAlexSearchService openAlexSearchService,
            DblpSearchService dblpSearchService,
            ResearchGateSearchService researchGateSearchService,
            GoogleScholarSearchService googleScholarSearchService,
            MemberPublicationNotificationService memberPublicationNotificationService,
            PublicationAuthorMatchingService publicationAuthorMatchingService
    ) {
        this.memberRepository = memberRepository;
        this.memberPublicationRepository = memberPublicationRepository;
        this.publicationRepository = publicationRepository;
        this.publicationDuplicateDetectionService = publicationDuplicateDetectionService;
        this.crossRefSearchService = crossRefSearchService;
        this.openAlexSearchService = openAlexSearchService;
        this.dblpSearchService = dblpSearchService;
        this.researchGateSearchService = researchGateSearchService;
        this.googleScholarSearchService = googleScholarSearchService;
        this.memberPublicationNotificationService = memberPublicationNotificationService;
        this.publicationAuthorMatchingService = publicationAuthorMatchingService;
        this.restTemplate = new RestTemplate();
    }

    private void schedulePublicationNotificationsAfterCommit(Set<Integer> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        LinkedHashSet<Integer> safeMemberIds = memberIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (safeMemberIds.isEmpty()) {
            return;
        }

        Runnable notificationTask = () -> {
            for (Integer matchedMemberId : safeMemberIds) {
                try {
                    memberPublicationNotificationService.notifyPendingPublicationsForMember(matchedMemberId);
                } catch (Exception ex) {
                    System.err.println(
                            "Publication notification failed for matched member "
                                    + matchedMemberId
                                    + ": "
                                    + cleanErrorMessage(ex)
                    );
                }
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationTask.run();
                }
            });
            return;
        }

        notificationTask.run();
    }

    @Transactional
    public PublicationImportResult importPublicationsForMember(Integer memberId, String manualQuery) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException("Member not found with id: " + memberId));

        String memberName = buildMemberFullName(member);
        if (memberName.isBlank()) {
            throw new IllegalArgumentException("Member must have a first name and last name before import.");
        }

        String firstName = cleanText(member.getFirstName());
        String lastName = cleanText(member.getLastName());
        String queryUsed = cleanText(manualQuery) != null ? cleanText(manualQuery) : memberName;

        Set<String> candidateQueries = buildCandidateQueries(member, manualQuery);

        List<ImportCandidate> allCandidates = new ArrayList<>();
        List<String> sourceMessages = new ArrayList<>();

        int totalFetched = 0;
        boolean semanticScholarRateLimited = false;
        String semanticScholarError = null;
        String crossRefError = null;
        String openAlexError = null;
        String dblpError = null;
        String researchGateError = null;
        String googleScholarError = null;

        try {
            SourcePullResult semanticScholarResult =
                    fetchSemanticScholarCandidates(member, firstName, lastName, candidateQueries, memberName);
            totalFetched += semanticScholarResult.rawCount();
            allCandidates.addAll(semanticScholarResult.candidates());

            if (semanticScholarResult.rawCount() > 0) {
                sourceMessages.add("Semantic Scholar: " + semanticScholarResult.rawCount());
            } else {
                sourceMessages.add("Semantic Scholar: 0");
            }
        } catch (SemanticScholarRateLimitException ex) {
            semanticScholarRateLimited = true;
            sourceMessages.add("Semantic Scholar: rate-limited");
        } catch (IllegalStateException ex) {
            semanticScholarError = ex.getMessage();
            sourceMessages.add("Semantic Scholar: error");
        }

        try {
            SourcePullResult crossRefResult =
                    fetchCrossRefCandidates(member, candidateQueries, firstName, lastName, memberName);
            totalFetched += crossRefResult.rawCount();
            allCandidates.addAll(crossRefResult.candidates());
            sourceMessages.add("CrossRef: " + crossRefResult.rawCount());
        } catch (IllegalStateException ex) {
            crossRefError = ex.getMessage();
            sourceMessages.add("CrossRef: error");
        }

        try {
            SourcePullResult openAlexResult =
                    fetchOpenAlexCandidates(member, candidateQueries, firstName, lastName, memberName);
            totalFetched += openAlexResult.rawCount();
            allCandidates.addAll(openAlexResult.candidates());
            sourceMessages.add("OpenAlex: " + openAlexResult.rawCount());
        } catch (IllegalStateException ex) {
            openAlexError = ex.getMessage();
            sourceMessages.add("OpenAlex: error");
        }

        try {
            SourcePullResult dblpResult =
                    fetchDblpCandidates(member, candidateQueries, firstName, lastName, memberName);
            totalFetched += dblpResult.rawCount();
            allCandidates.addAll(dblpResult.candidates());
            sourceMessages.add("DBLP: " + dblpResult.rawCount());
        } catch (IllegalStateException ex) {
            dblpError = ex.getMessage();
            sourceMessages.add("DBLP: error");
        }

        try {
            SourcePullResult researchGateResult =
                    fetchResearchGateCandidates(member, candidateQueries, firstName, lastName, memberName);
            totalFetched += researchGateResult.rawCount();
            allCandidates.addAll(researchGateResult.candidates());
            sourceMessages.add("ResearchGate: " + researchGateResult.rawCount());
        } catch (IllegalStateException ex) {
            researchGateError = ex.getMessage();
            sourceMessages.add("ResearchGate: error");
        }

        try {
            SourcePullResult googleScholarResult =
                    fetchGoogleScholarCandidates(member, candidateQueries, memberName);
            totalFetched += googleScholarResult.rawCount();
            allCandidates.addAll(googleScholarResult.candidates());
            sourceMessages.add("Google Scholar: " + googleScholarResult.rawCount());
        } catch (IllegalStateException ex) {
            googleScholarError = ex.getMessage();
            sourceMessages.add("Google Scholar: error");
        }

        LinkedHashMap<String, ImportCandidate> uniqueCandidates = deduplicateCandidates(allCandidates);
        int totalUniqueCandidates = uniqueCandidates.size();

        if (uniqueCandidates.isEmpty()) {
            return new PublicationImportResult(
                    member.getId(),
                    memberName,
                    queryUsed,
                    totalFetched,
                    0,
                    0,
                    0,
                    buildNoMatchMessage(
                            sourceMessages,
                            semanticScholarRateLimited,
                            semanticScholarError,
                            crossRefError,
                            openAlexError,
                            dblpError,
                            researchGateError,
                            googleScholarError
                    )
            );
        }

        Set<String> existingMemberFingerprints = buildExistingMemberFingerprints(
                publicationRepository.findByMembers_IdOrderByPublicationYearDescCreatedAtDesc(member.getId())
        );

        LinkedHashMap<String, ImportCandidate> unseenCandidates =
                filterAlreadyScrapedCandidatesForMember(uniqueCandidates, existingMemberFingerprints);

        if (unseenCandidates.isEmpty()) {
            return new PublicationImportResult(
                    member.getId(),
                    memberName,
                    queryUsed,
                    totalFetched,
                    0,
                    0,
                    totalUniqueCandidates,
                    buildNoNewPublicationsMessage(sourceMessages, totalUniqueCandidates)
            );
        }

        int importedCount = 0;
        int linkedExistingCount = 0;
        boolean memberUpdated = false;
        Set<Integer> membersToNotify = new LinkedHashSet<>();

        for (ImportCandidate candidate : unseenCandidates.values()) {
            ImportOutcome outcome = importCandidate(member, candidate, membersToNotify);

            switch (outcome) {
                case IMPORTED -> {
                    importedCount++;
                    memberUpdated = true;
                }
                case LINKED_EXISTING -> {
                    linkedExistingCount++;
                    memberUpdated = true;
                }
                case SKIPPED -> {
                }
            }
        }

        if (memberUpdated) {
            schedulePublicationNotificationsAfterCommit(membersToNotify);
        }

        int skippedCount = Math.max(0, totalUniqueCandidates - importedCount - linkedExistingCount);

        String message = buildSuccessMessage(
                sourceMessages,
                totalUniqueCandidates,
                unseenCandidates.size()
        );

        return new PublicationImportResult(
                member.getId(),
                memberName,
                queryUsed,
                totalFetched,
                importedCount,
                linkedExistingCount,
                skippedCount,
                message
        );
    }

    private SourcePullResult fetchSemanticScholarCandidates(
            Member member,
            String firstName,
            String lastName,
            Set<String> candidateQueries,
            String fallbackAuthorName
    ) {
        List<ImportCandidate> candidates = new ArrayList<>();
        int rawCount = 0;

        AuthorMatch authorMatch = resolveSemanticScholarAuthor(firstName, lastName, candidateQueries);

        if (authorMatch != null) {
            JsonNode papersRoot = fetchAuthorPapers(authorMatch.authorId());
            JsonNode papersArray = papersRoot.path("data");

            rawCount += papersArray.isArray() ? papersArray.size() : 0;

            for (JsonNode paperNode : iterableOf(papersArray)) {
                if (!paperHasStrongAuthorMatch(paperNode.path("authors"), firstName, lastName)) {
                    continue;
                }

                if (!hasMediatronSignal(member, paperNode.toString())) {
                    continue;
                }

                ImportCandidate candidate = mapSemanticScholarCandidate(paperNode, fallbackAuthorName);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        PaperSearchMatch paperSearchMatch = resolvePapersFromSearch(firstName, lastName, candidateQueries);
        if (paperSearchMatch != null) {
            JsonNode papersArray = paperSearchMatch.papers();
            rawCount += papersArray.isArray() ? papersArray.size() : 0;

            for (JsonNode paperNode : iterableOf(papersArray)) {
                if (!paperHasStrongAuthorMatch(paperNode.path("authors"), firstName, lastName)) {
                    continue;
                }

                if (!hasMediatronSignal(member, paperNode.toString())) {
                    continue;
                }

                ImportCandidate candidate = mapSemanticScholarCandidate(paperNode, fallbackAuthorName);
                if (candidate != null) {
                    candidates.add(candidate);
                }
            }
        }

        return new SourcePullResult(candidates, rawCount);
    }

    private SourcePullResult fetchCrossRefCandidates(
            Member member,
            Set<String> candidateQueries,
            String firstName,
            String lastName,
            String fallbackAuthorName
    ) {
        CrossRefSearchService.CrossRefSearchResult result =
                crossRefSearchService.searchWorks(candidateQueries, firstName, lastName);

        if (result == null || result.items() == null || !result.items().isArray()) {
            return new SourcePullResult(List.of(), 0);
        }

        List<ImportCandidate> candidates = new ArrayList<>();
        JsonNode items = result.items();

        for (JsonNode item : iterableOf(items)) {
            if (!hasMediatronSignal(member, item.toString())) {
                continue;
            }

            ImportCandidate candidate = mapCrossRefCandidate(item, fallbackAuthorName);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return new SourcePullResult(candidates, items.size());
    }

    private SourcePullResult fetchOpenAlexCandidates(
            Member member,
            Set<String> candidateQueries,
            String firstName,
            String lastName,
            String fallbackAuthorName
    ) {
        OpenAlexSearchService.OpenAlexSearchResult result =
                openAlexSearchService.searchWorks(candidateQueries, firstName, lastName);

        if (result == null || result.works() == null || !result.works().isArray()) {
            return new SourcePullResult(List.of(), 0);
        }

        List<ImportCandidate> candidates = new ArrayList<>();
        JsonNode works = result.works();

        for (JsonNode work : iterableOf(works)) {
            if (!hasMediatronSignal(member, work.toString())) {
                continue;
            }

            ImportCandidate candidate = mapOpenAlexCandidate(work, fallbackAuthorName);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return new SourcePullResult(candidates, works.size());
    }

    private SourcePullResult fetchDblpCandidates(
            Member member,
            Set<String> candidateQueries,
            String firstName,
            String lastName,
            String fallbackAuthorName
    ) {
        DblpSearchService.DblpSearchResult result =
                dblpSearchService.searchWorks(candidateQueries, firstName, lastName);

        if (result == null || result.works() == null || result.works().isEmpty()) {
            return new SourcePullResult(List.of(), 0);
        }

        List<ImportCandidate> candidates = new ArrayList<>();

        for (DblpSearchService.DblpWork work : result.works()) {
            boolean mediatronSignalFound = hasMediatronSignal(
                    member,
                    work.title(),
                    work.authorsText(),
                    work.externalUrl(),
                    work.pdfUrl(),
                    work.indexation(),
                    work.contextText()
            );

            boolean trustedComputerScienceBibliographyMatch = isTrustedDblpFallbackMatch(
                    work,
                    firstName,
                    lastName
            );

            if (!mediatronSignalFound && !trustedComputerScienceBibliographyMatch) {
                continue;
            }

            ImportCandidate candidate = mapDblpCandidate(work, fallbackAuthorName);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return new SourcePullResult(candidates, result.rawCount());
    }

    private SourcePullResult fetchResearchGateCandidates(
            Member member,
            Set<String> candidateQueries,
            String firstName,
            String lastName,
            String fallbackAuthorName
    ) {
        ResearchGateSearchService.ResearchGateSearchResult result =
                researchGateSearchService.searchWorks(candidateQueries, firstName, lastName);

        if (result == null || result.works() == null || result.works().isEmpty()) {
            return new SourcePullResult(List.of(), 0);
        }

        List<ImportCandidate> candidates = new ArrayList<>();

        for (ResearchGateSearchService.ResearchGateWork work : result.works()) {
            boolean mediatronSignalFound = hasMediatronSignal(
                    member,
                    work.title(),
                    work.authorsText(),
                    work.externalUrl(),
                    work.pdfUrl(),
                    work.indexation(),
                    work.contextText()
            );

            boolean trustedResearchGateFallbackMatch = isTrustedResearchGateFallbackMatch(
                    work,
                    firstName,
                    lastName
            );

            if (!mediatronSignalFound && !trustedResearchGateFallbackMatch) {
                continue;
            }

            ImportCandidate candidate = mapResearchGateCandidate(work, fallbackAuthorName);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return new SourcePullResult(candidates, result.rawCount());
    }

    private SourcePullResult fetchGoogleScholarCandidates(
            Member member,
            Set<String> candidateQueries,
            String fallbackAuthorName
    ) {
        GoogleScholarSearchService.GoogleScholarSearchResult result =
                googleScholarSearchService.searchWorks(member, candidateQueries);

        if (result == null || result.works() == null || result.works().isEmpty()) {
            return new SourcePullResult(List.of(), 0);
        }

        List<ImportCandidate> candidates = new ArrayList<>();

        for (GoogleScholarSearchService.GoogleScholarWork work : result.works()) {
            if (!hasMediatronSignal(
                    member,
                    work.title(),
                    work.authorsText(),
                    work.externalUrl(),
                    work.pdfUrl(),
                    work.indexation(),
                    work.contextText()
            )) {
                continue;
            }

            ImportCandidate candidate = mapGoogleScholarCandidate(work, fallbackAuthorName);
            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return new SourcePullResult(candidates, result.works().size());
    }

    private LinkedHashMap<String, ImportCandidate> deduplicateCandidates(List<ImportCandidate> candidates) {
        LinkedHashMap<String, ImportCandidate> unique = new LinkedHashMap<>();

        for (ImportCandidate rawCandidate : candidates) {
            ImportCandidate candidate = sanitizeCandidate(rawCandidate);
            if (candidate == null) {
                continue;
            }

            String key = buildCandidateKey(candidate);

            if (!unique.containsKey(key)) {
                unique.put(key, candidate);
            } else {
                unique.put(key, mergeCandidates(unique.get(key), candidate));
            }
        }

        return unique;
    }

    private LinkedHashMap<String, ImportCandidate> filterAlreadyScrapedCandidatesForMember(
            LinkedHashMap<String, ImportCandidate> uniqueCandidates,
            Set<String> existingMemberFingerprints
    ) {
        LinkedHashMap<String, ImportCandidate> filtered = new LinkedHashMap<>();

        for (Map.Entry<String, ImportCandidate> entry : uniqueCandidates.entrySet()) {
            if (!isAlreadyScrapedForMember(entry.getValue(), existingMemberFingerprints)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }

        return filtered;
    }

    private boolean isAlreadyScrapedForMember(
            ImportCandidate candidate,
            Set<String> existingMemberFingerprints
    ) {
        for (String fingerprint : buildFingerprintsForCandidate(candidate)) {
            if (existingMemberFingerprints.contains(fingerprint)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> buildExistingMemberFingerprints(List<Publication> existingPublications) {
        Set<String> fingerprints = new HashSet<>();

        for (Publication publication : existingPublications) {
            fingerprints.addAll(buildFingerprintsForPublication(publication));
        }

        return fingerprints;
    }

    private Set<String> buildFingerprintsForPublication(Publication publication) {
        LinkedHashSet<String> fingerprints = new LinkedHashSet<>();

        String normalizedDoi = normalizeDoi(publication.getDoi());
        String cleanedTitle = cleanCandidateTitle(publication.getTitle());
        String normalizedTitle = normalize(cleanedTitle);
        Integer publicationYear = publication.getPublicationYear();

        if (normalizedDoi != null) {
            fingerprints.add("doi|" + normalizedDoi);
        }

        if (!normalizedTitle.isBlank() && publicationYear != null) {
            fingerprints.add("titleYear|" + normalizedTitle + "|" + publicationYear);
        }

        if (!normalizedTitle.isBlank()) {
            fingerprints.add("title|" + normalizedTitle);
        }

        return fingerprints;
    }

    private Set<String> buildFingerprintsForCandidate(ImportCandidate candidate) {
        LinkedHashSet<String> fingerprints = new LinkedHashSet<>();

        String normalizedDoi = normalizeDoi(candidate.doi());
        String cleanedTitle = cleanCandidateTitle(candidate.title());
        String normalizedTitle = normalize(cleanedTitle);
        Integer publicationYear = candidate.publicationYear();

        if (normalizedDoi != null) {
            fingerprints.add("doi|" + normalizedDoi);
        }

        if (!normalizedTitle.isBlank() && publicationYear != null) {
            fingerprints.add("titleYear|" + normalizedTitle + "|" + publicationYear);
        }

        if (!normalizedTitle.isBlank()) {
            fingerprints.add("title|" + normalizedTitle);
        }

        return fingerprints;
    }

    private ImportCandidate sanitizeCandidate(ImportCandidate candidate) {
        if (candidate == null) {
            return null;
        }

        String cleanedAuthors = cleanText(candidate.authorsText());
        String cleanedIndexation = cleanText(candidate.indexation());

        String cleanedTitle = sanitizeTitle(
                candidate.title(),
                cleanedAuthors,
                candidate.publicationYear(),
                candidate.source(),
                candidate.type(),
                cleanedIndexation
        );

        if (cleanedTitle == null) {
            return null;
        }

        return new ImportCandidate(
                cleanedTitle,
                candidate.publicationYear(),
                normalizeDoi(candidate.doi()),
                cleanedAuthors,
                candidate.source(),
                cleanText(candidate.externalUrl()),
                cleanText(candidate.pdfUrl()),
                cleanedIndexation,
                candidate.type()
        );
    }

    private String sanitizeTitle(
            String rawTitle,
            String authorsText,
            Integer publicationYear,
            ImportSource source,
            PublicationType type,
            String indexation
    ) {
        String cleaned = cleanCandidateTitle(rawTitle);
        if (cleaned == null) {
            return null;
        }

        if (isInvalidPublicationTitle(cleaned, authorsText, publicationYear, source, type, indexation)) {
            return null;
        }

        return cleaned;
    }

    private String buildCandidateKey(ImportCandidate candidate) {
        String normalizedDoi = normalizeDoi(candidate.doi());
        if (normalizedDoi != null) {
            return "doi|" + normalizedDoi;
        }

        String normalizedTitle = normalize(candidate.title());
        Integer year = candidate.publicationYear();

        if (!normalizedTitle.isBlank() && year != null) {
            return "titleYear|" + normalizedTitle + "|" + year;
        }

        return "title|" + normalizedTitle;
    }

    private ImportCandidate mergeCandidates(ImportCandidate current, ImportCandidate incoming) {
        ImportSource preferredSource = preferredSource(current.source(), incoming.source());

        String title = chooseBetterTitle(current.title(), incoming.title());
        Integer publicationYear = chooseBetterYear(current.publicationYear(), incoming.publicationYear());
        String doi = chooseBetterDoi(current.doi(), incoming.doi());
        String authorsText = chooseBetterAuthors(
                current.authorsText(), incoming.authorsText(), preferredSource, current.source(), incoming.source()
        );
        String externalUrl = chooseBetterUrl(
                current.externalUrl(), incoming.externalUrl(), preferredSource, current.source(), incoming.source()
        );
        String pdfUrl = chooseBetterUrl(
                current.pdfUrl(), incoming.pdfUrl(), preferredSource, current.source(), incoming.source()
        );
        String indexation = chooseBetterIndexation(
                current.indexation(), incoming.indexation(), preferredSource, current.source(), incoming.source()
        );
        PublicationType type = chooseBetterType(
                current.type(), incoming.type(), preferredSource, current.source(), incoming.source()
        );

        return new ImportCandidate(
                title,
                publicationYear,
                doi,
                authorsText,
                preferredSource,
                externalUrl,
                pdfUrl,
                indexation,
                type
        );
    }

    private ImportOutcome importCandidate(
            Member triggerMember,
            ImportCandidate rawCandidate,
            Set<Integer> membersToNotify
    ) {
        ImportCandidate candidate = sanitizeCandidate(rawCandidate);
        if (candidate == null) {
            return ImportOutcome.SKIPPED;
        }

        String title = candidate.title();
        String doi = normalizeDoi(candidate.doi());
        Integer publicationYear = candidate.publicationYear();

        Publication existingPublication = publicationDuplicateDetectionService.findExistingPublication(
                title, publicationYear, doi
        );

        if (existingPublication != null) {
            int newlyLinkedMembers = linkMatchedMembersToPublication(triggerMember, existingPublication, candidate, membersToNotify);
            return newlyLinkedMembers > 0 ? ImportOutcome.LINKED_EXISTING : ImportOutcome.SKIPPED;
        }

        Publication publication = new Publication();
        publication.setTitle(title);
        publication.setDoi(doi);
        publication.setPublicationYear(publicationYear);
        publication.setAuthorsText(cleanText(candidate.authorsText()));
        publication.setSource(candidate.source().dbValue());
        publication.setExternalUrl(cleanText(candidate.externalUrl()));
        publication.setPdfUrl(cleanText(candidate.pdfUrl()));
        publication.setIndexation(
                cleanText(candidate.indexation()) != null
                        ? cleanText(candidate.indexation())
                        : candidate.source().displayName()
        );
        publication.setType(candidate.type() != null ? candidate.type() : PublicationType.OTHER);
        publication.setStatus(PublicationStatus.PENDING);

        try {
            Publication savedPublication = publicationRepository.saveAndFlush(publication);
            linkMatchedMembersToPublication(triggerMember, savedPublication, candidate, membersToNotify);
            return ImportOutcome.IMPORTED;
        } catch (DataIntegrityViolationException ex) {
            if (doi != null) {
                Publication duplicateByDoi = publicationRepository.findByDoi(doi).orElse(null);

                if (duplicateByDoi != null) {
                    int newlyLinkedMembers = linkMatchedMembersToPublication(triggerMember, duplicateByDoi, candidate, membersToNotify);
                    return newlyLinkedMembers > 0 ? ImportOutcome.LINKED_EXISTING : ImportOutcome.SKIPPED;
                }

                throw new DuplicatePublicationDoiException(
                        doi,
                        "Duplicate DOI detected while importing publication with DOI '" + doi + "'."
                );
            }

            throw ex;
        }
    }

    private int linkMatchedMembersToPublication(
            Member triggerMember,
            Publication publication,
            ImportCandidate candidate,
            Set<Integer> membersToNotify
    ) {
        if (publication == null || publication.getId() == null) {
            return 0;
        }

        LinkedHashSet<Member> matchedMembers = new LinkedHashSet<>(
                publicationAuthorMatchingService.findMatchingMembersForAuthors(candidate.authorsText())
        );

        if (triggerMember != null && triggerMember.getId() != null) {
            matchedMembers.add(triggerMember);
        }

        int newlyLinkedMembers = 0;

        for (Member matchedMember : matchedMembers) {
            if (matchedMember == null || matchedMember.getId() == null) {
                continue;
            }

            MemberPublicationId id = new MemberPublicationId(matchedMember.getId(), publication.getId());

            if (memberPublicationRepository.existsById(id)) {
                continue;
            }

            MemberPublication memberPublication = new MemberPublication();
            memberPublication.setId(id);
            memberPublication.setMember(matchedMember);
            memberPublication.setPublication(publication);
            memberPublication.setStatus(PublicationStatus.PENDING);
            memberPublication.setVerified(false);

            try {
                memberPublicationRepository.saveAndFlush(memberPublication);
                membersToNotify.add(matchedMember.getId());
                newlyLinkedMembers++;
            } catch (DataIntegrityViolationException ignored) {
                // Another import already linked the same member and publication.
            }
        }

        return newlyLinkedMembers;
    }

    private ImportCandidate mapSemanticScholarCandidate(JsonNode paperNode, String fallbackAuthorName) {
        String title = cleanCandidateTitle(cleanText(paperNode.path("title").asText(null)));
        if (title == null) {
            return null;
        }

        Integer publicationYear = paperNode.path("year").isInt() ? paperNode.path("year").asInt() : null;
        String doi = normalizeDoi(cleanText(paperNode.path("externalIds").path("DOI").asText(null)));
        String authorsText = extractSemanticScholarAuthorsText(paperNode.path("authors"), fallbackAuthorName);
        String externalUrl = cleanText(paperNode.path("url").asText(null));
        String indexation = extractSemanticScholarIndexation(paperNode);
        PublicationType type = mapSemanticScholarPublicationType(paperNode.path("publicationTypes"));

        return new ImportCandidate(
                title,
                publicationYear,
                doi,
                authorsText,
                ImportSource.SEMANTIC_SCHOLAR,
                externalUrl,
                null,
                indexation,
                type
        );
    }

    private ImportCandidate mapCrossRefCandidate(JsonNode workNode, String fallbackAuthorName) {
        String title = cleanCandidateTitle(extractCrossRefTitle(workNode));
        if (title == null) {
            return null;
        }

        Integer publicationYear = extractCrossRefYear(workNode);
        String doi = normalizeDoi(cleanText(workNode.path("DOI").asText(null)));
        String authorsText = extractCrossRefAuthorsText(workNode.path("author"), fallbackAuthorName);
        String externalUrl = cleanText(workNode.path("URL").asText(null));
        String indexation = extractCrossRefIndexation(workNode);
        PublicationType type = mapCrossRefPublicationType(cleanText(workNode.path("type").asText(null)));

        return new ImportCandidate(
                title,
                publicationYear,
                doi,
                authorsText,
                ImportSource.CROSSREF,
                externalUrl,
                null,
                indexation,
                type
        );
    }

    private ImportCandidate mapOpenAlexCandidate(JsonNode workNode, String fallbackAuthorName) {
        String title = cleanCandidateTitle(cleanText(workNode.path("display_name").asText(null)));
        if (title == null) {
            return null;
        }

        Integer publicationYear = workNode.path("publication_year").isInt()
                ? workNode.path("publication_year").asInt()
                : null;

        String doi = normalizeDoi(extractOpenAlexDoi(workNode));
        String authorsText = extractOpenAlexAuthorsText(workNode.path("authorships"), fallbackAuthorName);
        String externalUrl = extractOpenAlexExternalUrl(workNode);
        String indexation = extractOpenAlexIndexation(workNode);
        PublicationType type = mapOpenAlexPublicationType(
                cleanText(workNode.path("type_crossref").asText(null)),
                cleanText(workNode.path("type").asText(null))
        );

        return new ImportCandidate(
                title,
                publicationYear,
                doi,
                authorsText,
                ImportSource.OPENALEX,
                externalUrl,
                null,
                indexation,
                type
        );
    }

    private ImportCandidate mapDblpCandidate(DblpSearchService.DblpWork work, String fallbackAuthorName) {
        String title = cleanCandidateTitle(cleanText(work.title()));
        if (title == null) {
            return null;
        }

        return new ImportCandidate(
                title,
                work.publicationYear(),
                normalizeDoi(work.doi()),
                cleanText(work.authorsText()) != null ? cleanText(work.authorsText()) : fallbackAuthorName,
                ImportSource.DBLP,
                cleanText(work.externalUrl()),
                cleanText(work.pdfUrl()),
                cleanText(work.indexation()) != null ? cleanText(work.indexation()) : "DBLP",
                work.type() != null ? work.type() : PublicationType.OTHER
        );
    }

    private ImportCandidate mapResearchGateCandidate(
            ResearchGateSearchService.ResearchGateWork work,
            String fallbackAuthorName
    ) {
        String title = cleanCandidateTitle(cleanText(work.title()));
        if (title == null) {
            return null;
        }

        return new ImportCandidate(
                title,
                work.publicationYear(),
                normalizeDoi(work.doi()),
                cleanText(work.authorsText()) != null ? cleanText(work.authorsText()) : fallbackAuthorName,
                ImportSource.RESEARCHGATE,
                cleanText(work.externalUrl()),
                cleanText(work.pdfUrl()),
                cleanText(work.indexation()) != null ? cleanText(work.indexation()) : "ResearchGate",
                work.type() != null ? work.type() : PublicationType.OTHER
        );
    }

    private ImportCandidate mapGoogleScholarCandidate(
            GoogleScholarSearchService.GoogleScholarWork work,
            String fallbackAuthorName
    ) {
        String title = cleanCandidateTitle(cleanText(work.title()));
        if (title == null) {
            return null;
        }

        return new ImportCandidate(
                title,
                work.publicationYear(),
                normalizeDoi(work.doi()),
                cleanText(work.authorsText()) != null ? cleanText(work.authorsText()) : fallbackAuthorName,
                ImportSource.GOOGLE_SCHOLAR,
                cleanText(work.externalUrl()),
                cleanText(work.pdfUrl()),
                cleanText(work.indexation()) != null ? cleanText(work.indexation()) : "Google Scholar",
                work.type() != null ? work.type() : PublicationType.OTHER
        );
    }

    private boolean isTrustedDblpFallbackMatch(
            DblpSearchService.DblpWork work,
            String firstName,
            String lastName
    ) {
        if (work == null) {
            return false;
        }

        String title = cleanCandidateTitle(work.title());
        if (title == null || title.length() < 12) {
            return false;
        }

        Integer year = work.publicationYear();
        if (year != null) {
            int currentYear = java.time.Year.now().getValue();
            if (year < 1980 || year > currentYear + 1) {
                return false;
            }
        }

        if (!paperAuthorTextHasStrongAuthorMatch(work.authorsText(), firstName, lastName)) {
            return false;
        }

        String normalizedIndexation = normalize(work.indexation());
        String normalizedContext = normalize(work.contextText());

        return normalizedIndexation.contains("dblp")
                || normalizedContext.contains("dblp")
                || normalizedContext.contains("doi")
                || normalizedContext.contains("springer")
                || normalizedContext.contains("ieee")
                || normalizedContext.contains("aina")
                || normalizedContext.contains("iwcmc");
    }

    private boolean isTrustedResearchGateFallbackMatch(
            ResearchGateSearchService.ResearchGateWork work,
            String firstName,
            String lastName
    ) {
        if (work == null) {
            return false;
        }

        String title = cleanCandidateTitle(work.title());
        if (title == null || title.length() < 12) {
            return false;
        }

        Integer year = work.publicationYear();
        if (year != null) {
            int currentYear = java.time.Year.now().getValue();
            if (year < 1980 || year > currentYear + 1) {
                return false;
            }
        }

        if (!paperAuthorTextHasStrongAuthorMatch(work.contextText(), firstName, lastName)
                && !paperAuthorTextHasStrongAuthorMatch(work.authorsText(), firstName, lastName)) {
            return false;
        }

        String normalizedContext = normalize(Stream.of(
                        work.title(),
                        work.authorsText(),
                        work.externalUrl(),
                        work.indexation(),
                        work.contextText()
                )
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));

        boolean academicSourceSignal = normalizedContext.contains("researchgate")
                || normalizedContext.contains("doi")
                || normalizedContext.contains("springer")
                || normalizedContext.contains("ieee")
                || normalizedContext.contains("conference")
                || normalizedContext.contains("chapter")
                || normalizedContext.contains("journal");

        boolean institutionSignal = normalizedContext.contains("mediatron")
                || normalizedContext.contains("supcom")
                || normalizedContext.contains("sup com")
                || normalizedContext.contains("university of carthage")
                || normalizedContext.contains("carthage")
                || normalizedContext.contains("higher school of communication");

        return academicSourceSignal && institutionSignal;
    }

    private boolean paperAuthorTextHasStrongAuthorMatch(String authorsText, String firstName, String lastName) {
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

    private AuthorMatch resolveSemanticScholarAuthor(String firstName, String lastName, Set<String> candidateQueries) {
        AuthorMatch bestMatch = null;
        int bestScore = Integer.MIN_VALUE;

        for (String query : candidateQueries) {
            JsonNode authorsData = searchAuthors(query);
            if (authorsData == null || !authorsData.isArray() || authorsData.isEmpty()) {
                continue;
            }

            for (JsonNode authorNode : iterableOf(authorsData)) {
                String authorId = cleanText(authorNode.path("authorId").asText(null));
                String authorName = cleanText(authorNode.path("name").asText(null));

                if (authorId == null || authorName == null) {
                    continue;
                }

                if (!isStrongAuthorMatch(firstName, lastName, authorName, query)) {
                    continue;
                }

                int score = scoreAuthorName(firstName, lastName, authorName, query);
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = new AuthorMatch(authorId, query, authorName);
                }
            }
        }

        return bestMatch;
    }

    private PaperSearchMatch resolvePapersFromSearch(String firstName, String lastName, Set<String> candidateQueries) {
        com.fasterxml.jackson.databind.node.ArrayNode allMatchingPapers = JsonNodeFactory.instance.arrayNode();

        for (String query : candidateQueries) {
            JsonNode papersData = searchPapers(query);
            if (papersData == null || !papersData.isArray() || papersData.isEmpty()) {
                continue;
            }

            for (JsonNode paper : iterableOf(papersData)) {
                if (paperHasStrongAuthorMatch(paper.path("authors"), firstName, lastName)) {
                    allMatchingPapers.add(paper);
                }
            }
        }

        if (allMatchingPapers.isEmpty()) {
            return null;
        }

        return new PaperSearchMatch("aggregated", allMatchingPapers);
    }

    private Set<String> buildCandidateQueries(Member member, String manualQuery) {
        LinkedHashSet<String> queries = new LinkedHashSet<>();

        String firstName = cleanText(member.getFirstName());
        String lastName = cleanText(member.getLastName());
        String fullName = buildMemberFullName(member);
        String lab = cleanText(member.getLab());
        String orcidId = cleanText(member.getOrcidId());
        String email = cleanText(member.resolveEmail());
        String emailLocalPart = extractEmailLocalPart(email);
        String emailDomainHint = extractEmailDomainHint(email);

        addQuery(queries, manualQuery);
        addQuery(queries, fullName);
        addQuery(queries, quote(fullName));

        if (firstName != null && lastName != null) {
            String reversedName = lastName + " " + firstName;
            addQuery(queries, reversedName);
            addQuery(queries, quote(reversedName));

            if (!firstName.isBlank()) {
                String initial = firstName.substring(0, 1);
                addQuery(queries, initial + " " + lastName);
                addQuery(queries, lastName + " " + initial);
            }
        }

        addQuery(queries, orcidId);
        addQuery(queries, email);
        addQuery(queries, emailLocalPart);

        for (String labSignal : buildLabQuerySignals(member)) {
            if (!fullName.isBlank()) {
                addQuery(queries, fullName + " " + labSignal);
                addQuery(queries, quote(fullName) + " " + labSignal);
            }

            if (firstName != null && lastName != null) {
                String reversedName = lastName + " " + firstName;
                addQuery(queries, reversedName + " " + labSignal);

                if (!firstName.isBlank()) {
                    String initial = firstName.substring(0, 1);
                    addQuery(queries, initial + " " + lastName + " " + labSignal);
                    addQuery(queries, lastName + " " + initial + " " + labSignal);
                }
            }

            if (email != null) {
                addQuery(queries, email + " " + labSignal);
            }
            if (emailLocalPart != null) {
                addQuery(queries, emailLocalPart + " " + labSignal);
            }
        }

        if (emailDomainHint != null && !fullName.isBlank()) {
            addQuery(queries, fullName + " " + emailDomainHint);
            addQuery(queries, quote(fullName) + " " + emailDomainHint);
        }

        if (lab != null && !fullName.isBlank()) {
            addQuery(queries, fullName + " " + lab);

            if (firstName != null && lastName != null) {
                addQuery(queries, lastName + " " + firstName + " " + lab);
            }
        }

        for (String axisHint : extractResearchAxisHints(member.getResearchAxes())) {
            if (!fullName.isBlank()) {
                addQuery(queries, fullName + " " + axisHint + " MEDIATRON");
            }
        }

        return queries;
    }

    private String quote(String value) {
        String cleaned = cleanText(value);
        return cleaned == null ? null : "\"" + cleaned + "\"";
    }

    private String extractEmailLocalPart(String email) {
        String cleaned = cleanText(email);
        if (cleaned == null || !cleaned.contains("@")) {
            return null;
        }
        return cleanText(cleaned.substring(0, cleaned.indexOf('@')).replace('.', ' ').replace('_', ' '));
    }

    private String extractEmailDomainHint(String email) {
        String cleaned = cleanText(email);
        if (cleaned == null || !cleaned.contains("@")) {
            return null;
        }

        String domain = cleaned.substring(cleaned.indexOf('@') + 1)
                .replace('.', ' ')
                .replace('-', ' ');
        return cleanText(domain);
    }

    private Set<String> buildLabQuerySignals(Member member) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        addSignal(signals, member.getLab());
        addSignal(signals, "MEDIATRON");
        addSignal(signals, "Laboratoire MEDIATRON");
        addSignal(signals, "Lab MEDIATRON");
        addSignal(signals, "SUPCOM");
        addSignal(signals, "SUP'COM");
        addSignal(signals, "supcom.tn");
        addSignal(signals, extractEmailDomainHint(member.resolveEmail()));
        return signals;
    }

    private void addSignal(Set<String> signals, String value) {
        String cleaned = cleanText(value);
        if (cleaned != null) {
            signals.add(cleaned);
        }
    }

    private boolean hasMediatronSignal(Member member, String... values) {
        String joined = Stream.of(values)
                .map(this::cleanText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        String normalizedText = normalize(joined);

        if (normalizedText.isBlank()) {
            return false;
        }

        return buildLabQuerySignals(member).stream()
                .map(this::normalize)
                .filter(signal -> !signal.isBlank() && signal.length() >= 4)
                .anyMatch(normalizedText::contains);
    }

    private void addQuery(Set<String> queries, String value) {
        String cleaned = cleanText(value);
        if (cleaned != null) {
            queries.add(cleaned);
        }
    }

    private List<String> extractResearchAxisHints(Collection<String> researchAxes) {
        if (researchAxes == null || researchAxes.isEmpty()) {
            return List.of();
        }

        return researchAxes.stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Stream.of(value.split("[,;|/]")))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> value.length() >= 4)
                .distinct()
                .limit(3)
                .toList();
    }

    private JsonNode searchAuthors(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(SEMANTIC_SCHOLAR_BASE_URL + "/author/search")
                .queryParam("query", query)
                .queryParam("limit", AUTHOR_SEARCH_LIMIT)
                .queryParam("fields", "name,paperCount")
                .toUriString();

        JsonNode root = getSemanticScholarJson(url, "search authors on Semantic Scholar");
        return root != null ? root.path("data") : null;
    }

    private JsonNode searchPapers(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(SEMANTIC_SCHOLAR_BASE_URL + "/paper/search")
                .queryParam("query", query)
                .queryParam("limit", PAPER_LIMIT)
                .queryParam("fields", "title,year,authors,externalIds,url,venue,publicationTypes,abstract")
                .toUriString();

        JsonNode root = getSemanticScholarJson(url, "search papers on Semantic Scholar");
        return root != null ? root.path("data") : JsonNodeFactory.instance.arrayNode();
    }

    private JsonNode fetchAuthorPapers(String authorId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(SEMANTIC_SCHOLAR_BASE_URL + "/author/" + authorId + "/papers")
                .queryParam("limit", PAPER_LIMIT)
                .queryParam("fields", "paperId,title,year,authors,externalIds,url,venue,publicationTypes,abstract")
                .toUriString();

        JsonNode root = getSemanticScholarJson(url, "fetch papers from Semantic Scholar");
        return root == null ? JsonNodeFactory.instance.objectNode() : root;
    }

    private JsonNode getSemanticScholarJson(String url, String actionDescription) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            if (semanticScholarApiKey != null && !semanticScholarApiKey.isBlank()) {
                headers.set("x-api-key", semanticScholarApiKey.trim());
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 429) {
                throw new SemanticScholarRateLimitException("Semantic Scholar rate limit reached (429 Too Many Requests).");
            }

            throw new IllegalStateException(
                    "Failed to " + actionDescription + ": HTTP " + ex.getRawStatusCode()
                            + " - " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to " + actionDescription + ": " + ex.getMessage(), ex);
        }
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

    private boolean paperHasStrongAuthorMatch(JsonNode authorsNode, String firstName, String lastName) {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return false;
        }

        return StreamSupport.stream(authorsNode.spliterator(), false)
                .map(author -> cleanText(author.path("name").asText(null)))
                .filter(name -> name != null && !name.isBlank())
                .anyMatch(authorName -> isStrongPaperAuthorMatch(firstName, lastName, authorName));
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

    private String extractSemanticScholarAuthorsText(JsonNode authorsNode, String fallbackAuthorName) {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return fallbackAuthorName;
        }

        String authors = StreamSupport.stream(authorsNode.spliterator(), false)
                .map(author -> cleanText(author.path("name").asText(null)))
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return authors.isBlank() ? fallbackAuthorName : authors;
    }

    private String extractCrossRefAuthorsText(JsonNode authorsNode, String fallbackAuthorName) {
        if (authorsNode == null || !authorsNode.isArray() || authorsNode.isEmpty()) {
            return fallbackAuthorName;
        }

        String authors = StreamSupport.stream(authorsNode.spliterator(), false)
                .map(author -> {
                    String given = cleanText(author.path("given").asText(null));
                    String family = cleanText(author.path("family").asText(null));

                    if (given != null && family != null) {
                        return given + " " + family;
                    }
                    if (family != null) {
                        return family;
                    }
                    return given;
                })
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return authors.isBlank() ? fallbackAuthorName : authors;
    }

    private String extractOpenAlexAuthorsText(JsonNode authorshipsNode, String fallbackAuthorName) {
        if (authorshipsNode == null || !authorshipsNode.isArray() || authorshipsNode.isEmpty()) {
            return fallbackAuthorName;
        }

        String authors = StreamSupport.stream(authorshipsNode.spliterator(), false)
                .map(authorship -> cleanText(authorship.path("author").path("display_name").asText(null)))
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));

        return authors.isBlank() ? fallbackAuthorName : authors;
    }

    private String extractSemanticScholarIndexation(JsonNode paperNode) {
        String venue = cleanText(paperNode.path("venue").asText(null));
        return venue != null ? venue : "Semantic Scholar";
    }

    private String extractCrossRefIndexation(JsonNode workNode) {
        JsonNode containerTitleNode = workNode.path("container-title");
        if (containerTitleNode.isArray() && !containerTitleNode.isEmpty()) {
            String containerTitle = cleanText(containerTitleNode.get(0).asText(null));
            if (containerTitle != null) {
                return containerTitle;
            }
        }
        return "CrossRef";
    }

    private String extractOpenAlexIndexation(JsonNode workNode) {
        String sourceName = cleanText(
                workNode.path("primary_location")
                        .path("source")
                        .path("display_name")
                        .asText(null)
        );

        return sourceName != null ? sourceName : "OpenAlex";
    }

    private String extractCrossRefTitle(JsonNode workNode) {
        JsonNode titleNode = workNode.path("title");
        if (titleNode.isArray() && !titleNode.isEmpty()) {
            return cleanText(titleNode.get(0).asText(null));
        }
        return null;
    }

    private Integer extractCrossRefYear(JsonNode workNode) {
        JsonNode issuedNode = workNode.path("issued").path("date-parts");
        if (issuedNode.isArray() && !issuedNode.isEmpty()) {
            JsonNode firstDatePart = issuedNode.get(0);
            if (firstDatePart != null
                    && firstDatePart.isArray()
                    && !firstDatePart.isEmpty()
                    && firstDatePart.get(0).isInt()) {
                return firstDatePart.get(0).asInt();
            }
        }
        return null;
    }

    private String extractOpenAlexDoi(JsonNode workNode) {
        String doi = cleanText(workNode.path("doi").asText(null));

        if (doi == null) {
            doi = cleanText(workNode.path("ids").path("doi").asText(null));
        }

        if (doi == null) {
            return null;
        }

        return normalizeDoi(doi);
    }

    private String extractOpenAlexExternalUrl(JsonNode workNode) {
        String landingPage = cleanText(
                workNode.path("primary_location")
                        .path("landing_page_url")
                        .asText(null)
        );

        if (landingPage != null) {
            return landingPage;
        }

        return cleanText(workNode.path("id").asText(null));
    }

    private PublicationType mapSemanticScholarPublicationType(JsonNode publicationTypesNode) {
        if (publicationTypesNode != null && publicationTypesNode.isArray()) {
            for (JsonNode typeNode : iterableOf(publicationTypesNode)) {
                String rawType = cleanText(typeNode.asText(null));
                if (rawType == null) {
                    continue;
                }

                String normalized = rawType.toLowerCase(Locale.ROOT);

                if (normalized.contains("journal")) {
                    return PublicationType.JOURNAL;
                }

                if (normalized.contains("workshop")) {
                    return PublicationType.WORKSHOP;
                }

                if (normalized.contains("chapter") || normalized.contains("book")) {
                    return PublicationType.BOOK_CHAPTER;
                }

                if (normalized.contains("conference")) {
                    return PublicationType.CONFERENCE_C;
                }
            }
        }

        return PublicationType.OTHER;
    }

    private PublicationType mapCrossRefPublicationType(String rawType) {
        if (rawType == null) {
            return PublicationType.OTHER;
        }

        String normalized = rawType.toLowerCase(Locale.ROOT);

        if (normalized.contains("journal")) {
            return PublicationType.JOURNAL;
        }

        if (normalized.contains("proceedings-article") || normalized.contains("proceedings")) {
            return PublicationType.CONFERENCE_C;
        }

        if (normalized.contains("book-chapter") || normalized.contains("chapter")) {
            return PublicationType.BOOK_CHAPTER;
        }

        if (normalized.contains("posted-content") || normalized.contains("report")) {
            return PublicationType.WORKSHOP;
        }

        return PublicationType.OTHER;
    }

    private PublicationType mapOpenAlexPublicationType(String typeCrossref, String type) {
        String rawType = typeCrossref != null ? typeCrossref : type;

        if (rawType == null) {
            return PublicationType.OTHER;
        }

        String normalized = rawType.toLowerCase(Locale.ROOT);

        if (normalized.contains("journal")) {
            return PublicationType.JOURNAL;
        }

        if (normalized.contains("proceedings-article")
                || normalized.contains("conference")
                || normalized.contains("proceedings")) {
            return PublicationType.CONFERENCE_C;
        }

        if (normalized.contains("book-chapter")
                || normalized.contains("chapter")
                || normalized.contains("book")) {
            return PublicationType.BOOK_CHAPTER;
        }

        if (normalized.contains("report")
                || normalized.contains("posted-content")
                || normalized.contains("preprint")) {
            return PublicationType.WORKSHOP;
        }

        return PublicationType.OTHER;
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

    private String buildMemberFullName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private String buildNoMatchMessage(
            List<String> sourceMessages,
            boolean semanticScholarRateLimited,
            String semanticScholarError,
            String crossRefError,
            String openAlexError,
            String dblpError,
            String researchGateError,
            String googleScholarError
    ) {
        StringBuilder sb = new StringBuilder("No matching publications found after aggregation. ");

        sb.append("Sources checked: ").append(String.join(", ", sourceMessages)).append(". ");

        if (semanticScholarRateLimited) {
            sb.append("Semantic Scholar was rate-limited. ");
        }

        appendErrorIfPresent(sb, semanticScholarError);
        appendErrorIfPresent(sb, crossRefError);
        appendErrorIfPresent(sb, openAlexError);
        appendErrorIfPresent(sb, dblpError);
        appendErrorIfPresent(sb, researchGateError);
        appendErrorIfPresent(sb, googleScholarError);

        return sb.toString().trim();
    }

    private String buildNoNewPublicationsMessage(List<String> sourceMessages, int uniqueCount) {
        return "Aggregated import completed from all sources. Raw fetched by source: "
                + String.join(", ", sourceMessages)
                + ". Unique publications after deduplication: "
                + uniqueCount
                + ". All deduplicated publications were already scraped earlier for this member, so no new publications were added.";
    }

    private String buildSuccessMessage(List<String> sourceMessages, int uniqueCount, int unseenCount) {
        return "Aggregated import completed from all sources. Raw fetched by source: "
                + String.join(", ", sourceMessages)
                + ". Unique publications after deduplication: "
                + uniqueCount
                + ". New publications after excluding already-scraped member publications: "
                + unseenCount
                + ".";
    }

    private void appendErrorIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(" ");
        }
    }

    private String cleanErrorMessage(Exception ex) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "Unknown error";
        }

        return ex.getMessage().trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanCandidateTitle(String value) {
        String cleaned = cleanText(value);
        if (cleaned == null) {
            return null;
        }

        String result = cleaned
                .replaceFirst("^\\[[^\\]]+\\]\\s*", "")
                .replaceAll("^\"|\"$", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (result.isBlank()) {
            return null;
        }

        return result;
    }

    private boolean isInvalidPublicationTitle(
            String title,
            String authorsText,
            Integer publicationYear,
            ImportSource source,
            PublicationType type,
            String indexation
    ) {
        String normalized = normalize(title);

        if (normalized.isBlank()) {
            return true;
        }

        if (INVALID_TITLE_LABELS.contains(normalized)) {
            return true;
        }

        if (normalized.startsWith("author names ")
                || normalized.startsWith("cited by ")
                || normalized.startsWith("related articles ")) {
            return true;
        }

        if (normalized.length() < 4) {
            return true;
        }

        if (!normalized.matches(".*[a-z].*")) {
            return true;
        }

        if (looksLikeAuthorAffiliationBlob(title, authorsText, publicationYear, source, type, indexation)) {
            return true;
        }

        return false;
    }

    private boolean looksLikeAuthorAffiliationBlob(
            String title,
            String authorsText,
            Integer publicationYear,
            ImportSource source,
            PublicationType type,
            String indexation
    ) {
        String normalizedTitle = normalize(title);
        String normalizedAuthors = normalize(authorsText);

        int separatorCount = countSeparators(title);
        boolean manySeparators = separatorCount >= 4;

        boolean hasAffiliationKeyword = containsAnyKeyword(normalizedTitle, AFFILIATION_KEYWORDS);

        long shortTokenCount = tokensOf(normalizedTitle).stream()
                .filter(token -> token.length() <= 2)
                .count();

        boolean manyShortTokens = shortTokenCount >= 5;

        int sharedTokenCount = sharedTokenCount(normalizedTitle, normalizedAuthors);
        boolean strongAuthorOverlap = sharedTokenCount >= 3;

        boolean genericScholarCandidate =
                source == ImportSource.GOOGLE_SCHOLAR
                        && publicationYear == null
                        && (type == null || type == PublicationType.OTHER)
                        && isGenericIndexation(indexation);

        return manySeparators && (
                hasAffiliationKeyword
                        || manyShortTokens
                        || strongAuthorOverlap
                        || genericScholarCandidate
        );
    }

    private int countSeparators(String value) {
        if (value == null) {
            return 0;
        }

        int count = 0;
        for (char ch : value.toCharArray()) {
            if (ch == ',' || ch == ';' || ch == '·' || ch == '|') {
                count++;
            }
        }
        return count;
    }

    private boolean containsAnyKeyword(String normalizedText, Set<String> keywords) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }

        for (String keyword : keywords) {
            if (normalizedText.contains(normalize(keyword))) {
                return true;
            }
        }

        return false;
    }

    private List<String> tokensOf(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizedText.split("\\s+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private int sharedTokenCount(String normalizedTitle, String normalizedAuthors) {
        if (normalizedTitle == null || normalizedTitle.isBlank()
                || normalizedAuthors == null || normalizedAuthors.isBlank()) {
            return 0;
        }

        Set<String> titleTokens = new LinkedHashSet<>(tokensOf(normalizedTitle));
        Set<String> authorTokens = new LinkedHashSet<>(tokensOf(normalizedAuthors));

        titleTokens.removeIf(token -> token.length() <= 1);
        authorTokens.removeIf(token -> token.length() <= 1);

        titleTokens.retainAll(authorTokens);
        return titleTokens.size();
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

    private String chooseBetterTitle(String current, String incoming) {
        String a = cleanCandidateTitle(current);
        String b = cleanCandidateTitle(incoming);

        if (a == null) return b;
        if (b == null) return a;

        int aScore = titleQualityScore(a);
        int bScore = titleQualityScore(b);

        return bScore > aScore ? b : a;
    }

    private int titleQualityScore(String value) {
        String cleaned = cleanCandidateTitle(value);
        if (cleaned == null) {
            return -1;
        }

        int score = cleaned.length();

        if (cleaned.startsWith("[") || cleaned.contains("[C]")) {
            score -= 20;
        }

        if (cleaned.equals(cleaned.toUpperCase(Locale.ROOT))) {
            score -= 5;
        }

        return score;
    }

    private Integer chooseBetterYear(Integer current, Integer incoming) {
        if (current == null) return incoming;
        if (incoming == null) return current;
        return current;
    }

    private String chooseBetterDoi(String current, String incoming) {
        String a = normalizeDoi(current);
        String b = normalizeDoi(incoming);

        if (a == null) return b;
        if (b == null) return a;
        return a;
    }

    private String chooseBetterAuthors(
            String current,
            String incoming,
            ImportSource preferredSource,
            ImportSource currentSource,
            ImportSource incomingSource
    ) {
        return chooseBetterText(current, incoming, preferredSource, currentSource, incomingSource);
    }

    private String chooseBetterUrl(
            String current,
            String incoming,
            ImportSource preferredSource,
            ImportSource currentSource,
            ImportSource incomingSource
    ) {
        return chooseBetterText(current, incoming, preferredSource, currentSource, incomingSource);
    }

    private String chooseBetterIndexation(
            String current,
            String incoming,
            ImportSource preferredSource,
            ImportSource currentSource,
            ImportSource incomingSource
    ) {
        String a = cleanText(current);
        String b = cleanText(incoming);

        if (a == null) return b;
        if (b == null) return a;

        if (isGenericIndexation(a) && !isGenericIndexation(b)) {
            return b;
        }

        if (isGenericIndexation(b) && !isGenericIndexation(a)) {
            return a;
        }

        return chooseBetterText(a, b, preferredSource, currentSource, incomingSource);
    }

    private boolean isGenericIndexation(String value) {
        String normalized = normalize(value);
        return normalized.equals("google scholar")
                || normalized.equals("crossref")
                || normalized.equals("openalex")
                || normalized.equals("researchgate")
                || normalized.equals("dblp")
                || normalized.equals("semantic scholar");
    }

    private PublicationType chooseBetterType(
            PublicationType current,
            PublicationType incoming,
            ImportSource preferredSource,
            ImportSource currentSource,
            ImportSource incomingSource
    ) {
        if (current == null) return incoming;
        if (incoming == null) return current;

        if (current == PublicationType.OTHER && incoming != PublicationType.OTHER) {
            return incoming;
        }

        if (incoming == PublicationType.OTHER && current != PublicationType.OTHER) {
            return current;
        }

        return preferredSource == incomingSource ? incoming : current;
    }

    private String chooseBetterText(
            String current,
            String incoming,
            ImportSource preferredSource,
            ImportSource currentSource,
            ImportSource incomingSource
    ) {
        String a = cleanText(current);
        String b = cleanText(incoming);

        if (a == null) return b;
        if (b == null) return a;

        if (b.length() > a.length()) {
            return b;
        }

        if (preferredSource == incomingSource) {
            return b;
        }

        return a;
    }

    private ImportSource preferredSource(ImportSource first, ImportSource second) {
        return second.priority() > first.priority() ? second : first;
    }

    private Iterable<JsonNode> iterableOf(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return List.of();
        }

        return arrayNode::elements;
    }

    private enum ImportOutcome {
        IMPORTED,
        LINKED_EXISTING,
        SKIPPED
    }

    private enum ImportSource {
        CROSSREF("CROSSREF", "CrossRef", 6),
        DBLP("DBLP", "DBLP", 5),
        RESEARCHGATE("RESEARCHGATE", "ResearchGate", 4),
        OPENALEX("OPENALEX", "OpenAlex", 3),
        SEMANTIC_SCHOLAR("SEMANTIC_SCHOLAR", "Semantic Scholar", 2),
        GOOGLE_SCHOLAR("GOOGLE_SCHOLAR", "Google Scholar", 1);

        private final String dbValue;
        private final String displayName;
        private final int priority;

        ImportSource(String dbValue, String displayName, int priority) {
            this.dbValue = dbValue;
            this.displayName = displayName;
            this.priority = priority;
        }

        public String dbValue() {
            return dbValue;
        }

        public String displayName() {
            return displayName;
        }

        public int priority() {
            return priority;
        }
    }

    private record ImportCandidate(
            String title,
            Integer publicationYear,
            String doi,
            String authorsText,
            ImportSource source,
            String externalUrl,
            String pdfUrl,
            String indexation,
            PublicationType type
    ) {
    }

    private record SourcePullResult(
            List<ImportCandidate> candidates,
            int rawCount
    ) {
    }

    private record AuthorMatch(String authorId, String queryUsed, String authorName) {
    }

    private record PaperSearchMatch(String queryUsed, JsonNode papers) {
    }
}
