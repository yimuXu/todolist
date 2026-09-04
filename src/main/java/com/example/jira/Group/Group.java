package com.example.jira.Group;

import jakarta.persistence.*;
import com.example.jira.Todo.TodoList;
import com.example.jira.User.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int groupId;
    private String groupName;
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "email"))
    private final List<User> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", fetch = FetchType.EAGER)
    private final List<TodoList> todoLists = new ArrayList<>();

    public Group() {}
    public Group(String groupName,User owner) {this.groupName = groupName;this.owner = owner;}
    public int getGroupId() {return groupId;}
    public void setGroupId(int groupId) {this.groupId = groupId;}
    public String getGroupName() {return groupName;}
    public void setGroupName(String groupName) {this.groupName = groupName;}
    public User getOwner() {return owner;}
    public void setOwner(User owner) {this.owner = owner;}
    public List<User> getMembers() {return members;}
    public List<TodoList> getTodoLists() {return todoLists;}
}
