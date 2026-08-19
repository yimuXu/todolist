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
    private static LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void signupStoresTheHashRatherThanThePassword() {
        when(userRepository.findByUsername("dd")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("hunter2")).thenReturn("$2a$10$hashed");

        assertEquals("sign up successful!",
                authService.signup(new SignupRequest("dd", "hunter2", "dd@example.com")));

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("hunter2");
    }

    @Test
    void signupRefusesAUsernameThatIsTaken() {
        when(userRepository.findByUsername("dd"))
                .thenReturn(Optional.of(new User("dd", "hash", "dd@example.com")));

        assertTrue(authService.signup(new SignupRequest("dd", "hunter2", "dd@example.com"))
                .startsWith("ERROR"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void signupRefusesAMissingField() {
        assertTrue(authService.signup(new SignupRequest("dd", null, "dd@example.com")).startsWith("ERROR"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginHandsBackATokenWhenThePasswordMatches() {
        User stored = new User("dd", "$2a$10$hashed", "dd@example.com");
        when(userRepository.findByUsername("dd")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("hunter2", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtil.generateToken("dd")).thenReturn("a.jwt.token");

        assertEquals("a.jwt.token", authService.login(login("dd", "hunter2")));
    }

    @Test
    void loginRefusesAWrongPasswordWithoutIssuingAToken() {
        User stored = new User("dd", "$2a$10$hashed", "dd@example.com");
        when(userRepository.findByUsername("dd")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertTrue(authService.login(login("dd", "wrong")).startsWith("ERROR"));
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void loginRefusesAnUnknownUsername() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertTrue(authService.login(login("nobody", "hunter2")).startsWith("ERROR"));
    }
}
