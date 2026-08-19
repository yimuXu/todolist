package com.example.jira.User;

import com.example.jira.Todo.TodoItem;
import com.example.jira.Todo.TodoItemRepository;
import com.example.jira.Todo.TodoList;
import com.example.jira.Todo.TodoListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CanvasServiceTest {

    private static final String API = "https://canvas.test/api/v1";
    /** include[]=term is what makes Canvas attach the semester the sync filters on. */
    private static final String COURSES_URL =
            API + "/courses?enrollment_state=active&include[]=term&per_page=100";
    /**
     * Worked out at run time rather than written down, so these tests do not start failing the
     * moment the semester rolls over.
     */
    private static final String CURRENT_TERM = AcademicTerm.current(LocalDate.now()).label();

    private final UserService userService = mock(UserService.class);
    private final TodoListRepository todoListRepository = mock(TodoListRepository.class);
    private final TodoItemRepository todoItemRepository = mock(TodoItemRepository.class);

    private MockRestServiceServer canvas;
    private CanvasService canvasService;
    private User user;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        canvas = MockRestServiceServer.bindTo(builder).build();
        canvasService = new CanvasService(userService, todoListRepository, todoItemRepository, builder);

        user = new User("student", "pw", "student@example.com");
        user.setCanvasToken("canvas-token");
        user.setCanvasApiUrl(API);
        when(userService.getCurrentUser()).thenReturn(user);
        when(todoListRepository.findByUser(user)).thenReturn(List.of());
        when(todoListRepository.save(any(TodoList.class))).thenAnswer(call -> call.getArgument(0));
        when(todoItemRepository.save(any(TodoItem.class))).thenAnswer(call -> call.getArgument(0));
        when(todoItemRepository.findByTodoListAndCanvasAssignmentIdIsNull(any())).thenReturn(List.of());
        when(todoItemRepository.findByTodoListAndCanvasAssignmentId(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void namesListsFromTheCourseCodeAndTitleCanvasReports() {
        expectCourses();
        expectAssignments(1, "[]");
        expectAssignments(3, "[]");

        canvasService.syncAssignments();
        canvas.verify();

        ArgumentCaptor<TodoList> lists = ArgumentCaptor.forClass(TodoList.class);
        org.mockito.Mockito.verify(todoListRepository, org.mockito.Mockito.times(2)).save(lists.capture());
        assertThat(lists.getAllValues()).extracting(TodoList::getTodolistname)
                .containsExactly("COMP3888 - Computer Science Project", "DATA1001 - Data Science");
    }

    @Test
    void followsLinkPaginationAndSkipsRestrictedCourses() {
        expectCourses();
        expectAssignments(1, "[]");
        expectAssignments(3, "[]");

        CanvasSyncResponse response = canvasService.syncAssignments();

        // Course 2 is access-restricted (no name at all) and must not become a list.
        assertThat(response.courses()).isEqualTo(2);
    }

    /**
     * The reason the term filter exists: an enrolment stays "active" for years, so without it
     * every unit ever taken comes back as a to-do list. The old course must be left out, and
     * named in the response so an empty sync can explain itself.
     */
    @Test
    void leavesCoursesFromAnEarlierSemesterOut() {
        expectCourses();
        expectAssignments(1, "[]");
        expectAssignments(3, "[]");

        CanvasSyncResponse response = canvasService.syncAssignments();
        canvas.verify();

        assertThat(response.term()).isEqualTo(CURRENT_TERM);
        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.skippedCourses())
                .containsExactly("INFO1110 - Introduction to Programming (Semester 1 2019)");
        // No assignment request was made for course 4 — canvas.verify() above would have failed.
    }

    @Test
    void importsAssignmentsAndConvertsDueDatesToLocalTime() {
        expectCourses();
        expectAssignments(1, """
                [{"id":10,"name":"Report","due_at":"2025-08-19T13:59:00Z","html_url":"https://canvas.test/a/10"},
                 {"id":11,"name":"Quiz","due_at":null}]""");
        expectAssignments(3, "[]");

        CanvasSyncResponse response = canvasService.syncAssignments();

        assertThat(response.added()).isEqualTo(2);
        assertThat(response.updated()).isZero();

        ArgumentCaptor<TodoItem> items = ArgumentCaptor.forClass(TodoItem.class);
        org.mockito.Mockito.verify(todoItemRepository, org.mockito.Mockito.times(2)).save(items.capture());
        TodoItem report = items.getAllValues().get(0);
        assertThat(report.getItemName()).isEqualTo("Report");
        assertThat(report.getCanvasAssignmentId()).isEqualTo(10L);
        assertThat(report.getItemDescription()).isEqualTo("https://canvas.test/a/10");
        assertThat(report.getDueDate()).isEqualTo(
                LocalDateTime.ofInstant(Instant.parse("2025-08-19T13:59:00Z"), ZoneId.systemDefault()));
        // A null due_at used to be read as the literal string "null" and blew up the whole sync.
        assertThat(items.getAllValues().get(1).getDueDate()).isNull();
    }

    @Test
    void reSyncUpdatesTheExistingItemInsteadOfAddingADuplicate() {
        TodoItem existing = new TodoItem();
        existing.setItemDescription("my own notes");
        when(todoItemRepository.findByTodoListAndCanvasAssignmentId(any(), any()))
                .thenReturn(Optional.of(existing));

        expectCourses();
        expectAssignments(1, """
                [{"id":10,"name":"Report v2","due_at":null,"html_url":"https://canvas.test/a/10"}]""");
        expectAssignments(3, "[]");

        CanvasSyncResponse response = canvasService.syncAssignments();

        assertThat(response.added()).isZero();
        assertThat(response.updated()).isEqualTo(1);
        assertThat(existing.getItemName()).isEqualTo("Report v2");
        assertThat(existing.getItemDescription()).isEqualTo("my own notes");
    }

    @Test
    void reportsARejectedTokenAsUnauthorized() {
        canvas.expect(requestTo(COURSES_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{}").contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> canvasService.syncAssignments())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Canvas rejected this token");
    }

    @Test
    void refusesToSyncWithoutAToken() {
        user.setCanvasToken(" ");

        assertThatThrownBy(() -> canvasService.syncAssignments())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Paste your Canvas access token");
    }

    @Test
    void checksAPastedTokenBeforeStoringIt() {
        canvas.expect(requestTo(API + "/users/self"))
                .andExpect(header("Authorization", "Bearer fresh-token"))
                .andRespond(withSuccess("{\"id\":7,\"name\":\"Sam Student\"}", MediaType.APPLICATION_JSON));

        CanvasSettingsRequest request = new CanvasSettingsRequest();
        request.setCanvasToken("  fresh-token  ");
        request.setCanvasApiUrl("https://canvas.test/");   // no /api/v1: the usual paste mistake

        CanvasSettingsResponse response = canvasService.saveSettings(request);

        assertThat(response.canvasAccountName()).isEqualTo("Sam Student");
        assertThat(response.canvasApiUrl()).isEqualTo(API);
        org.mockito.Mockito.verify(userService).saveCanvasCredentials("fresh-token", API);
    }

    /**
     * Two pages of courses, so the Link-header pagination is exercised on every sync test. The
     * page also carries the three cases the sync has to tell apart: a course in the current
     * semester, one Canvas has closed off, and one from a semester that is long over.
     */
    private void expectCourses() {
        canvas.expect(requestTo(COURSES_URL))
                .andExpect(header("Authorization", "Bearer canvas-token"))
                .andRespond(withSuccess("""
                        [{"id":1,"name":"Computer Science Project","course_code":"COMP3888_S2C_ND",
                          "term":{"id":1,"name":"%s"}},
                         {"id":2,"access_restricted_by_date":true},
                         {"id":4,"name":"Introduction to Programming","course_code":"INFO1110_S1C_ND",
                          "term":{"id":2,"name":"Semester 1 2019"}}]""".formatted(CURRENT_TERM),
                        MediaType.APPLICATION_JSON)
                        .header("Link", "<" + API + "/courses?page=2&per_page=100>; rel=\"next\""));
        canvas.expect(requestTo(API + "/courses?page=2&per_page=100"))
                .andRespond(withSuccess("""
                        [{"id":3,"name":"Data Science","course_code":"DATA1001_S2C",
                          "term":{"id":1,"name":"%s"}}]""".formatted(CURRENT_TERM),
                        MediaType.APPLICATION_JSON));
    }

    private void expectAssignments(int courseId, String body) {
        canvas.expect(requestTo(API + "/courses/" + courseId + "/assignments?per_page=100"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
