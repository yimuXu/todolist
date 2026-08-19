package com.example.jira.Todo;
import com.fasterxml.jackson.annotation.JsonProperty;
public class TodoListRequest {
    @JsonProperty("todolistname")
    private String todolistname;

    public TodoListRequest(String name) {
        this.todolistname = name;
    }
    public TodoListRequest() {}
    public void setTodoListname(String todolistname) {
        this.todolistname = todolistname;
    }
    public String getTodoListname() {
        return this.todolistname;
    }
}
