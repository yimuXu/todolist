package com.example.jira.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** One course as returned by GET /api/v1/courses. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasCourse(
        long id,
        String name,
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("access_restricted_by_date") Boolean accessRestrictedByDate,
        CanvasTerm term) {

    /** e.g. "COMP3888_S2C_ND" -> "COMP3888". Canvas suffixes the unit code with the offering. */
    private static final Pattern UNIT_CODE = Pattern.compile("[A-Za-z]{2,6}\\d{4}");
    private static final int MAX_LIST_NAME = 120;

    /** Canvas hides courses outside their term but still lists them, with no name attached. */
    public boolean isReadable() {
        return id != 0 && !Boolean.TRUE.equals(accessRestrictedByDate);
    }

    /**
     * True when this course's term is the semester running now. Canvas prints the term under
     * every course card on the dashboard ("Semester 2 2026"), so that name is all the sync goes
     * on. An enrolment stays "active" long after a semester ends, which is why the term has to
     * be checked at all: without it every unit the student has ever taken comes back as a to-do
     * list on each sync.
     */
    public boolean isInCurrentTerm(AcademicTerm currentTerm) {
        return currentTerm.equals(AcademicTerm.parse(term == null ? null : term.name()));
    }

    /** How this course's term should be described in a "skipped" message. */
    public String termLabel() {
        return term == null ? "no term" : term.label();
    }

    /** "COMP3888 - Computer Science Project (2026 Semester 2)" for a skipped-course message. */
    public String describe() {
        return listName() + " (" + termLabel() + ")";
    }

    /** The to-do list name for this course: unit code + title, both taken from Canvas. */
    public String listName() {
        String title = name == null || name.isBlank() ? null : name.trim();
        String unit = unitCode();
        String listName;
        if (unit != null && title != null) {
            listName = title.toUpperCase().startsWith(unit) ? title : unit + " - " + title;
        } else if (title != null) {
            listName = title;
        } else if (courseCode != null && !courseCode.isBlank()) {
            listName = courseCode.trim();
        } else {
            listName = "Canvas course " + id;
        }
        return listName.length() > MAX_LIST_NAME ? listName.substring(0, MAX_LIST_NAME) : listName;
    }

    private String unitCode() {
        if (courseCode == null || courseCode.isBlank()) return null;
        Matcher matcher = UNIT_CODE.matcher(courseCode);
        return matcher.find() ? matcher.group().toUpperCase() : null;
    }
}
