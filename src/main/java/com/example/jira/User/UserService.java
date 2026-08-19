package com.example.jira.User;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserbyusername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public void updateProfile(String username, String email) throws AccessDeniedException {
        User user = getCurrentUser();
        if (!user.getUsername().equals(username)) {throw new AccessDeniedException("Username cannot be changed");}
        user.setEmail(email);
        userRepository.save(user);
    }

    public void saveCanvasCredentials(String canvasToken, String canvasApiUrl) {
        User user = getCurrentUser();
        user.setCanvasToken(canvasToken);
        user.setCanvasApiUrl(canvasApiUrl);
        userRepository.save(user);
    }
}
