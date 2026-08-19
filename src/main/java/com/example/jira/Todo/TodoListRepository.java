package com.example.jira.Todo;

import com.example.jira.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoListRepository extends JpaRepository<TodoList, Integer> {
    List<TodoList> findByUser(User user);
    Optional<TodoList> findByTodoListIdAndUser(int todoListId, User user);
}
