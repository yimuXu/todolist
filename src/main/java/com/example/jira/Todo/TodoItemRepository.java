package com.example.jira.Todo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoItemRepository extends JpaRepository<TodoItem,Integer> {
    Optional<TodoItem> findByIdAndTodoList(int itemId, TodoList todoList);
    List<TodoItem> findByTodoListOrderByDueDateAsc(TodoList todoList);
    Optional<TodoItem> findByTodoListAndCanvasAssignmentId(TodoList todoList, Long canvasAssignmentId);
    List<TodoItem> findByTodoListAndCanvasAssignmentIdIsNull(TodoList todoList);
}
