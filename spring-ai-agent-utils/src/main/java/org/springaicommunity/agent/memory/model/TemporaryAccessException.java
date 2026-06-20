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
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Represents a temporary/exception access grant across one or more platforms.
 * Used to detect Case 4: "On-call engineer granted temporary admin but privileges never revoked."
 *
 * <p>Example:
 * <pre>
 *   Employee "Mike Chen" was granted temporary admin on AD + AWS + Okta
 *   for incident response on 2026-03-15. Exception expired 2026-03-18
 *   but privileges were never revoked on any platform.
 * </pre>
 *
 * @param exceptionId       unique exception/ticket identifier (e.g. "EXC-001")
 * @param employeeId        employee who received the exception
 * @param displayName       display name for reporting
 * @param grantedPlatforms  platforms where elevated access was granted
 * @param grantedRoles      roles/permissions granted under the exception
 * @param grantedAt         when the exception was approved
 * @param expiresAt         when the exception was supposed to expire
 * @param reason            business justification for the exception
 * @param revokedPlatforms  platforms where access was actually revoked (subset of grantedPlatforms)
 * @param approver          who approved the exception
 *
 * @author Antigravity
 */
public record TemporaryAccessException(
        String exceptionId,
        String employeeId,
        String displayName,
        List<String> grantedPlatforms,
        List<String> grantedRoles,
        LocalDateTime grantedAt,
        LocalDateTime expiresAt,
        String reason,
        List<String> revokedPlatforms,
        String approver
) {

    /**
     * Returns true if the exception has expired but not all platforms have been revoked.
     */
    public boolean isStale() {
        return LocalDateTime.now().isAfter(expiresAt) && !isFullyRevoked();
    }

    /**
     * Returns true if all granted platforms have been revoked.
     */
    public boolean isFullyRevoked() {
        if (revokedPlatforms == null || revokedPlatforms.isEmpty()) return false;
        return revokedPlatforms.containsAll(grantedPlatforms);
    }

    /**
     * Returns platforms where access was granted but never revoked after expiry.
     */
    public List<String> unrevokedPlatforms() {
        if (revokedPlatforms == null || revokedPlatforms.isEmpty()) return grantedPlatforms;
        return grantedPlatforms.stream()
                .filter(p -> !revokedPlatforms.contains(p))
                .toList();
    }

    /**
     * Returns the number of days the exception has been expired but not fully revoked.
     */
    public long daysOverdue() {
        if (!isStale()) return 0;
        return ChronoUnit.DAYS.between(expiresAt, LocalDateTime.now());
    }
}
