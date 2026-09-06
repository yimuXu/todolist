package com.example.jira.User;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * The enrolment term a course belongs to, from the "term" object Canvas attaches to
 * GET /api/v1/courses when include[]=term is requested. Canvas gives every term real start/end
 * dates here, which is what the sync actually filters on — every institution sets these
 * correctly regardless of hemisphere or what it calls the term ("Semester 2", "Term 1",
 * "Autumn"), unlike the display name, which has no fixed vocabulary to parse.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CanvasTerm(long id, String name, @JsonProperty("end_at") String endAt) {

    public String label() {
        return name == null || name.isBlank() ? "term " + id : name.trim();
    }

    /**
     * True once this term's end date has passed, or when it has no end date at all. A missing
     * end_at almost always means Canvas's catch-all "Default Term" — the term a course sits in
     * when nobody assigned it a real semester — which is indefinite and would otherwise never
     * count as "ended," syncing that course's to-do list permanently. Treating no end date as
     * already ended means only a term with a real, dated close is kept.
     */
    public boolean hasEnded() {
        if (endAt == null || endAt.isBlank()) return true;
        try {
            return OffsetDateTime.parse(endAt).toInstant().isBefore(Instant.now());
        } catch (DateTimeParseException exception) {
            return true;
        }
    }
}
