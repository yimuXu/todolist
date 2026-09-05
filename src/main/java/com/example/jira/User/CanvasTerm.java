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
     * True once this term's end date has passed. A null end date (an ongoing or not-yet-dated
     * term) is treated as not yet ended, since Canvas leaves it null for terms with no fixed
     * close, not as a way of saying "this happened a long time ago."
     */
    public boolean hasEnded() {
        if (endAt == null || endAt.isBlank()) return false;
        try {
            return OffsetDateTime.parse(endAt).toInstant().isBefore(Instant.now());
        } catch (DateTimeParseException exception) {
            return false;
        }
    }
}
