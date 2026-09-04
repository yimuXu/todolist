package com.example.jira.Todo;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.Date;
import com.example.jira.User.User;

@Entity
public class TodoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private  String itemName;
    private String itemDescription;
    private LocalDateTime dueDate;
    @CreationTimestamp
    private Date createdDate;
    private ItemStatus status = ItemStatus.TODO;
    /** Set only on items pulled from Canvas; the dedup key for re-syncs. */
    @Column(name = "canvas_assignment_id")
    private Long canvasAssignmentId;
    @ManyToOne
    @JoinColumn(name = "Todolist_id")
    @JsonIgnore
    private TodoList todoList;
    @ManyToOne
    @JoinColumn(name = "addedby_id")
    private User addedby;

    public TodoItem(String itemName, String itemDescription, LocalDateTime dueDate) {
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.dueDate = dueDate;
    }

    public TodoItem() {

    }
    public String getItemName() {
        return itemName;
    }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public ItemStatus getStatus() {
        return status;
    }
    public void setItemStatus(ItemStatus itemStatus) {
        this.status = itemStatus;
    }

    public void setTodoList(TodoList todoList) {
        this.todoList = todoList;
    }
    public TodoList getTodoList() {
        return this.todoList;
    }
    public int getId() {return this.id;}

    public Long getCanvasAssignmentId() {return canvasAssignmentId;}
    public void setCanvasAssignmentId(Long canvasAssignmentId) {this.canvasAssignmentId = canvasAssignmentId;}

    public String getAddedByUsername() {return addedby != null ? addedby.getEmail() : null;}
    public void setAddedBy(User addedby) {this.addedby = addedby;}
}
