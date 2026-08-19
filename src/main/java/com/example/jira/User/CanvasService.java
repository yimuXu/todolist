package com.example.jira.User;

import com.example.jira.Todo.TodoItem;
import com.example.jira.Todo.TodoItemRepository;
import com.example.jira.Todo.TodoList;
import com.example.jira.Todo.TodoListRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a user's Canvas access token into to-do lists: one list per enrolled course,
 * one item per assignment. Course codes and names come from Canvas itself.
 */
@Service
public class CanvasService {
    static final String DEFAULT_CANVAS_API = "https://canvas.sydney.edu.au/api/v1";
    /** include[]=term is required for the current-semester check in CanvasCourse. */
    static final String COURSES_PATH = "/courses?enrollment_state=active&include[]=term&per_page=100";

    private static final ParameterizedTypeReference<List<CanvasCourse>> COURSE_PAGE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CanvasAssignment>> ASSIGNMENT_PAGE = new ParameterizedTypeReference<>() {};
    /** Canvas paginates with RFC 5988 Link headers; per_page alone silently truncates. */
    private static final Pattern NEXT_PAGE = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"next\"");
    private static final int MAX_PAGES = 25;
    /** How many out-of-term course names the sync response names before it stops listing them. */
    private static final int MAX_REPORTED_SKIPS = 20;

    private final UserService userService;
    private final TodoListRepository todoListRepository;
    private final TodoItemRepository todoItemRepository;
    private final RestClient restClient;

    public CanvasService(UserService userService, TodoListRepository todoListRepository,
                         TodoItemRepository todoItemRepository, RestClient.Builder restClientBuilder) {
        this.userService = userService;
        this.todoListRepository = todoListRepository;
        this.todoItemRepository = todoItemRepository;
        this.restClient = restClientBuilder.build();
    }

    // ------------------------------ settings ------------------------------

    public CanvasSettingsResponse getSettings() {
        User user = userService.getCurrentUser();
        return new CanvasSettingsResponse(hasToken(user), apiUrlOf(user), null);
    }

    /**
     * Saves a pasted token after checking it against Canvas, so a bad paste is reported
     * immediately instead of at the next sync. A blank token keeps the stored one.
     */
    public CanvasSettingsResponse saveSettings(CanvasSettingsRequest request) {
        User user = userService.getCurrentUser();
        String apiUrl = normaliseApiUrl(request.getCanvasApiUrl(), apiUrlOf(user));
        String pasted = request.getCanvasToken() == null ? null : request.getCanvasToken().trim();
        String token = pasted == null || pasted.isEmpty() ? requireToken(user.getCanvasToken()) : pasted;

        CanvasProfile profile = call(() -> restClient.get()
                .uri(URI.create(apiUrl + "/users/self"))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(CanvasProfile.class));

        userService.saveCanvasCredentials(token, apiUrl);
        return new CanvasSettingsResponse(true, apiUrl, profile == null ? null : profile.name());
    }

    // ------------------------------ sync ------------------------------

    @Transactional
    public CanvasSyncResponse syncAssignments() {
        User user = userService.getCurrentUser();
        String token = requireToken(user.getCanvasToken());
        String apiUrl = apiUrlOf(user);

        // include[]=term is what makes Canvas attach the semester each course belongs to; without
        // it every course comes back termless and finished units cannot be told from current ones.
        List<CanvasCourse> courses = fetchAll(apiUrl, COURSES_PATH, token, COURSE_PAGE);
        AcademicTerm currentTerm = AcademicTerm.current(LocalDate.now());
        int syncedCourses = 0;
        int skippedCourses = 0;
        int added = 0;
        int updated = 0;
        // Named rather than only counted: when a sync brings back nothing the user needs to see
        // which courses were left out and what term Canvas put them in, or an empty result just
        // looks broken. Capped so a long enrolment history cannot bloat the response.
        List<String> skippedNames = new ArrayList<>();

        for (CanvasCourse course : courses) {
            if (course == null || !course.isReadable()) continue;
            if (!course.isInCurrentTerm(currentTerm)) {
                skippedCourses++;
                if (skippedNames.size() < MAX_REPORTED_SKIPS) skippedNames.add(course.describe());
                continue;
            }
            syncedCourses++;
            TodoList list = getOrCreateList(user, course.listName());
            backfillLegacyCanvasIds(list);

            for (CanvasAssignment assignment : fetchAll(apiUrl,
                    "/courses/" + course.id() + "/assignments?per_page=100", token, ASSIGNMENT_PAGE)) {
                if (assignment == null || assignment.id() == 0) continue;
                Optional<TodoItem> existing =
                        todoItemRepository.findByTodoListAndCanvasAssignmentId(list, assignment.id());
                TodoItem item = existing.orElseGet(TodoItem::new);
                item.setItemName(assignment.title());
                item.setDueDate(parseDueDate(assignment.dueAt()));
                if (existing.isPresent()) {
                    updated++;
                } else {
                    // Only set on create: a description the user has since typed must survive a re-sync.
                    item.setItemDescription(assignment.htmlUrl());
                    item.setCanvasAssignmentId(assignment.id());
                    item.setTodoList(list);
                    item.setAddedBy(user);
                    added++;
                }
                todoItemRepository.save(item);
            }
        }
        return new CanvasSyncResponse(syncedCourses, added, updated, skippedCourses,
                currentTerm.label(), skippedNames);
    }

    private TodoList getOrCreateList(User user, String courseName) {
        return todoListRepository.findByUser(user).stream()
                .filter(list -> list.getGroup() == null && courseName.equals(list.getTodolistname()))
                .findFirst()
                .orElseGet(() -> {
                    TodoList list = new TodoList(courseName);
                    list.setUser(user);
                    return todoListRepository.save(list);
                });
    }

    /**
     * One-off migration for items written by the previous sync, which identified an assignment by
     * stuffing a "[Canvas assignment 42]" marker into the description. Without this they would be
     * re-added as duplicates on the first sync after the upgrade.
     */
    private static final Pattern LEGACY_MARKER = Pattern.compile("\\[Canvas assignment (\\d+)]");

    private void backfillLegacyCanvasIds(TodoList list) {
        for (TodoItem item : todoItemRepository.findByTodoListAndCanvasAssignmentIdIsNull(list)) {
            Matcher matcher = LEGACY_MARKER.matcher(item.getItemDescription() == null ? "" : item.getItemDescription());
            if (matcher.find()) {
                item.setCanvasAssignmentId(Long.parseLong(matcher.group(1)));
                todoItemRepository.save(item);
            }
        }
    }

    /**
     * Canvas sends UTC ("2025-08-19T13:59:00Z"); keeping the raw wall time would show a 23:59
     * Sydney deadline as 13:59, so shift the instant into the server's zone first.
     */
    private LocalDateTime parseDueDate(String dueAt) {
        if (dueAt == null || dueAt.isBlank()) return null;
        try {
            return OffsetDateTime.parse(dueAt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    // ------------------------------ HTTP ------------------------------

    private <T> List<T> fetchAll(String apiUrl, String path, String token,
                                 ParameterizedTypeReference<List<T>> pageType) {
        List<T> all = new ArrayList<>();
        String url = apiUrl + path;
        for (int page = 0; page < MAX_PAGES && url != null; page++) {
            // URI.create, not the String overload: Canvas' next-page links are already encoded
            // and RestClient would treat them as URI templates and double-encode them.
            URI pageUrl = URI.create(url);
            ResponseEntity<List<T>> response = call(() -> restClient.get()
                    .uri(pageUrl)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .toEntity(pageType));
            if (response.getBody() != null) all.addAll(response.getBody());
            url = nextPage(response.getHeaders().getFirst("Link"), apiUrl);
        }
        return all;
    }

    /** Only follow a next-page link that stays on the configured Canvas host. */
    private String nextPage(String linkHeader, String apiUrl) {
        if (linkHeader == null) return null;
        Matcher matcher = NEXT_PAGE.matcher(linkHeader);
        if (!matcher.find()) return null;
        String next = matcher.group(1);
        return next.startsWith(apiUrl) ? next : null;
    }

    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            int code = exception.getStatusCode().value();
            if (code == HttpStatus.UNAUTHORIZED.value() || code == HttpStatus.FORBIDDEN.value()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Canvas rejected this token. Create a new Canvas access token and save it again.", exception);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Canvas returned HTTP " + code, exception);
        } catch (ResourceAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The server could not connect to Canvas", exception);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not read the response from Canvas", exception);
        }
    }

    // ------------------------------ helpers ------------------------------

    private boolean hasToken(User user) {
        return user.getCanvasToken() != null && !user.getCanvasToken().isBlank();
    }

    private String apiUrlOf(User user) {
        return user.getCanvasApiUrl() == null || user.getCanvasApiUrl().isBlank()
                ? DEFAULT_CANVAS_API : user.getCanvasApiUrl();
    }

    private String requireToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Paste your Canvas access token first (Canvas > Account > Settings > New Access Token)");
        }
        return token;
    }

    /** Accepts a bare Canvas host and appends the API path, which is the usual paste mistake. */
    private String normaliseApiUrl(String url, String fallback) {
        if (url == null || url.isBlank()) return fallback;
        String cleaned = url.trim().replaceAll("/+$", "");
        if (!cleaned.startsWith("https://")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canvas API URL must use HTTPS");
        }
        if (!cleaned.endsWith("/api/v1")) cleaned = cleaned + "/api/v1";
        return cleaned;
    }
}
