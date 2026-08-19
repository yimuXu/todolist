package com.example.jira.User;

import java.util.List;

/**
 * The outcome of one Canvas sync.
 *
 * {@code term} and {@code skippedCourses} exist so that a sync which pulls in nothing is
 * explainable from the UI: the user can see which semester was matched and which courses were
 * left out because they belong to another one.
 */
public record CanvasSyncResponse(int courses, int added, int updated, int skipped,
                                 String term, List<String> skippedCourses) {}
