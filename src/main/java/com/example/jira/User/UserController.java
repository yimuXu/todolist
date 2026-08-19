package com.example.jira.User;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final CanvasService canvasService;

    public UserController(UserService userService, CanvasService canvasService) {
        this.userService = userService;
        this.canvasService = canvasService;
    }

    @PutMapping("/user/{username}")
    public ResponseEntity<String> updateUser(@PathVariable String username, @RequestParam String email) {
        try {
            userService.updateProfile(username, email);
            return ResponseEntity.ok("Profile updated");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    @GetMapping("/user/canvas-settings")
    public CanvasSettingsResponse getCanvasSettings() {
        return canvasService.getSettings();
    }

    @PutMapping("/user/canvas-settings")
    public CanvasSettingsResponse updateCanvasSettings(@RequestBody CanvasSettingsRequest request) {
        return canvasService.saveSettings(request);
    }

    @PostMapping("/canvas/sync")
    public CanvasSyncResponse syncCanvasAssignments() {
        return canvasService.syncAssignments();
    }
}
