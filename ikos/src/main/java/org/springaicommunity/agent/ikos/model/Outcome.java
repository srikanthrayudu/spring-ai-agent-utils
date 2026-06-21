package org.springaicommunity.agent.ikos.model;

import java.time.LocalDateTime;

/**
 * Represents the result of applying a recommendation, used to adjust confidence.
 */
public record Outcome(
        boolean successful,
        String feedback,
        LocalDateTime timestamp
) {
}
