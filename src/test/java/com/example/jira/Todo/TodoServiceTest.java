package com.example.jira.Todo;

import com.example.jira.TestEntities;
import com.example.jira.User.User;
import com.example.jira.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TodoServiceTest {

    private final UserService userService = mock(UserService.class);
    private final TodoItemRepository todoItemRepository = mock(TodoItemRepository.class);
    private final TodoListRepository todoListRepository = mock(TodoListRepository.class);
    private final TodoService todoService =
            new TodoService(userService, todoItemRepository, todoListRepository);

    private final User user = new User("dd@example.com", "hashed");
    private TodoList list;
    private TodoItem item;

    @BeforeEach
    void setUp() {
        when(userService.getCurrentUser()).thenReturn(user);

        list = TestEntities.withField(new TodoList("DATA1001"), "todoListId", 7);
        list.setUser(user);
        item = TestEntities.withField(new TodoItem("Quiz 1", "http://canvas/1", null), "id", 42);
        item.setTodoList(list);
        list.setTodoItem(new ArrayList<>(List.of(item)));

        when(todoListRepository.findByTodoListIdAndUser(7, user)).thenReturn(Optional.of(list));
        when(todoItemRepository.findByIdAndTodoList(42, list)).thenReturn(Optional.of(item));
    }

    /**
     * The regression this whole method exists for. TodoList maps its tasks EAGER with
     * CascadeType.ALL, so a task left behind in the parent's collection is re-persisted on flush
     * and comes straight back after a "successful" delete. Taking it out of the collection is
     * what makes the removal stick, so that is what gets asserted.
     */
    @Test
    void deleteTakesTheTaskOutOfTheListsOwnCollection() {
        todoService.deleteTodoItem(7, 42);

        assertTrue(list.getTodoItem().isEmpty(), "the deleted task must not be left in the parent list");
        verify(todoItemRepository).delete(item);
        verify(todoItemRepository).flush();
    }

    @Test
    void deleteRefusesATaskThatIsNotInThatList() {
        when(todoItemRepository.findByIdAndTodoList(99, list)).thenReturn(Optional.empty());

        ResponseStatusException thrown =
                assertThrows(ResponseStatusException.class, () -> todoService.deleteTodoItem(7, 99));
        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void deleteRefusesAListTheUserDoesNotOwn() {
        when(todoListRepository.findByTodoListIdAndUser(8, user)).thenReturn(Optional.empty());

        ResponseStatusException thrown =
                assertThrows(ResponseStatusException.class, () -> todoService.deleteTodoItem(8, 42));
        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    @Test
    void createListRefusesABlankName() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> todoService.createList(new TodoListRequest("   ")));
        assertEquals(HttpStatus.BAD_REQUEST, thrown.getStatusCode());
    }

    /** A null field in the request means "leave it alone", not "clear it". */
    @Test
    void updateOnlyTouchesTheFieldsTheRequestCarries() {
        TodoItemRequest request = new TodoItemRequest();
        request.setItemName("Quiz 1 (resit)");

        todoService.updateTodoItem(7, 42, request);

        assertEquals("Quiz 1 (resit)", item.getItemName());
        assertEquals("http://canvas/1", item.getItemDescription());
        verify(todoItemRepository).save(item);
    }

    @Test
    void updateSetsTheDueDateWhenOneIsGiven() {
        LocalDateTime due = LocalDateTime.of(2026, 9, 4, 23, 59);
        TodoItemRequest request = new TodoItemRequest("Quiz 1", null, due);

        todoService.updateTodoItem(7, 42, request);

        assertEquals(due, item.getDueDate());
    }

    @Test
    void markStatusMovesTheTaskBetweenColumns() {
        todoService.markStatus(7, 42, ItemStatus.DONE);

        assertEquals(ItemStatus.DONE, item.getStatus());
        verify(todoItemRepository).save(item);
    }
}
