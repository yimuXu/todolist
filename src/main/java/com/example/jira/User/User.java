package com.example.jira.User;

import com.example.jira.Group.Group;
import com.example.jira.Todo.TodoList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.*;
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    private String username;
    private String password;
    private String email;
    private UserRole role;
    @JsonIgnore
    @Column(name = "canvas_token", nullable = true, length = 512)
    private String canvasToken;
    @JsonIgnore
    @Column(name = "canvas_api_url", nullable = true, length = 512)
    private String canvasApiUrl;
    @OneToMany(mappedBy ="user")
    @JsonIgnore
    private List<TodoList> todoLists;
    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private List<Group> groups = new ArrayList<>();



    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        role = UserRole.General;
    }

    public User() {

    }
    // Everything below that Spring Security needs is @JsonIgnore'd: a User is reachable from
    // several serialised responses (group owner, invitee), and without this the bcrypt password
    // hash and the UserDetails flags are written straight into the JSON.
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    @Override
    public String getUsername() {
        return username;
    }

    public void setUserName(String userName) {
        this.username = userName;
    }
    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }
    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() { return true; }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    @JsonIgnore
    public boolean isEnabled() { return true;}


//    getter and setter
    public void setPassword(String password) {this.password = password;}
    public List<TodoList> getTodoLists() {return todoLists;}
    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}
    public String getRole() {return role == null ? null : role.toString();}
    public void setRole(UserRole role) {this.role = role;}
    public String getCanvasToken() {return canvasToken;}
    public void setCanvasToken(String canvasToken) {this.canvasToken = canvasToken;}
    public String getCanvasApiUrl() {return canvasApiUrl;}
    public void setCanvasApiUrl(String canvasApiUrl) {this.canvasApiUrl = canvasApiUrl;}
}



