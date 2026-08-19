package com.example.jira.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The list name is the only thing the user ever sees of a synced course, and it has to survive
 * whatever Canvas hands over — courses with no name, offering suffixes on the code, titles that
 * already start with the unit code.
 */
class CanvasCourseTest {

    private static final CanvasTerm S2 = new CanvasTerm(1, "Semester 2 2026");

    @Test
    void putsTheUnitCodeInFrontOfTheTitle() {
        CanvasCourse course = new CanvasCourse(1, "Foundations of Data Science", "DATA1001_S2C_ND", false, S2);
        assertEquals("DATA1001 - Foundations of Data Science", course.listName());
    }

    /** No point printing "COMP3888 - COMP3888 Computer Science Project". */
    @Test
    void leavesATitleThatAlreadyStartsWithTheUnitCodeAlone() {
        CanvasCourse course = new CanvasCourse(1, "COMP3888 Computer Science Project", "COMP3888_S2C_ND", false, S2);
        assertEquals("COMP3888 Computer Science Project", course.listName());
    }

    @Test
    void fallsBackToTheCourseCodeWhenThereIsNoName() {
        CanvasCourse course = new CanvasCourse(1, null, "DATA1001_S2C_ND", false, S2);
        assertEquals("DATA1001_S2C_ND", course.listName());
    }

    @Test
    void fallsBackToTheCourseIdWhenThereIsNothingElse() {
        CanvasCourse course = new CanvasCourse(4218, null, null, false, S2);
        assertEquals("Canvas course 4218", course.listName());
    }

    @Test
    void trimsAVeryLongListNameToFitTheColumn() {
        String longTitle = "Foundations of ".repeat(20);
        CanvasCourse course = new CanvasCourse(1, longTitle, "DATA1001", false, S2);
        assertEquals(120, course.listName().length());
    }

    /** Canvas lists courses the student can no longer open, with the name stripped off. */
    @Test
    void skipsCoursesCanvasHasClosedOff() {
        assertFalse(new CanvasCourse(1, null, null, true, S2).isReadable());
        assertTrue(new CanvasCourse(1, "Foundations of Data Science", "DATA1001", false, S2).isReadable());
        assertFalse(new CanvasCourse(0, "Foundations of Data Science", "DATA1001", null, S2).isReadable());
    }
}
