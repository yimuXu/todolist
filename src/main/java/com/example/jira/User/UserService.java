package com.example.jira.User;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public void saveCanvasCredentials(String canvasToken, String canvasApiUrl) {
        User user = getCurrentUser();
        user.setCanvasToken(canvasToken);
        user.setCanvasApiUrl(canvasApiUrl);
        userRepository.save(user);
    }
}
