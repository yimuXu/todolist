package com.example.jira.User;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final CanvasService canvasService;

    public UserController(UserService userService, CanvasService canvasService) {
        this.userService = userService;
        this.canvasService = canvasService;
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
