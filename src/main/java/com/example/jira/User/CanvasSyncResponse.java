package com.example.jira.User;

import java.util.List;

/**
 * The outcome of one Canvas sync.
 *
 * {@code terms} and {@code skippedCourses} exist so that a sync which pulls in nothing is
 * explainable from the UI: the user can see which term(s) were matched and which courses were
 * left out because their term had already ended. A list rather than a single term because a
 * student can be enrolled in more than one not-yet-ended term at once — typically around an
 * enrolment changeover, where next semester's units already show up alongside this semester's.
 */
public record CanvasSyncResponse(int courses, int added, int updated, int skipped,
                                 List<String> terms, List<String> skippedCourses) {}
