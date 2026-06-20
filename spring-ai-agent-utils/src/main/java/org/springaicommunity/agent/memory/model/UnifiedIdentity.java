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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A correlated identity that unifies accounts across multiple platforms.
 *
 * <p>The Identity Correlation Engine resolves that:
 * <pre>
 *   john.smith (AD), jsmith (AWS), john.smith@corp.com (Okta)
 *   → same person
 * </pre>
 *
 * @param unifiedId       stable unique identifier for this person
 * @param displayName     canonical display name
 * @param email           primary email address
 * @param employeeId      employee identifier used for cross-platform matching
 * @param department      organizational department
 * @param accounts        all linked platform accounts
 * @param metadata        additional attributes (manager, title, location, etc.)
 *
 * @author Antigravity
 */
public record UnifiedIdentity(
        String unifiedId,
        String displayName,
        String email,
        String employeeId,
        String department,
        List<IdentityAccount> accounts,
        Map<String, String> metadata
) {

    /**
     * Returns the set of platforms this identity has accounts on.
     */
    public Set<String> platforms() {
        return accounts.stream()
                .map(IdentityAccount::platform)
                .collect(Collectors.toSet());
    }

    /**
     * Returns the number of platforms where this identity has admin privileges.
     */
    public long adminPlatformCount() {
        return accounts.stream()
                .filter(IdentityAccount::isAdmin)
                .map(IdentityAccount::platform)
                .distinct()
                .count();
    }

    /**
     * Returns all platforms where the account is active.
     */
    public Set<String> activePlatforms() {
        return accounts.stream()
                .filter(IdentityAccount::isActiveOnPlatform)
                .map(IdentityAccount::platform)
                .collect(Collectors.toSet());
    }

    /**
     * Returns all platforms where the account is disabled.
     */
    public Set<String> disabledPlatforms() {
        return accounts.stream()
                .filter(a -> a.status() == IdentityAccount.AccountStatus.DISABLED)
                .map(IdentityAccount::platform)
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if the identity has admin on 3+ platforms (cross-platform admin risk).
     */
    public boolean isCrossPlatformAdmin() {
        return adminPlatformCount() >= 3;
    }

    /**
     * Returns true if the identity is disabled on one platform but active on another.
     */
    public boolean hasOffboardingGap() {
        Set<String> disabled = disabledPlatforms();
        Set<String> active = activePlatforms();
        return !disabled.isEmpty() && !active.isEmpty();
    }

    /**
     * Computed risk score based on identity characteristics [0.0–1.0].
     */
    public double computeRiskScore() {
        double score = 0.0;
        if (hasOffboardingGap()) score += 0.35;
        if (isCrossPlatformAdmin()) score += 0.30;
        long dormantAdminCount = accounts.stream()
                .filter(a -> a.isAdmin() && a.isDormant(90))
                .count();
        if (dormantAdminCount > 0) score += 0.25;
        if (accounts.size() > 5) score += 0.10;
        return Math.min(1.0, score);
    }
}
