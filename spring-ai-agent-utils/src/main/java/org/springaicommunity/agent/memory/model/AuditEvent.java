/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a single audit event from an identity platform's log.
 * Used for behavioral analysis, anomaly detection, and privilege change tracking.
 *
 * @param eventId       unique event identifier
 * @param accountId     account that generated the event
 * @param platform      source platform (AD, AWS_IAM, Okta, etc.)
 * @param eventType     type of event
 * @param description   human-readable event description
 * @param timestamp     when the event occurred
 * @param sourceIp      originating IP address
 * @param targetResource the resource accessed or modified
 * @param success       whether the action succeeded
 * @param metadata      additional event-specific attributes
 *
 * @author Antigravity
 */
public record AuditEvent(
        String eventId,
        String accountId,
        String platform,
        EventType eventType,
        String description,
        LocalDateTime timestamp,
        String sourceIp,
        String targetResource,
        boolean success,
        Map<String, String> metadata
) {

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        PRIVILEGE_GRANT,
        PRIVILEGE_REVOKE,
        ROLE_CHANGE,
        RESOURCE_ACCESS,
        API_CALL,
        TOKEN_USAGE,
        PASSWORD_CHANGE,
        MFA_BYPASS,
        ACCOUNT_LOCKOUT,
        ACCOUNT_DISABLE,
        ACCOUNT_ENABLE,
        GROUP_ADD,
        GROUP_REMOVE
    }

    /**
     * Returns the hour of day (0-23) for time-of-day analysis.
     */
    public int hourOfDay() {
        return timestamp.getHour();
    }

    /**
     * Returns true if this event occurred during off-hours (before 7 AM or after 8 PM).
     */
    public boolean isOffHours() {
        int hour = hourOfDay();
        return hour < 7 || hour > 20;
    }

    /**
     * Returns true if this is a privilege escalation event.
     */
    public boolean isPrivilegeEscalation() {
        return eventType == EventType.PRIVILEGE_GRANT
                || eventType == EventType.ROLE_CHANGE
                || eventType == EventType.GROUP_ADD;
    }
}
