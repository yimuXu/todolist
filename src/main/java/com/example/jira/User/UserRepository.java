package com.example.jira.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// User's @Id is the String email, so the id type has to be String — not Integer.
public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> findByEmail(String email);
    List<User> findByCanvasTokenIsNotNull();
}
