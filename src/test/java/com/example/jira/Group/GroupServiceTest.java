package com.example.jira.Group;

import com.example.jira.TestEntities;
import com.example.jira.Todo.ItemStatus;
import com.example.jira.Todo.TodoItem;
import com.example.jira.Todo.TodoItemRepository;
import com.example.jira.Todo.TodoItemRequest;
import com.example.jira.Todo.TodoList;
import com.example.jira.Todo.TodoListRepository;
import com.example.jira.Todo.TodoService;
import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import com.example.jira.User.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A shared list is created by one member but worked on by all of them, so every check here is
 * about a member who is *not* the creator being allowed to touch the board — and about the id
 * they pass still being confined to their own group.
 */
class GroupServiceTest {

    private final GroupRepository groupRepository = mock(GroupRepository.class);
    private final UserService userService = mock(UserService.class);
    private final GroupInviteRepository groupInviteRepository = mock(GroupInviteRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TodoListRepository todoListRepository = mock(TodoListRepository.class);
    private final TodoItemRepository todoItemRepository = mock(TodoItemRepository.class);

    // A real TodoService on mocked repositories, so the delete path under test is the production one.
    private final TodoService todoService =
            new TodoService(userService, todoItemRepository, todoListRepository);
    private final GroupService groupService = new GroupService(groupRepository, userService,
            groupInviteRepository, userRepository, todoListRepository, todoService, todoItemRepository);

    private final User alice = new User("alice@example.com", "hashed");
    private final User bob = new User("bob@example.com", "hashed");
    private final User mallory = new User("mallory@example.com", "hashed");

    private Group group;
    private TodoList sharedList;
    private TodoItem item;

    @BeforeEach
    void setUp() {
        group = TestEntities.withField(new Group("COMP3888 team", alice), "groupId", 3);
        group.getMembers().addAll(List.of(alice, bob));

        // Created by alice: going through list ownership would lock bob out of his own team board.
        sharedList = TestEntities.withField(new TodoList("Sprint 1"), "todoListId", 7);
        sharedList.setUser(alice);
        sharedList.setGroup(group);

        item = TestEntities.withField(new TodoItem("Write the report", "", null), "id", 42);
        item.setTodoList(sharedList);
        sharedList.setTodoItem(new ArrayList<>(List.of(item)));

        when(groupRepository.findById(3)).thenReturn(Optional.of(group));
        when(todoListRepository.findById(7)).thenReturn(Optional.of(sharedList));
        when(todoItemRepository.findByIdAndTodoList(42, sharedList)).thenReturn(Optional.of(item));
        when(userService.getCurrentUser()).thenReturn(bob);
    }

    @Test
    void aMemberWhoDidNotCreateTheListCanDeleteFromIt() {
        groupService.deleteGroupTodoItem(3, 7, 42);

        assertTrue(sharedList.getTodoItem().isEmpty());
        verify(todoItemRepository).delete(item);
    }

    @Test
    void aMemberWhoDidNotCreateTheListCanEditItsTasks() {
        TodoItemRequest request = new TodoItemRequest();
        request.setItemName("Write the report (draft 2)");

        groupService.updateGroupTodoItem(3, 7, 42, request);

        assertEquals("Write the report (draft 2)", item.getItemName());
    }

    @Test
    void aMemberWhoDidNotCreateTheListCanMoveItsCards() {
        groupService.markGroupTodoItemStatus(3, 7, 42, ItemStatus.ONPROGRESS);

        assertEquals(ItemStatus.ONPROGRESS, item.getStatus());
        verify(todoItemRepository).save(item);
    }

    @Test
    void aMemberWhoDidNotCreateTheListCanAddToIt() {
        groupService.createGroupTodoItem(3, 7, new TodoItemRequest("New task", "notes", null));

        verify(todoItemRepository).save(any(TodoItem.class));
    }

    @Test
    void somebodyOutsideTheGroupIsRefused() {
        when(userService.getCurrentUser()).thenReturn(mallory);

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroupTodoItem(3, 7, 42));
        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    /** Membership alone is not enough — the list id has to belong to the group being named. */
    @Test
    void aListBelongingToAnotherGroupIsRefused() {
        Group other = TestEntities.withField(new Group("Someone else's team", alice), "groupId", 9);
        TodoList otherList = TestEntities.withField(new TodoList("Their sprint"), "todoListId", 8);
        otherList.setGroup(other);
        when(todoListRepository.findById(8)).thenReturn(Optional.of(otherList));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroupTodoItem(3, 8, 42));
        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    /** A private list has no group at all, so passing its id must not reach it either. */
    @Test
    void aPrivateListIsRefused() {
        TodoList privateList = TestEntities.withField(new TodoList("Alice's own list"), "todoListId", 8);
        privateList.setUser(alice);
        when(todoListRepository.findById(8)).thenReturn(Optional.of(privateList));

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> groupService.deleteGroupTodoItem(3, 8, 42));
        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    @Test
    void onlyTheOwnerCanRenameTheGroup() {
        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> groupService.updateGroup(3, "bob's rename"));
        assertEquals(HttpStatus.FORBIDDEN, thrown.getStatusCode());
    }

    @Test
    void theOwnerCanRenameTheGroup() {
        when(userService.getCurrentUser()).thenReturn(alice);
        when(groupRepository.save(group)).thenReturn(group);

        assertEquals("Renamed", groupService.updateGroup(3, "Renamed").getGroupName());
    }

    @Test
    void invitingSomebodyTwiceDoesNotCreateASecondInvite() {
        when(userService.getCurrentUser()).thenReturn(alice);
        when(userRepository.findByEmail("mallory@example.com")).thenReturn(Optional.of(mallory));
        when(groupInviteRepository.existsByGroupAndInviteeAndStatus(group, mallory, InviteStatus.PENDING))
                .thenReturn(true);

        assertEquals("Please do not invite repeatedly.", groupService.inviteUser(3, "mallory@example.com"));
    }
}
