package com.example.jira.User;

/** canvasAccountName is only known right after a token is checked, so it may be null. */
public record CanvasSettingsResponse(boolean canvasTokenSaved, String canvasApiUrl,
                                     String canvasAccountName) {}
