package com.example.jira.Todo;

import com.example.jira.Group.Group;
import com.example.jira.User.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "todolists")
public class TodoList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int todoListId;
    private String todolistname;
    @CreationTimestamp
    private LocalDateTime date;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    // Cascade + orphanRemoval so deleting a list also clears its tasks. Without it the
    // todoitem rows kept pointing at the deleted list and the FK constraint failed, so a
    // list could only ever be deleted while it was empty.
    @OneToMany(mappedBy="todoList",fetch = FetchType.EAGER,cascade = CascadeType.ALL,orphanRemoval = true)
    private List<TodoItem> todoItem;

    @ManyToOne
    @JoinColumn(name = "group_id", nullable = true)
    @JsonIgnore
    private Group group;

    public TodoList(String todolistname) {
        this.todolistname = todolistname;
    }

    public TodoList() {}
    @JsonIgnore
    public User getUser() {return this.user;}
    public void setUser(User user) {this.user = user;}

    public String getTodoListname() {
        return this.todolistname;
    }
    public void setTodoListname(String todolistname) {
        this.todolistname = todolistname;
    }

    public int getTodoListId() {return this.todoListId;}
    public String getTodolistname() {return this.todolistname;}
    public List<TodoItem> getTodoItem() {return this.todoItem;}
    public LocalDateTime getDate() {return this.date;}
    public Group getGroup() {return this.group;}

    public void setGroup(Group group) {this.group = group;}
    public void setTodoItem(List<TodoItem> todoItem) {this.todoItem = todoItem;}

}
