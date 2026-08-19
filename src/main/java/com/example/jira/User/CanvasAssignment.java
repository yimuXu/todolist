package com.example.jira.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One assignment as returned by GET /api/v1/courses/{id}/assignments. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasAssignment(
        long id,
        String name,
        @JsonProperty("due_at") String dueAt,
        @JsonProperty("html_url") String htmlUrl) {

    public String title() {
        return name == null || name.isBlank() ? "Untitled Canvas assignment" : name.trim();
    }
}
