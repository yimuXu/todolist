package com.example.jira.Auth;

import com.example.jira.User.User;
import com.example.jira.User.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AuthService authService = new AuthService(jwtUtil, userRepository, passwordEncoder);

    /** LoginRequest is a form-binding bean, so it only has setters. */
    private static LoginRequest login(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    @Test
    void loginHandsBackATokenWhenThePasswordMatches() {
        User stored = new User("dd@example.com", "$2a$10$hashed");
        when(userRepository.findByEmail("dd@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("hunter2", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("dd@example.com")).thenReturn("a.jwt.token");

        assertEquals("a.jwt.token", authService.login(login("dd@example.com", "hunter2")));
    }

    @Test
    void loginRefusesAWrongPasswordWithoutIssuingAToken() {
        User stored = new User("dd@example.com", "$2a$10$hashed");
        when(userRepository.findByEmail("dd@example.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertTrue(authService.login(login("dd@example.com", "wrong")).startsWith("ERROR"));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void loginRefusesAnUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertTrue(authService.login(login("nobody@example.com", "hunter2")).startsWith("ERROR"));
    }
}
