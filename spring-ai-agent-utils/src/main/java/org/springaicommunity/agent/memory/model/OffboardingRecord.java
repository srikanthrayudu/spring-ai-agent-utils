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

import java.time.LocalDate;
import java.util.Map;

/**
 * Represents an offboarding/termination record from the HR system.
 * Used for proactive offboarding gap detection.
 *
 * @param employeeId        employee identifier
 * @param displayName       employee name
 * @param terminationDate   date of termination/offboarding
 * @param platformDisableStatus  map of platform → disabled (true/false)
 * @param terminationType   type of termination
 *
 * @author Antigravity
 */
public record OffboardingRecord(
        String employeeId,
        String displayName,
        LocalDate terminationDate,
        Map<String, Boolean> platformDisableStatus,
        TerminationType terminationType
) {

    public enum TerminationType {
        VOLUNTARY,
        INVOLUNTARY,
        CONTRACT_END,
        TRANSFER,
        RETIREMENT
    }

    /**
     * Returns platforms where the account is still active after termination.
     */
    public java.util.List<String> activePlatformsPostTermination() {
        return platformDisableStatus.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Returns the number of days since termination.
     */
    public long daysSinceTermination() {
        return java.time.temporal.ChronoUnit.DAYS.between(terminationDate, LocalDate.now());
    }

    /**
     * Returns true if there's an offboarding gap (some platforms still active).
     */
    public boolean hasGap() {
        return !activePlatformsPostTermination().isEmpty();
    }
}
