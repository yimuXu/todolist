package com.example.jira.Todo;

import java.time.LocalDateTime;

public class TodoItemRequest {
    private String itemName;
    private String itemDescription;
    private LocalDateTime itemDate;

    public TodoItemRequest(String itemName, String itemDescription, LocalDateTime itemDate) {
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemDate = itemDate;
    }

    public TodoItemRequest() {}

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;

    }
    public void setItemDate(LocalDateTime itemDate) {
        this.itemDate = itemDate;
    }

    public String getname() {
        return this.itemName;
    }
    public String getdescription() {
        return this.itemDescription;
    }
    public LocalDateTime getdate() {
        return this.itemDate;
    }
}
