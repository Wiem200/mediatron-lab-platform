package com.Mediatron.Lab_Management.services;

import com.Mediatron.Lab_Management.data_dto.ChatbotMessageRequest;
import com.Mediatron.Lab_Management.data_dto.ChatbotMessageResponse;
import com.Mediatron.Lab_Management.data_models.Event;
import com.Mediatron.Lab_Management.data_models.EventStatus;
import com.Mediatron.Lab_Management.data_models.Member;
import com.Mediatron.Lab_Management.data_models.MemberPublication;
import com.Mediatron.Lab_Management.data_models.Project;
import com.Mediatron.Lab_Management.data_models.PhDStudent;
import com.Mediatron.Lab_Management.data_models.ProjectStatus;
import com.Mediatron.Lab_Management.data_models.Publication;
import com.Mediatron.Lab_Management.data_models.PublicationStatus;
import com.Mediatron.Lab_Management.repositories.EventRepository;
import com.Mediatron.Lab_Management.repositories.MemberPublicationRepository;
import com.Mediatron.Lab_Management.repositories.MemberRepository;
import com.Mediatron.Lab_Management.repositories.ProjectRepository;
import com.Mediatron.Lab_Management.repositories.PhDStudentRepository;
import com.Mediatron.Lab_Management.repositories.PublicationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.text.Normalizer;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final int MAX_PUBLICATIONS = 8;
    private static final int MAX_PROJECTS = 6;
    private static final int MAX_EVENTS = 6;
    private static final int MAX_MEMBERS = 6;
    private static final int MAX_PHD_STUDENTS = 6;

    private final PublicationRepository publicationRepository;
    private final ProjectRepository projectRepository;
    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final MemberPublicationRepository memberPublicationRepository;
    private final PhDStudentRepository phdStudentRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String geminiModel;

    @Value("${gemini.url-context-model:gemini-3-flash-preview}")
    private String geminiUrlContextModel;

    @Value("${app.frontend.public-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public ChatbotService(
            PublicationRepository publicationRepository,
            ProjectRepository projectRepository,
            EventRepository eventRepository,
            MemberRepository memberRepository,
            MemberPublicationRepository memberPublicationRepository,
            PhDStudentRepository phdStudentRepository,
            ObjectMapper objectMapper
    ) {
        this.publicationRepository = publicationRepository;
        this.projectRepository = projectRepository;
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
        this.memberPublicationRepository = memberPublicationRepository;
        this.phdStudentRepository = phdStudentRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @Transactional(readOnly = true)
    public ChatbotMessageResponse reply(ChatbotMessageRequest request, Authentication authentication) {
        String message = cleanText(request == null ? null : request.getMessage());
        boolean personalized = authentication != null && authentication.isAuthenticated();

        if (message == null) {
            return new ChatbotMessageResponse(
                    "Please type a question about MEDIATRON publications, members, projects, or events.",
                    personalized,
                    0,
                    defaultSuggestions()
            );
        }

        try {
            Optional<Member> currentMember = resolveCurrentMember(authentication);
            ChatbotContext context = buildContext(message, currentMember);

            Optional<String> directAnswer = buildDirectAnswer(message, context, currentMember.isPresent());
            if (directAnswer.isPresent()) {
                return new ChatbotMessageResponse(
                        directAnswer.get(),
                        currentMember.isPresent(),
                        context.sourceCount(),
                        buildSuggestions(currentMember.isPresent())
                );
            }

            String answer = null;
            PublicationSourceTarget sourceTarget = selectPublicationSourceTarget(message, context);

            if (sourceTarget != null) {
                String sourcePrompt = buildPublicationSourcePrompt(
                        message,
                        request.getHistory(),
                        sourceTarget.publication(),
                        sourceTarget.sourceUrl(),
                        currentMember.isPresent()
                );

                answer = cleanText(callGeminiWithUrlContext(sourcePrompt, sourceTarget.sourceUrl()).orElse(null));

                if (answer != null && !answer.toLowerCase(Locale.ROOT).contains("source used")) {
                    answer = answer + "\n\nSource used: " + sourceTarget.sourceUrl();
                }
            }

            if (answer == null) {
                String prompt = buildPrompt(message, request.getHistory(), context, currentMember.isPresent());
                answer = cleanText(callGemini(prompt).orElse(null));
            }

            if (answer == null) {
                answer = buildFallbackAnswer(message, context, currentMember.isPresent());
            }

            answer = sanitizePublicAnswer(answer, message);

            return new ChatbotMessageResponse(
                    answer,
                    currentMember.isPresent(),
                    context.sourceCount(),
                    buildSuggestions(currentMember.isPresent())
            );
        } catch (Exception ex) {
            log.warn("Chatbot failed while processing message. A safe public answer will be returned.", ex);
            return new ChatbotMessageResponse(
                    friendlyErrorMessage(message),
                    personalized,
                    0,
                    defaultSuggestions()
            );
        }
    }

    private ChatbotContext buildContext(String message, Optional<Member> currentMember) {
        String normalizedMessage = normalize(message);
        Set<String> terms = extractTerms(normalizedMessage);
        ChatbotContext context = new ChatbotContext();

        boolean asksPublications = containsAny(normalizedMessage, "publication", "publications", "paper", "papers", "article", "articles", "journal", "conference", "doi", "ml", "machine learning", "iot", "security");
        boolean asksMembers = containsAny(normalizedMessage, "member", "members", "membre", "membres", "researcher", "researchers", "chercheur", "chercheurs", "doctor", "dr", "professor", "professeur", "teacher", "enseignant", "who is", "qui est", "profile", "profil", "mr ", "ms ", "mrs ", "mme ", "m. ");
        boolean asksProjects = containsAny(normalizedMessage, "project", "projects", "projet", "projets", "research project", "github", "funding", "financement");
        boolean asksEvents = containsAny(normalizedMessage, "event", "events", "evenement", "evenements", "événement", "événements", "seminar", "seminars", "workshop", "workshops", "conference", "calendar", "agenda");
        boolean asksPhd = containsAny(normalizedMessage, "phd", "doctoral", "doctorant", "doctorants", "these", "thesis", "supervisor", "encadrant");
        boolean asksMine = containsAny(normalizedMessage, "my ", "mine", "me", "pending", "validate", "validation", "dashboard");

        if (!asksPublications && !asksMembers && !asksProjects && !asksEvents && !asksPhd && !asksMine) {
            asksPublications = true;
            asksMembers = true;
            asksProjects = true;
            asksEvents = true;
            asksPhd = true;
        }

        if (asksPublications) {
            context.publications.addAll(searchPublications(terms, normalizedMessage));
        }

        if (asksMembers) {
            context.members.addAll(searchMembers(terms, normalizedMessage));
        }

        if (asksProjects) {
            context.projects.addAll(searchProjects(terms, normalizedMessage));
        }

        if (asksEvents) {
            context.events.addAll(searchEvents(terms, normalizedMessage));
        }

        if (asksPhd) {
            context.phdStudents.addAll(searchPhdStudents(terms, normalizedMessage));
        }

        currentMember.ifPresent(member -> {
            if (asksMine || containsAny(normalizedMessage, "my publication", "pending", "validate", "profile", "my project")) {
                context.personalLines.addAll(buildPersonalContext(member));
            }
        });

        return context;
    }

    private List<Publication> searchPublications(Set<String> terms, String normalizedMessage) {
        List<Publication> approved = publicationRepository.findByStatusOrderByPublicationYearDescTitleAsc(PublicationStatus.APPROVED);

        return approved.stream()
                .map(publication -> new ScoredItem<>(publication, scorePublication(publication, terms, normalizedMessage)))
                .filter(item -> item.score() > 0 || containsAny(normalizedMessage, "publication", "publications", "paper", "papers", "article", "articles"))
                .sorted(Comparator.comparingInt((ScoredItem<Publication> item) -> item.score()).reversed()
                        .thenComparing(item -> nullSafe(item.item().getPublicationYear()), Comparator.reverseOrder())
                        .thenComparing(item -> nullSafe(item.item().getTitle()), String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_PUBLICATIONS)
                .map(item -> item.item())
                .toList();
    }

    private List<Member> searchMembers(Set<String> terms, String normalizedMessage) {
        List<Member> members = memberRepository.findVisibleLabMembersByOrderByName();

        return members.stream()
                .map(member -> new ScoredItem<>(member, scoreMember(member, terms, normalizedMessage)))
                .filter(item -> item.score() > 0 || containsAny(normalizedMessage, "member", "members", "researcher", "researchers", "who is", "profile"))
                .sorted(Comparator.comparingInt((ScoredItem<Member> item) -> item.score()).reversed()
                        .thenComparing(item -> buildMemberName(item.item()), String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_MEMBERS)
                .map(item -> item.item())
                .toList();
    }

    private List<Project> searchProjects(Set<String> terms, String normalizedMessage) {
        List<Project> projects = projectRepository.findByStatusOrderByTitleAsc(ProjectStatus.APPROVED);

        return projects.stream()
                .map(project -> new ScoredItem<>(project, scoreProject(project, terms, normalizedMessage)))
                .filter(item -> item.score() > 0 || containsAny(normalizedMessage, "project", "projects", "research project"))
                .sorted(Comparator.comparingInt((ScoredItem<Project> item) -> item.score()).reversed()
                        .thenComparing(item -> nullSafe(item.item().getTitle()), String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_PROJECTS)
                .map(item -> item.item())
                .toList();
    }

    private List<Event> searchEvents(Set<String> terms, String normalizedMessage) {
        List<Event> events = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED);

        return events.stream()
                .map(event -> new ScoredItem<>(event, scoreEvent(event, terms, normalizedMessage)))
                .filter(item -> item.score() > 0 || containsAny(normalizedMessage, "event", "events", "seminar", "seminars", "workshop", "workshops", "calendar", "agenda"))
                .sorted(Comparator.comparingInt((ScoredItem<Event> item) -> item.score()).reversed()
                        .thenComparing(item -> item.item().getEventDate(), Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_EVENTS)
                .map(item -> item.item())
                .toList();
    }

    private List<PhDStudent> searchPhdStudents(Set<String> terms, String normalizedMessage) {
        List<PhDStudent> students = phdStudentRepository.findAll();

        return students.stream()
                .map(student -> new ScoredItem<>(student, scorePhdStudent(student, terms, normalizedMessage)))
                .filter(item -> item.score() > 0 || containsAny(normalizedMessage, "phd", "doctoral", "doctorant", "doctorants", "these", "thesis"))
                .sorted(Comparator.comparingInt((ScoredItem<PhDStudent> item) -> item.score()).reversed()
                        .thenComparing(item -> buildPhdStudentName(item.item()), String.CASE_INSENSITIVE_ORDER))
                .limit(MAX_PHD_STUDENTS)
                .map(ScoredItem::item)
                .toList();
    }

    private int scorePublication(Publication publication, Set<String> terms, String normalizedMessage) {
        String text = normalize(joinText(
                publication.getTitle(),
                publication.getAuthorsText(),
                publication.getDoi(),
                publication.getSource(),
                publication.getIndexation(),
                publication.getType() == null ? null : publication.getType().name()
        ));

        int score = scoreText(text, terms);

        if (publication.getPublicationYear() != null && normalizedMessage.contains(String.valueOf(publication.getPublicationYear()))) {
            score += 5;
        }

        return score;
    }

    private int scoreMember(Member member, Set<String> terms, String normalizedMessage) {
        String text = normalize(joinText(
                member.getFirstName(),
                member.getLastName(),
                member.getGrade(),
                member.getBio(),
                member.getOrcidId(),
                member.resolveEmail(),
                String.join(" ", member.getResearchAxes())
        ));

        int score = scoreText(text, terms);
        String fullName = normalize(buildMemberName(member));

        if (!fullName.isBlank() && normalizedMessage.contains(fullName)) {
            score += 10;
        }

        return score;
    }

    private int scoreProject(Project project, Set<String> terms, String normalizedMessage) {
        String responsibleName = project.getResponsibleMember() == null ? null : buildMemberName(project.getResponsibleMember());
        String text = normalize(joinText(
                project.getTitle(),
                project.getDescription(),
                project.getKeywords(),
                project.getFunding(),
                project.getDuration(),
                responsibleName
        ));

        return scoreText(text, terms);
    }

    private int scoreEvent(Event event, Set<String> terms, String normalizedMessage) {
        String text = normalize(joinText(
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getResponsiblePerson(),
                event.getEventDate() == null ? null : event.getEventDate().toString()
        ));

        int score = scoreText(text, terms);

        if (containsAny(normalizedMessage, "upcoming", "next", "future") && event.getEventDate() != null && event.getEventDate().isAfter(LocalDateTime.now())) {
            score += 5;
        }

        return score;
    }

    private int scorePhdStudent(PhDStudent student, Set<String> terms, String normalizedMessage) {
        String supervisorName = student.getSupervisor() == null ? null : buildMemberName(student.getSupervisor());
        String text = normalize(joinText(
                student.getFirstName(),
                student.getLastName(),
                student.getThesisTopic(),
                student.getResearchTeam(),
                student.getCoSupervisor(),
                supervisorName,
                student.getRegistrationYear() == null ? null : student.getRegistrationYear().toString()
        ));

        int score = scoreText(text, terms);
        String fullName = normalize(buildPhdStudentName(student));
        if (!fullName.isBlank() && normalizedMessage.contains(fullName)) {
            score += 10;
        }
        return score;
    }

    private List<String> buildPersonalContext(Member member) {
        List<String> lines = new ArrayList<>();
        lines.add("Logged-in member profile: " + buildMemberName(member) + " | Grade: " + nullSafe(member.getGrade()) + " | Email: " + nullSafe(member.resolveEmail()));

        List<MemberPublication> pendingPublications = memberPublicationRepository.findAllForMemberByStatus(member.getId(), PublicationStatus.PENDING);
        List<MemberPublication> approvedPublications = memberPublicationRepository.findAllForMemberByStatus(member.getId(), PublicationStatus.APPROVED);

        lines.add("My pending publication validations: " + pendingPublications.size());
        pendingPublications.stream().limit(5).forEach(item -> lines.add("Pending publication: " + publicationLine(item.getPublication())));

        lines.add("My approved publications: " + approvedPublications.size());
        approvedPublications.stream().limit(5).forEach(item -> lines.add("Approved publication: " + publicationLine(item.getPublication())));

        if (!member.getResearchAxes().isEmpty()) {
            lines.add("My research axes: " + String.join(", ", member.getResearchAxes()));
        }

        lines.add("My publications dashboard: " + buildUrl("/my-publications"));

        return lines;
    }

    private PublicationSourceTarget selectPublicationSourceTarget(String userMessage, ChatbotContext context) {
        if (context == null || context.publications.isEmpty()) {
            return null;
        }

        String normalizedMessage = normalize(userMessage);

        boolean asksForSourceReading = containsAny(
                normalizedMessage,
                "explain",
                "summarize",
                "summary",
                "method",
                "methodology",
                "approach",
                "contribution",
                "conclusion",
                "result",
                "results",
                "findings",
                "abstract",
                "read",
                "according to",
                "what does",
                "what is",
                "how does",
                "why does",
                "source",
                "article",
                "paper"
        );

        if (!asksForSourceReading) {
            return null;
        }

        Publication bestPublication = context.publications.get(0);
        String sourceUrl = buildReadablePublicationSourceUrl(bestPublication);

        if (sourceUrl == null) {
            return null;
        }

        return new PublicationSourceTarget(bestPublication, sourceUrl);
    }

    private String buildPublicationSourcePrompt(
            String userMessage,
            List<ChatbotMessageRequest.HistoryMessage> history,
            Publication publication,
            String sourceUrl,
            boolean personalized
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are MEDIATRON Chatbot, a research assistant for the MEDIATRON laboratory portal.\n");
        prompt.append("The user is asking a detailed question about one MEDIATRON publication.\n");
        prompt.append("Use the publication source URL as the primary evidence when it is accessible. Also use the trusted MEDIATRON metadata below.\n");
        prompt.append("If the URL is paywalled, unavailable, or does not contain enough information, clearly say that the full article could not be accessed and answer only from the available metadata.\n");
        prompt.append("Do not invent claims that are not supported by the source URL or metadata. Keep the answer clear and useful.\n");
        prompt.append("The user is ").append(personalized ? "logged in." : "a public visitor.").append("\n\n");

        List<ChatbotMessageRequest.HistoryMessage> safeHistory = history == null ? List.of() : history;
        if (!safeHistory.isEmpty()) {
            prompt.append("Recent conversation, oldest to newest:\n");
            safeHistory.stream()
                    .skip(Math.max(0, safeHistory.size() - 10))
                    .forEach(item -> prompt.append("- ")
                            .append(cleanRole(item.getRole()))
                            .append(": ")
                            .append(shorten(cleanText(item.getContent()), 500))
                            .append("\n"));
            prompt.append("\n");
        }

        prompt.append("Trusted publication metadata:\n");
        prompt.append("- ID: ").append(publication.getId()).append("\n");
        prompt.append("- Title: ").append(nullSafe(publication.getTitle())).append("\n");
        prompt.append("- Authors: ").append(nullSafe(publication.getAuthorsText())).append("\n");
        prompt.append("- Year: ").append(nullSafe(publication.getPublicationYear())).append("\n");
        prompt.append("- DOI: ").append(nullSafe(publication.getDoi())).append("\n");
        prompt.append("- Source URL: ").append(sourceUrl).append("\n\n");

        prompt.append("User question:\n").append(userMessage).append("\n\n");
        prompt.append("Answer from the publication source and metadata:");

        return prompt.toString();
    }

    private String buildPrompt(
            String userMessage,
            List<ChatbotMessageRequest.HistoryMessage> history,
            ChatbotContext context,
            boolean personalized
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are MEDIATRON Assistant, a chatbot for the MEDIATRON research laboratory portal.\n");
        prompt.append("Answer using ONLY the trusted MEDIATRON context provided below. Do not invent publications, projects, events, members, or links.\n");
        prompt.append("If the context does not contain enough information, ask a short clarification question or say that you could not find matching MEDIATRON data.\n");
        prompt.append("Keep the answer helpful and concise. Use bullet points only when useful.\n");
        prompt.append("The user is ").append(personalized ? "logged in, so personal context may be included." : "a public visitor unless context says otherwise.").append("\n\n");

        List<ChatbotMessageRequest.HistoryMessage> safeHistory = history == null ? List.of() : history;
        if (!safeHistory.isEmpty()) {
            prompt.append("Recent conversation, oldest to newest:\n");
            safeHistory.stream()
                    .skip(Math.max(0, safeHistory.size() - 10))
                    .forEach(item -> prompt.append("- ")
                            .append(cleanRole(item.getRole()))
                            .append(": ")
                            .append(shorten(cleanText(item.getContent()), 500))
                            .append("\n"));
            prompt.append("\n");
        }

        prompt.append("Trusted MEDIATRON context:\n");
        prompt.append(context.toPromptContext());
        prompt.append("\nUser question:\n").append(userMessage).append("\n");
        prompt.append("\nAnswer:");

        return prompt.toString();
    }

    private Optional<String> callGemini(String prompt) {
        return callGemini(prompt, false, null, geminiModel);
    }

    private Optional<String> callGeminiWithUrlContext(String prompt, String sourceUrl) {
        return callGemini(prompt, true, sourceUrl, geminiUrlContextModel);
    }

    private Optional<String> callGemini(
            String prompt,
            boolean enableUrlContext,
            String sourceUrl,
            String preferredModel
    ) {
        String apiKey = cleanText(geminiApiKey);

        if (apiKey == null) {
            log.info("Gemini API key is not configured. Returning database fallback answer.");
            return Optional.empty();
        }

        try {
            String model = cleanText(preferredModel) == null ? "gemini-1.5-flash" : preferredModel.trim();
            if (model.startsWith("models/")) {
                model = model.substring("models/".length());
            }

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + URLEncoder.encode(model, StandardCharsets.UTF_8)
                    + ":generateContent?key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            String finalPrompt = prompt;
            if (enableUrlContext && cleanText(sourceUrl) != null && !prompt.contains(sourceUrl)) {
                finalPrompt = prompt + "\n\nPublication source URL to read: " + sourceUrl;
            }

            java.util.Map<String, Object> requestMap = new java.util.LinkedHashMap<>();
            requestMap.put(
                    "contents",
                    List.of(java.util.Map.of(
                            "role", "user",
                            "parts", List.of(java.util.Map.of("text", finalPrompt))
                    ))
            );
            requestMap.put(
                    "generationConfig",
                    java.util.Map.of(
                            "temperature", 0.15,
                            "maxOutputTokens", enableUrlContext ? 1100 : 700
                    )
            );

            if (enableUrlContext) {
                requestMap.put(
                        "tools",
                        List.of(java.util.Map.of("url_context", java.util.Map.of()))
                );
            }

            String requestBody = objectMapper.writeValueAsString(requestMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(enableUrlContext ? 18 : 8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn(
                        "Gemini request failed with status {} using model {}: {}",
                        response.statusCode(),
                        model,
                        shorten(response.body(), 500)
                );
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                return Optional.empty();
            }

            JsonNode parts = candidates.get(0).path("content").path("parts");

            if (!parts.isArray() || parts.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(cleanText(parts.get(0).path("text").asText(null)));
        } catch (Exception ex) {
            log.warn("Gemini chatbot call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> buildDirectAnswer(String message, ChatbotContext context, boolean personalized) {
        String normalizedMessage = normalize(message);
        boolean french = isFrenchQuestion(normalizedMessage);

        if (isGreetingOnly(normalizedMessage)) {
            return Optional.of(french
                    ? "Bonjour ! Je peux vous aider à trouver des informations sur les membres, publications, projets et événements du laboratoire MEDIATRON."
                    : "Hello! I can help you find MEDIATRON members, publications, research projects, and events.");
        }

        boolean asksCount = containsAny(
                normalizedMessage,
                "how many", "how much", "number of", "count", "total", "statistics", "stats", "combien", "nombre", "nbr", "statistique", "statistiques"
        );
        boolean asksPublications = containsAny(normalizedMessage, "publication", "publications", "paper", "papers", "article", "articles", "journal", "journals", "conference paper", "doi");
        boolean asksMembers = containsAny(normalizedMessage, "member", "members", "membre", "membres", "researcher", "researchers", "chercheur", "chercheurs", "who is", "who are", "qui est", "profile", "profil", "mr ", "ms ", "mrs ", "mme ", "m. ", "professor", "professeur", "teacher", "enseignant");
        boolean asksProjects = containsAny(normalizedMessage, "project", "projects", "projet", "projets", "research project", "github", "funding", "financement");
        boolean asksEvents = containsAny(normalizedMessage, "event", "events", "evenement", "evenements", "événement", "événements", "agenda", "calendar", "seminar", "workshop", "conference");
        boolean asksPhd = containsAny(normalizedMessage, "phd", "doctoral", "doctorant", "doctorants", "these", "thesis");
        boolean asksLabOverview = containsAny(normalizedMessage, "what is mediatron", "about mediatron", "presentation of mediatron", "mediatron lab", "mediatron laboratory", "c est quoi mediatron", "qu est ce que mediatron", "presentation de mediatron", "présentation de mediatron");
        boolean asksResearchAxes = containsAny(normalizedMessage, "research axes", "research axis", "axes de recherche", "axe de recherche", "research topics", "themes de recherche", "thèmes de recherche");

        if (asksLabOverview && !asksCount) {
            return Optional.of(buildLabOverviewAnswer(french));
        }

        if (asksResearchAxes && !asksCount && !hasSpecificMemberNameTerms(normalizedMessage)) {
            return Optional.of(buildGeneralResearchAxesAnswer(french));
        }

        if (asksCount) {
            int requestedCategories = countRequestedCategories(asksPublications, asksMembers, asksProjects, asksEvents) + (asksPhd ? 1 : 0);

            if (requestedCategories > 1 || (!asksPublications && !asksMembers && !asksProjects && !asksEvents && !asksPhd)) {
                return Optional.of(buildStatisticsSummaryAnswer(french, asksPublications, asksMembers, asksProjects, asksEvents, asksPhd));
            }

            Set<String> specificTerms = extractSpecificSearchTerms(normalizedMessage);

            if (asksPhd) {
                List<PhDStudent> students = phdStudentRepository.findAll();

                if (!specificTerms.isEmpty()) {
                    long count = students.stream()
                            .filter(student -> scorePhdStudent(student, specificTerms, normalizedMessage) > 0)
                            .count();
                    return Optional.of(french
                            ? "Le portail MEDIATRON contient actuellement " + count + " doctorant" + pluralFr(count) + " correspondant à : " + formatTerms(specificTerms) + "."
                            : "The MEDIATRON portal currently contains " + count + " PhD student" + plural(count) + " matching: " + formatTerms(specificTerms) + ".");
                }

                int count = students.size();
                return Optional.of(french
                        ? "Le portail MEDIATRON contient actuellement " + count + " doctorants."
                        : "The MEDIATRON portal currently contains " + count + " PhD students.");
            }

            if (asksMembers) {
                List<Member> visibleMembers = memberRepository.findVisibleLabMembersByOrderByName();

                if (!specificTerms.isEmpty()) {
                    long count = visibleMembers.stream()
                            .filter(member -> scoreMember(member, specificTerms, normalizedMessage) > 0)
                            .count();
                    return Optional.of(french
                            ? "Le laboratoire MEDIATRON contient actuellement " + count + " profil" + pluralFr(count) + " public" + pluralFr(count) + " correspondant à : " + formatTerms(specificTerms) + "."
                            : "The MEDIATRON laboratory currently has " + count + " public lab member profile" + plural(count) + " matching: " + formatTerms(specificTerms) + ".");
                }

                int count = visibleMembers.size();
                return Optional.of(french
                        ? "Le laboratoire MEDIATRON contient actuellement " + count + " profils publics de membres."
                        : "The MEDIATRON laboratory currently has " + count + " public lab member profiles.");
            }

            if (asksEvents) {
                List<Event> approvedEvents = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED);

                if (!specificTerms.isEmpty()) {
                    long count = approvedEvents.stream()
                            .filter(event -> scoreEvent(event, specificTerms, normalizedMessage) > 0)
                            .count();
                    return Optional.of(french
                            ? "L’agenda public MEDIATRON contient actuellement " + count + " événement" + pluralFr(count) + " approuvé" + pluralFr(count) + " correspondant à : " + formatTerms(specificTerms) + "."
                            : "The MEDIATRON public agenda currently contains " + count + " approved event" + plural(count) + " matching: " + formatTerms(specificTerms) + ".");
                }

                int count = approvedEvents.size();
                return Optional.of(french
                        ? "L’agenda public MEDIATRON contient actuellement " + count + " événement" + pluralFr(count) + " approuvé" + pluralFr(count) + "."
                        : "The MEDIATRON public agenda currently contains " + count + " approved event" + plural(count) + ".");
            }

            if (asksProjects) {
                List<Project> approvedProjects = projectRepository.findByStatusOrderByTitleAsc(ProjectStatus.APPROVED);

                if (!specificTerms.isEmpty()) {
                    long count = approvedProjects.stream()
                            .filter(project -> scoreProject(project, specificTerms, normalizedMessage) > 0)
                            .count();
                    return Optional.of(french
                            ? "Le répertoire public MEDIATRON contient actuellement " + count + " projet" + pluralFr(count) + " de recherche approuvé" + pluralFr(count) + " correspondant à : " + formatTerms(specificTerms) + "."
                            : "The MEDIATRON public directory currently contains " + count + " approved research project" + plural(count) + " matching: " + formatTerms(specificTerms) + ".");
                }

                int count = approvedProjects.size();
                return Optional.of(french
                        ? "Le répertoire public MEDIATRON contient actuellement " + count + " projet" + pluralFr(count) + " de recherche approuvé" + pluralFr(count) + "."
                        : "The MEDIATRON public directory currently contains " + count + " approved research project" + plural(count) + ".");
            }

            if (asksPublications) {
                Optional<Member> mentionedMember = resolveMentionedMember(normalizedMessage);
                if (mentionedMember.isPresent() && containsMemberPublicationOwnershipPhrase(normalizedMessage)) {
                    Member member = mentionedMember.get();
                    long count = approvedPublicationsForMember(member).size();
                    return Optional.of(french
                            ? buildMemberName(member) + " possède actuellement " + count + " publication" + pluralFr(count) + " approuvée" + pluralFr(count) + " liée" + pluralFr(count) + " dans le portail MEDIATRON."
                            : buildMemberName(member) + " currently has " + count + " approved linked publication" + plural(count) + " in the MEDIATRON portal.");
                }

                List<Publication> approvedPublications = publicationRepository.findByStatusOrderByPublicationYearDescTitleAsc(PublicationStatus.APPROVED);

                if (!specificTerms.isEmpty()) {
                    long count = approvedPublications.stream()
                            .filter(publication -> scorePublication(publication, specificTerms, normalizedMessage) > 0)
                            .count();
                    return Optional.of(french
                            ? "Le portail MEDIATRON contient actuellement " + count + " publication" + pluralFr(count) + " approuvée" + pluralFr(count) + " correspondant à : " + formatTerms(specificTerms) + "."
                            : "The MEDIATRON portal currently contains " + count + " approved publication" + plural(count) + " matching: " + formatTerms(specificTerms) + ".");
                }

                int count = approvedPublications.size();
                return Optional.of(french
                        ? "Le portail MEDIATRON contient actuellement " + count + " publications approuvées."
                        : "The MEDIATRON portal currently contains " + count + " approved publications.");
            }
        }

        if (asksNextEvent(normalizedMessage)) {
            return Optional.of(buildNextEventAnswer(french));
        }

        Optional<Member> mentionedMember = resolveMentionedMember(normalizedMessage);
        if (asksWhoIsMember(normalizedMessage)
                || (asksMembers && hasSpecificIdentityTerms(normalizedMessage) && mentionedMember.isPresent())
                || (!asksPublications && !asksProjects && !asksEvents && hasSpecificIdentityTerms(normalizedMessage) && mentionedMember.isPresent())) {
            if (mentionedMember.isEmpty()) {
                return Optional.of(french
                        ? "Je n’ai pas trouvé de profil membre MEDIATRON correspondant à ce nom. Vérifiez l’orthographe ou essayez une autre partie du nom."
                        : "I could not find a matching MEDIATRON member profile for that name. Please check the spelling or try another part of the name.");
            }

            return Optional.of(buildMemberProfileAnswer(mentionedMember.get(), french));
        }

        if (isListQuestion(normalizedMessage)) {
            Optional<String> listAnswer = buildListAnswer(normalizedMessage, context, french, asksMembers, asksPublications, asksProjects, asksEvents, asksPhd);
            if (listAnswer.isPresent()) {
                return listAnswer;
            }
        }

        if (asksEvents && !context.events.isEmpty() && containsAny(normalizedMessage, "when", "date", "schedule", "program", "quand", "programme")) {
            return Optional.of(buildEventAnswer(context.events.get(0), french));
        }

        return Optional.empty();
    }

    private Set<String> extractSpecificSearchTerms(String normalizedMessage) {
        Set<String> ignored = Set.of(
                "the", "and", "for", "with", "about", "show", "give", "tell", "what", "who", "where", "when",
                "how", "much", "many", "total", "count", "number", "mediatron", "laboratory", "lab", "please",
                "from", "that", "this", "you", "are", "can", "there", "related", "relates", "relating", "linked",
                "concerning", "regarding", "topic", "subject", "sur", "concernant", "lie", "lies", "liee", "liees",
                "member", "members", "membre", "membres", "researcher", "researchers", "chercheur", "chercheurs",
                "publication", "publications", "paper", "papers", "project", "projects", "projet", "projets",
                "event", "events", "evenement", "evenements", "agenda", "phd", "doctoral", "doctorant", "doctorants", "these", "thesis", "combien", "nombre", "nbr", "stat", "stats",
                "does", "have", "has", "having", "possede", "possède", "avoir", "avec", "dans", "pour", "par"
        );

        return extractTerms(normalizedMessage).stream()
                .filter(term -> !ignored.contains(term))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean hasSpecificMemberNameTerms(String normalizedMessage) {
        Set<String> terms = extractSpecificSearchTerms(normalizedMessage);

        if (terms.isEmpty()) {
            return false;
        }

        return memberRepository.findVisibleLabMembersByOrderByName().stream()
                .anyMatch(member -> scoreMember(member, terms, normalizedMessage) > 0);
    }

    private String formatTerms(Set<String> terms) {
        if (terms == null || terms.isEmpty()) {
            return "the requested topic";
        }

        return String.join(", ", terms);
    }

    private String buildNextEventAnswer() {
        return buildNextEventAnswer(false);
    }

    private String buildNextEventAnswer(boolean french) {
        List<Event> approvedEvents = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED);
        LocalDateTime now = LocalDateTime.now();

        Optional<Event> nextEvent = approvedEvents.stream()
                .filter(event -> event.getEventDate() != null)
                .filter(event -> !event.getEventDate().isBefore(now))
                .findFirst();

        if (nextEvent.isEmpty()) {
            return french
                    ? "Je n’ai trouvé aucun événement MEDIATRON approuvé à venir dans l’agenda public pour le moment."
                    : "I could not find an upcoming approved MEDIATRON event in the public agenda right now.";
        }

        return french
                ? "Le prochain événement MEDIATRON approuvé est :\n- " + eventLine(nextEvent.get())
                : "The next approved MEDIATRON event is:\n- " + eventLine(nextEvent.get());
    }

    private String buildEventAnswer(Event event) {
        return buildEventAnswer(event, false);
    }

    private String buildEventAnswer(Event event, boolean french) {
        return french
                ? "Voici l’événement MEDIATRON correspondant :\n- " + eventLine(event)
                : "Here is the matching MEDIATRON event I found:\n- " + eventLine(event);
    }

    private String buildMemberProfileAnswer(Member member) {
        return buildMemberProfileAnswer(member, false);
    }

    private String buildMemberProfileAnswer(Member member, boolean french) {
        StringBuilder answer = new StringBuilder();
        answer.append(buildMemberName(member));

        if (french) {
            answer.append(" est un membre du laboratoire MEDIATRON");
            if (cleanText(member.getGrade()) != null) {
                answer.append(" avec le grade : ").append(member.getGrade());
            }
            answer.append(".\n");
        } else {
            answer.append(" is a MEDIATRON lab member");
            if (cleanText(member.getGrade()) != null) {
                answer.append(" with the grade: ").append(member.getGrade());
            }
            answer.append(".\n");
        }

        if (cleanText(member.getBio()) != null) {
            answer.append(french ? "\nBiographie : " : "\nBiography: ")
                    .append(shorten(member.getBio(), 650))
                    .append("\n");
        }

        if (!member.getResearchAxes().isEmpty()) {
            answer.append(french ? "\nAxes de recherche : " : "\nResearch axes: ")
                    .append(String.join(", ", member.getResearchAxes()))
                    .append(".\n");
        }

        List<Publication> publications = approvedPublicationsForMember(member);
        answer.append(french ? "\nPublications approuvées liées : " : "\nApproved linked publications: ")
                .append(publications.size())
                .append(".\n");
        publications.stream().limit(3).forEach(publication -> answer.append("- ").append(publicationLine(publication)).append("\n"));

        if (cleanText(member.resolveEmail()) != null) {
            answer.append(french ? "\nEmail public : " : "\nPublic email: ")
                    .append(member.resolveEmail())
                    .append("\n");
        }

        answer.append(french ? "Profil : " : "Profile: ").append(buildUrl("/members/" + member.getId()));

        return answer.toString().trim();
    }

    private int countRequestedCategories(boolean asksPublications, boolean asksMembers, boolean asksProjects, boolean asksEvents) {
        int count = 0;
        if (asksPublications) count++;
        if (asksMembers) count++;
        if (asksProjects) count++;
        if (asksEvents) count++;
        return count;
    }

    private String buildStatisticsSummaryAnswer(boolean french, boolean asksPublications, boolean asksMembers, boolean asksProjects, boolean asksEvents, boolean asksPhd) {
        boolean includeAll = !asksPublications && !asksMembers && !asksProjects && !asksEvents && !asksPhd;
        StringBuilder answer = new StringBuilder(french ? "Résumé public MEDIATRON :\n" : "MEDIATRON public summary:\n");

        if (includeAll || asksMembers) {
            int count = memberRepository.findVisibleLabMembersByOrderByName().size();
            answer.append(french ? "- Membres publics : " : "- Public lab members: ").append(count).append("\n");
        }
        if (includeAll || asksPublications) {
            int count = publicationRepository.findByStatusOrderByPublicationYearDescTitleAsc(PublicationStatus.APPROVED).size();
            answer.append(french ? "- Publications approuvées : " : "- Approved publications: ").append(count).append("\n");
        }
        if (includeAll || asksProjects) {
            int count = projectRepository.findByStatusOrderByTitleAsc(ProjectStatus.APPROVED).size();
            answer.append(french ? "- Projets de recherche approuvés : " : "- Approved research projects: ").append(count).append("\n");
        }
        if (includeAll || asksEvents) {
            int count = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.APPROVED).size();
            answer.append(french ? "- Événements approuvés : " : "- Approved events: ").append(count).append("\n");
        }

        return answer.toString().trim();
    }

    private Optional<Member> resolveMentionedMember(String normalizedMessage) {
        Set<String> terms = extractIdentitySearchTerms(normalizedMessage);
        if (terms.isEmpty()) {
            return Optional.empty();
        }

        return memberRepository.findVisibleLabMembersByOrderByName().stream()
                .map(member -> new ScoredItem<>(member, scoreMember(member, terms, normalizedMessage) + fuzzyNameScore(member, terms)))
                .filter(item -> item.score() >= 3)
                .sorted(Comparator.comparingInt((ScoredItem<Member> item) -> item.score()).reversed()
                        .thenComparing(item -> buildMemberName(item.item()), String.CASE_INSENSITIVE_ORDER))
                .map(ScoredItem::item)
                .findFirst();
    }

    private Set<String> extractIdentitySearchTerms(String normalizedMessage) {
        Set<String> ignored = Set.of(
                "who", "is", "are", "mr", "ms", "mrs", "mme", "doctor", "dr", "prof", "professor", "professeur",
                "member", "members", "membre", "membres", "profile", "profil", "chercheur", "chercheurs", "researcher", "researchers",
                "publication", "publications", "paper", "papers", "article", "articles", "project", "projects", "event", "events",
                "how", "many", "much", "count", "number", "total", "does", "have", "has", "tell", "show", "give", "about", "mediatron", "lab", "laboratory",
                "qui", "est", "combien", "nombre", "de", "des", "du", "la", "le", "les", "un", "une", "avec", "pour", "sur", "par"
        );

        return extractTerms(normalizedMessage).stream()
                .filter(term -> !ignored.contains(term))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int fuzzyNameScore(Member member, Set<String> terms) {
        String name = normalize(buildMemberName(member));
        Set<String> nameParts = extractTerms(name);
        int score = 0;

        for (String term : terms) {
            for (String namePart : nameParts) {
                if (namePart.equals(term)) {
                    score += 6;
                } else if (term.length() >= 4 && namePart.length() >= 4 && levenshteinDistance(term, namePart) <= 2) {
                    score += 3;
                }
            }
        }

        return score;
    }

    private int levenshteinDistance(String first, String second) {
        if (first == null || second == null) {
            return 99;
        }

        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];

        for (int j = 0; j <= second.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= first.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[second.length()];
    }

    private boolean containsMemberPublicationOwnershipPhrase(String normalizedMessage) {
        return containsAny(normalizedMessage, "publication", "publications", "paper", "papers", "article", "articles")
                && containsAny(normalizedMessage, "does", "has", "have", "by", "from", "for", "de", "des", "par", "pour", "possede", "possède");
    }

    private boolean isListQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage, "show", "list", "give me", "display", "find", "search", "montre", "affiche", "liste", "chercher", "recherche");
    }

    private Optional<String> buildListAnswer(String normalizedMessage, ChatbotContext context, boolean french, boolean asksMembers, boolean asksPublications, boolean asksProjects, boolean asksEvents, boolean asksPhd) {
        if (asksMembers && !context.members.isEmpty()) {
            StringBuilder answer = new StringBuilder(french ? "Membres MEDIATRON correspondants :\n" : "Matching MEDIATRON members:\n");
            context.members.stream().limit(6).forEach(member -> answer.append("- ").append(memberLine(member)).append("\n"));
            return Optional.of(answer.toString().trim());
        }

        if (asksPublications && !context.publications.isEmpty()) {
            StringBuilder answer = new StringBuilder(french ? "Publications MEDIATRON correspondantes :\n" : "Matching MEDIATRON publications:\n");
            context.publications.stream().limit(5).forEach(publication -> answer.append("- ").append(publicationLine(publication)).append("\n"));
            return Optional.of(answer.toString().trim());
        }

        if (asksProjects && !context.projects.isEmpty()) {
            StringBuilder answer = new StringBuilder(french ? "Projets MEDIATRON correspondants :\n" : "Matching MEDIATRON projects:\n");
            context.projects.stream().limit(5).forEach(project -> answer.append("- ").append(projectLine(project)).append("\n"));
            return Optional.of(answer.toString().trim());
        }

        if (asksEvents && !context.events.isEmpty()) {
            StringBuilder answer = new StringBuilder(french ? "Événements MEDIATRON correspondants :\n" : "Matching MEDIATRON events:\n");
            context.events.stream().limit(5).forEach(event -> answer.append("- ").append(eventLine(event)).append("\n"));
            return Optional.of(answer.toString().trim());
        }

        return Optional.empty();
    }

    private String buildLabOverviewAnswer(boolean french) {
        if (french) {
            return "MEDIATRON est un laboratoire de recherche de SUP’COM / Université de Carthage spécialisé dans les technologies des médias, les réseaux radio-mobiles multimédias, les systèmes intelligents, l’IoT, l’Edge Computing, la 5G/6G et la sécurité. Le portail MEDIATRON présente ses membres, publications, projets et événements.";
        }

        return "MEDIATRON is a SUP’COM / University of Carthage research laboratory focused on media technologies, multimedia radio-mobile networks, intelligent systems, IoT, Edge Computing, 5G/6G, and security. The MEDIATRON portal presents its members, publications, projects, and events.";
    }

    private String buildGeneralResearchAxesAnswer(boolean french) {
        if (french) {
            return "Les axes de recherche MEDIATRON couvrent notamment les réseaux radio-mobiles intelligents, les services intelligents, la sécurité, l’IoT, l’Edge Computing, la 5G/6G, la virtualisation, l’O-RAN, le Network Slicing, l’IA et le Digital Twin.";
        }

        return "MEDIATRON research axes include intelligent radio-mobile networks, intelligent services, security, IoT, Edge Computing, 5G/6G, virtualization, O-RAN, Network Slicing, AI, and Digital Twin.";
    }

    private boolean isFrenchQuestion(String normalizedMessage) {
        return containsAny(normalizedMessage, "bonjour", "salut", "merci", "combien", "qui ", "quoi", "quand", "projet", "projets", "membre", "membres", "evenement", "événement", "chercheur", "professeur", "enseignant", "axe de recherche", "axes de recherche", "these", "thèse");
    }

    private boolean isGreetingOnly(String normalizedMessage) {
        Set<String> terms = extractTerms(normalizedMessage);
        if (terms.isEmpty()) {
            return false;
        }

        return terms.stream().allMatch(term -> Set.of("hello", "hi", "hey", "bonjour", "salut", "merci", "thanks", "thank").contains(term));
    }

    private List<Publication> approvedPublicationsForMember(Member member) {
        if (member == null || member.getId() == null) {
            return List.of();
        }

        return publicationRepository.findByMembers_IdOrderByPublicationYearDescCreatedAtDesc(member.getId())
                .stream()
                .filter(publication -> publication.getStatus() == PublicationStatus.APPROVED)
                .toList();
    }

    private boolean asksNextEvent(String normalizedMessage) {
        return containsAny(
                normalizedMessage,
                "next event", "upcoming event", "future event", "prochain evenement", "prochain événement", "evenement prochain", "événement prochain"
        ) || (containsAny(normalizedMessage, "when", "quand") && containsAny(normalizedMessage, "event", "events", "evenement", "evenements", "événement", "événements"));
    }

    private boolean asksWhoIsMember(String normalizedMessage) {
        return containsAny(normalizedMessage, "who is", "who are", "qui est", "c'est qui", "c est qui", "mr ", "ms ", "mrs ", "mme ", "m. ")
                && hasSpecificIdentityTerms(normalizedMessage);
    }

    private boolean hasSpecificIdentityTerms(String normalizedMessage) {
        return !extractIdentitySearchTerms(normalizedMessage).isEmpty();
    }

    private String sanitizePublicAnswer(String answer, String originalMessage) {
        String cleaned = cleanText(answer);

        if (cleaned == null || looksTechnical(cleaned)) {
            return friendlyErrorMessage(originalMessage);
        }

        return cleaned;
    }

    private boolean looksTechnical(String value) {
        String normalized = normalize(value);
        return containsAny(
                normalized,
                "sqlexception", "invaliddataaccess", "jdbc", "hibernate", "org.springframework", "java.",
                "stack trace", "syntax error", "select ", " from ", " where ", "internal server error"
        );
    }

    private String friendlyErrorMessage(String userMessage) {
        String normalized = normalize(userMessage);

        if (containsAny(normalized, "bonjour", "salut", "merci", "combien", "qui", "quoi", "quand", "publication", "membre", "projet", "evenement", "événement")) {
            return "Désolé, je n’ai pas pu traiter cette demande pour le moment. Veuillez reformuler votre question ou réessayer dans quelques secondes.";
        }

        return "Sorry, I could not process that request right now. Please rephrase your question or try again in a few seconds.";
    }

    private String plural(long count) {
        return count == 1 ? "" : "s";
    }

    private String pluralFr(long count) {
        return count == 1 ? "" : "s";
    }

    private String buildFallbackAnswer(String message, ChatbotContext context, boolean personalized) {
        if (context.sourceCount() == 0) {
            return "I could not find matching MEDIATRON data for that question. Try asking about publications, members, research projects, events, or your own publications if you are signed in.";
        }

        StringBuilder answer = new StringBuilder();

        if (!context.personalLines.isEmpty()) {
            answer.append("Here is what I found for your account:\n");
            context.personalLines.stream().limit(8).forEach(line -> answer.append("- ").append(line).append("\n"));
            answer.append("\n");
        }

        if (!context.publications.isEmpty()) {
            answer.append("Matching publications:\n");
            context.publications.stream().limit(4).forEach(publication -> answer.append("- ").append(publicationLine(publication)).append("\n"));
            answer.append("\n");
        }

        if (!context.members.isEmpty()) {
            answer.append("Matching members:\n");
            context.members.stream().limit(4).forEach(member -> answer.append("- ").append(memberLine(member)).append("\n"));
            answer.append("\n");
        }

        if (!context.projects.isEmpty()) {
            answer.append("Matching projects:\n");
            context.projects.stream().limit(4).forEach(project -> answer.append("- ").append(projectLine(project)).append("\n"));
            answer.append("\n");
        }

        if (!context.events.isEmpty()) {
            answer.append("Matching events:\n");
            context.events.stream().limit(4).forEach(event -> answer.append("- ").append(eventLine(event)).append("\n"));
        }

        return answer.toString().trim();
    }

    private Optional<Member> resolveCurrentMember(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }

        return memberRepository.findByUserEmail(authentication.getName().trim());
    }

    private int scoreText(String text, Set<String> terms) {
        if (text == null || text.isBlank() || terms.isEmpty()) {
            return 0;
        }

        int score = 0;

        for (String term : terms) {
            if (text.contains(term)) {
                score += term.length() >= 5 ? 3 : 1;
            }
        }

        return score;
    }

    private Set<String> extractTerms(String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return Set.of();
        }

        return List.of(normalizedMessage.split("[^a-z0-9]+"))
                .stream()
                .map(String::trim)
                .filter(value -> value.length() >= 3 || (value.length() >= 2 && value.chars().anyMatch(Character::isDigit)))
                .filter(value -> !isStopWord(value))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isStopWord(String value) {
        return Set.of(
                "the", "and", "for", "with", "about", "show", "give", "tell", "what", "who", "where", "when",
                "how", "much", "many", "total", "count", "number", "mediatron", "laboratory", "lab", "please", "from", "that", "this", "you", "are", "can",
                "mr", "ms", "mrs", "mme", "qui", "est", "quoi", "quand", "combien", "nombre", "membre", "membres"
        ).contains(value);
    }

    private boolean containsAny(String value, String... needles) {
        String normalized = normalize(value);

        for (String needle : needles) {
            if (normalized.contains(normalize(needle))) {
                return true;
            }
        }

        return false;
    }

    private String publicationLine(Publication publication) {
        String year = publication.getPublicationYear() == null ? "year unknown" : publication.getPublicationYear().toString();
        String url = buildPublicationUrl(publication);

        return nullSafe(publication.getTitle())
                + " (" + year + ")"
                + " | Authors: " + nullSafe(publication.getAuthorsText())
                + " | Link: " + url;
    }

    private String memberLine(Member member) {
        return buildMemberName(member)
                + " | Grade: " + nullSafe(member.getGrade())
                + " | Research axes: " + (member.getResearchAxes().isEmpty() ? "not specified" : String.join(", ", member.getResearchAxes()))
                + " | Profile: " + buildUrl("/members/" + member.getId());
    }

    private String projectLine(Project project) {
        return nullSafe(project.getTitle())
                + " | Lead: " + (project.getResponsibleMember() == null ? "not assigned" : buildMemberName(project.getResponsibleMember()))
                + " | Link: " + buildUrl("/research-projects/" + project.getId());
    }

    private String eventLine(Event event) {
        return nullSafe(event.getTitle())
                + " | Date: " + formatDate(event.getEventDate())
                + " | Location: " + nullSafe(event.getLocation())
                + " | Link: " + buildUrl("/lab-events");
    }

    private String phdStudentLine(PhDStudent student) {
        String supervisor = student.getSupervisor() == null ? "not assigned" : buildMemberName(student.getSupervisor());
        return buildPhdStudentName(student)
                + " | Thesis: " + nullSafe(student.getThesisTopic())
                + " | Registration year: " + nullSafe(student.getRegistrationYear())
                + " | Supervisor: " + supervisor;
    }

    private String buildPhdStudentName(PhDStudent student) {
        String firstName = student.getFirstName() == null ? "" : student.getFirstName().trim();
        String lastName = student.getLastName() == null ? "" : student.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "PhD student #" + student.getId() : fullName;
    }

    private String buildReadablePublicationSourceUrl(Publication publication) {
        String pdf = cleanText(publication.getPdfUrl());

        if (pdf != null) {
            return pdf;
        }

        String external = cleanText(publication.getExternalUrl());

        if (external != null) {
            return external;
        }

        String doi = cleanText(publication.getDoi());

        if (doi != null) {
            if (doi.startsWith("http://") || doi.startsWith("https://")) {
                return doi;
            }
            return "https://doi.org/" + doi;
        }

        return null;
    }

    private String buildPublicationUrl(Publication publication) {
        String external = cleanText(publication.getExternalUrl());

        if (external != null) {
            return external;
        }

        String doi = cleanText(publication.getDoi());

        if (doi != null) {
            if (doi.startsWith("http://") || doi.startsWith("https://")) {
                return doi;
            }
            return "https://doi.org/" + doi;
        }

        return buildUrl("/publications?search=" + encode(publication.getTitle()));
    }

    private String buildMemberName(Member member) {
        String firstName = member.getFirstName() == null ? "" : member.getFirstName().trim();
        String lastName = member.getLastName() == null ? "" : member.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "Member #" + member.getId() : fullName;
    }

    private String buildUrl(String path) {
        String base = cleanText(frontendBaseUrl) == null ? "http://localhost:5173" : frontendBaseUrl.trim();

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        if (path == null || path.isBlank()) {
            return base;
        }

        return path.startsWith("/") ? base + path : base + "/" + path;
    }

    private List<String> buildSuggestions(boolean personalized) {
        return List.of();
    }

    private List<String> defaultSuggestions() {
        return List.of();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return withoutAccents.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replace('`', '\'')
                .replace('‐', '-')
                .replace('‑', '-')
                .replace('–', '-')
                .replace('—', '-')
                .trim();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private String cleanRole(String value) {
        String role = normalize(value);
        return "assistant".equals(role) ? "assistant" : "user";
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String nullSafe(Object value) {
        return value == null ? "not available" : value.toString();
    }

    private String joinText(String... values) {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                builder.append(value).append(' ');
            }
        }

        return builder.toString();
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "not scheduled";
        }

        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record ScoredItem<T>(T item, int score) {
    }

    private record PublicationSourceTarget(Publication publication, String sourceUrl) {
    }

    private static class ChatbotContext {
        private final List<Publication> publications = new ArrayList<>();
        private final List<Project> projects = new ArrayList<>();
        private final List<Event> events = new ArrayList<>();
        private final List<Member> members = new ArrayList<>();
        private final List<String> personalLines = new ArrayList<>();
        private final List<PhDStudent> phdStudents = new ArrayList<>();

        int sourceCount() {
            return publications.size() + projects.size() + events.size() + members.size() + phdStudents.size() + personalLines.size();
        }

        String toPromptContext() {
            StringBuilder builder = new StringBuilder();

            if (!personalLines.isEmpty()) {
                builder.append("Personal context:\n");
                personalLines.forEach(line -> builder.append("- ").append(line).append("\n"));
            }

            if (!publications.isEmpty()) {
                builder.append("Publications:\n");
                publications.forEach(publication -> builder.append("- ID ").append(publication.getId())
                        .append(" | Title: ").append(publication.getTitle())
                        .append(" | Year: ").append(publication.getPublicationYear())
                        .append(" | Authors: ").append(publication.getAuthorsText())
                        .append(" | DOI: ").append(publication.getDoi())
                        .append(" | Source URL: ").append(publication.getExternalUrl())
                        .append("\n"));
            }

            if (!members.isEmpty()) {
                builder.append("Members:\n");
                members.forEach(member -> builder.append("- ID ").append(member.getId())
                        .append(" | Name: ").append((member.getFirstName() == null ? "" : member.getFirstName()) + " " + (member.getLastName() == null ? "" : member.getLastName()))
                        .append(" | Grade: ").append(member.getGrade())
                        .append(" | Bio: ").append(member.getBio())
                        .append(" | Research axes: ").append(String.join(", ", member.getResearchAxes()))
                        .append("\n"));
            }

            if (!projects.isEmpty()) {
                builder.append("Projects:\n");
                projects.forEach(project -> builder.append("- ID ").append(project.getId())
                        .append(" | Title: ").append(project.getTitle())
                        .append(" | Description: ").append(project.getDescription())
                        .append(" | Keywords: ").append(project.getKeywords())
                        .append(" | Duration: ").append(project.getDuration())
                        .append(" | Funding: ").append(project.getFunding())
                        .append("\n"));
            }

            if (!events.isEmpty()) {
                builder.append("Events:\n");
                events.forEach(event -> builder.append("- ID ").append(event.getId())
                        .append(" | Title: ").append(event.getTitle())
                        .append(" | Description: ").append(event.getDescription())
                        .append(" | Date: ").append(event.getEventDate())
                        .append(" | Location: ").append(event.getLocation())
                        .append(" | Responsible: ").append(event.getResponsiblePerson())
                        .append("\n"));
            }

            if (builder.isEmpty()) {
                builder.append("No matching MEDIATRON database context was found.\n");
            }

            return builder.toString();
        }
    }
}
