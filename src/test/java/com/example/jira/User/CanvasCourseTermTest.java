package com.example.jira.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sync keeps a course whenever its Canvas term has not ended yet — the term running now,
 * plus any future term the student is already enrolled in — and drops it once the term's own
 * end_at date has passed. Dates rather than a parsed term name: Canvas sets end_at correctly
 * for every institution regardless of hemisphere or what it calls the term.
 */
class CanvasCourseTermTest {

    private static CanvasCourse course(CanvasTerm term) {
        return new CanvasCourse(1, "Foundations of Data Science", "DATA1001", false, term);
    }

    private static CanvasTerm term(String endAt) {
        return new CanvasTerm(1, "Semester 2 2026", endAt);
    }

    @Test
    void keepsATermThatHasNotEndedYet() {
        assertFalse(term("2099-01-01T00:00:00Z").hasEnded());
        assertTrue(course(term("2099-01-01T00:00:00Z")).isInCurrentTerm());
    }

    @Test
    void dropsATermThatHasAlreadyEnded() {
        assertTrue(term("2020-01-01T00:00:00Z").hasEnded());
        assertFalse(course(term("2020-01-01T00:00:00Z")).isInCurrentTerm());
    }

    @Test
    void dropsATermWithNoEndDateAtAll() {
        // A missing end_at is almost always Canvas's catch-all "Default Term" — the term a
        // course sits in when nobody assigned it a real semester — which is indefinite and
        // would otherwise never count as "ended," syncing that course's list forever.
        assertTrue(term(null).hasEnded());
        assertFalse(course(term(null)).isInCurrentTerm());
    }

    @Test
    void keepsAFutureTermTheStudentIsAlreadyEnrolledIn() {
        // Enrolment for next semester opens before this semester ends: both should sync so
        // upcoming assignments are visible ahead of the term actually starting.
        assertTrue(course(term("2099-06-30T00:00:00Z")).isInCurrentTerm());
    }

    @Test
    void treatsAnUnparseableEndDateAsEnded() {
        assertTrue(term("not-a-date").hasEnded());
    }

    @Test
    void dropsACourseWithNoTermAtAll() {
        assertFalse(course(null).isInCurrentTerm());
    }

    @Test
    void describesASkippedCourseWithItsTermLabel() {
        assertEquals("DATA1001 - Foundations of Data Science (Semester 2 2026)",
                course(term("2020-01-01T00:00:00Z")).describe());
    }
}
