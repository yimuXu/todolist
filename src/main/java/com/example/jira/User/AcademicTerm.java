package com.example.jira.User;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A semester the way Canvas writes it on a course card: "Semester 2 2026".
 */
public record AcademicTerm(int year, int semester) {

    /** "Semester 2 2026", "2026 Semester 2", "Semester 2, 2026" — order does not matter. */
    private static final Pattern YEAR = Pattern.compile("(?<![0-9])(20\\d{2})(?![0-9])");
    private static final Pattern SEMESTER = Pattern.compile("(?i)semester\\s*([12])(?![0-9])");

    /**
     * The semester in progress on the given day. Sydney starts Semester 1 in February and
     * Semester 2 in July, so the year splits at 1 July.
     */
    public static AcademicTerm current(LocalDate today) {
        return new AcademicTerm(today.getYear(), today.getMonthValue() >= 7 ? 2 : 1);
    }

    /** The term named in the text, or null when it does not name both a year and a semester. */
    public static AcademicTerm parse(String text) {
        Integer year = firstGroup(YEAR, text);
        Integer semester = firstGroup(SEMESTER, text);
        return year == null || semester == null ? null : new AcademicTerm(year, semester);
    }

    private static Integer firstGroup(Pattern pattern, String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    public String label() {
        return "Semester " + semester + " " + year;
    }
}
