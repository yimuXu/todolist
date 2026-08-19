package com.example.jira.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The enrolment term a course belongs to, from the "term" object Canvas attaches to
 * GET /api/v1/courses when include[]=term is requested. Its name is what the dashboard prints
 * under each course card ("Semester 2 2026"), and that name is what the sync matches on.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasTerm(long id, String name) {

    public String label() {
        return name == null || name.isBlank() ? "term " + id : name.trim();
    }
}
