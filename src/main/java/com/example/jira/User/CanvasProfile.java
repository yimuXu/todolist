package com.example.jira.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** GET /api/v1/users/self — used to check a pasted token before it is stored. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasProfile(long id, String name) {}
