package com.example.jira.User;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sync keeps a course when its Canvas term is the semester running now, and nothing else.
 * "Now" here is August 2026, i.e. Semester 2 2026.
 */
class CanvasCourseTermTest {

    private static final AcademicTerm NOW = AcademicTerm.current(LocalDate.of(2026, 8, 19));

    private static CanvasCourse course(String termName) {
        return new CanvasCourse(1, "Foundations of Data Science", "DATA1001", false,
                termName == null ? null : new CanvasTerm(1, termName));
    }

    @Test
    void currentSemesterSplitsTheYearAtJuly() {
        assertEquals("Semester 1 2026", AcademicTerm.current(LocalDate.of(2026, 6, 30)).label());
        assertEquals("Semester 2 2026", AcademicTerm.current(LocalDate.of(2026, 7, 1)).label());
        assertEquals("Semester 2 2026", AcademicTerm.current(LocalDate.of(2026, 8, 19)).label());
        assertEquals("Semester 1 2027", AcademicTerm.current(LocalDate.of(2027, 3, 3)).label());
    }

    @Test
    void readsTheTermNameEitherWayRound() {
        assertEquals(new AcademicTerm(2026, 2), AcademicTerm.parse("Semester 2 2026"));
        assertEquals(new AcademicTerm(2026, 2), AcademicTerm.parse("2026 Semester 2"));
        assertEquals(new AcademicTerm(2026, 2), AcademicTerm.parse("Semester 2, 2026"));
        assertEquals(new AcademicTerm(2026, 1), AcademicTerm.parse("Semester 1 2026"));
        assertNull(AcademicTerm.parse("Default Term"));
        assertNull(AcademicTerm.parse(null));
    }

    @Test
    void keepsThisSemestersCourses() {
        assertTrue(course("Semester 2 2026").isInCurrentTerm(NOW));
    }

    @Test
    void dropsEveryOtherSemester() {
        assertFalse(course("Semester 1 2026").isInCurrentTerm(NOW));   // finished in June
        assertFalse(course("Semester 2 2025").isInCurrentTerm(NOW));   // last year
        assertFalse(course("Semester 1 2027").isInCurrentTerm(NOW));   // not started yet
    }

    @Test
    void dropsCoursesWhoseTermSaysNothingUseful() {
        assertFalse(course("Default Term").isInCurrentTerm(NOW));
        assertFalse(course(null).isInCurrentTerm(NOW));
    }

    @Test
    void describesASkippedCourseWithItsTerm() {
        assertEquals("DATA1001 - Foundations of Data Science (Semester 1 2024)",
                course("Semester 1 2024").describe());
    }
}
