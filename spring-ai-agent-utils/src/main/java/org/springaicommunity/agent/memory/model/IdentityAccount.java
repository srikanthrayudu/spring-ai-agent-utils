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
import java.util.Map;

/**
 * Represents a single account on a specific identity platform.
 *
 * <p>Multiple {@code IdentityAccount} records are correlated into a single
 * {@link UnifiedIdentity} by the Identity Correlation Engine.
 *
 * @param accountId              platform-specific identifier (e.g. "john.smith", "jsmith@corp.com")
 * @param platform               identity platform name (e.g. "ActiveDirectory", "AWS_IAM", "Okta")
 * @param displayName            human-readable display name
 * @param email                  email address if available
 * @param employeeId             employee identifier for cross-platform matching
 * @param status                 account status: ACTIVE, DISABLED, LOCKED, SUSPENDED
 * @param roles                  assigned roles on this platform
 * @param groups                 group memberships on this platform
 * @param isAdmin                whether this account has admin-level privileges
 * @param lastLogin              timestamp of last authentication event
 * @param createdAt              when the account was created
 * @param metadata               additional platform-specific attributes
 * @param accountType            type of account: EMPLOYEE, CONTRACTOR, SERVICE_ACCOUNT, API_TOKEN
 * @param credentialAgeDays      days since credential was last created or rotated (-1 if unknown)
 * @param lastCredentialRotation timestamp of last credential/key rotation (null if never rotated)
 *
 * @author Antigravity
 */
public record IdentityAccount(
        String accountId,
        String platform,
        String displayName,
        String email,
        String employeeId,
        AccountStatus status,
        List<String> roles,
        List<String> groups,
        boolean isAdmin,
        LocalDateTime lastLogin,
        LocalDateTime createdAt,
        Map<String, String> metadata,
        AccountType accountType,
        int credentialAgeDays,
        LocalDateTime lastCredentialRotation
) {

    /**
     * Backward-compatible constructor without the new fields.
     * Sets accountType based on heuristics, credentialAgeDays to -1, rotation to null.
     */
    public IdentityAccount(String accountId, String platform, String displayName,
                           String email, String employeeId, AccountStatus status,
                           List<String> roles, List<String> groups, boolean isAdmin,
                           LocalDateTime lastLogin, LocalDateTime createdAt,
                           Map<String, String> metadata) {
        this(accountId, platform, displayName, email, employeeId, status,
                roles, groups, isAdmin, lastLogin, createdAt, metadata,
                inferAccountType(accountId, displayName, employeeId),
                -1, null);
    }

    public enum AccountStatus {
        ACTIVE,
        DISABLED,
        LOCKED,
        SUSPENDED,
        PENDING_DELETION
    }

    /**
     * Classifies the type of identity account.
     */
    public enum AccountType {
        /** Full-time or part-time employee with HR record. */
        EMPLOYEE,
        /** External contractor, vendor, or consultant. */
        CONTRACTOR,
        /** Non-human service account for automated processes. */
        SERVICE_ACCOUNT,
        /** API token or key-based access credential. */
        API_TOKEN
    }

    /**
     * Returns true if the account has not logged in for the given number of days.
     */
    public boolean isDormant(long thresholdDays) {
        if (lastLogin == null) return true;
        return lastLogin.isBefore(LocalDateTime.now().minusDays(thresholdDays));
    }

    /**
     * Returns true if the account is active but its owner is disabled elsewhere.
     */
    public boolean isActiveOnPlatform() {
        return status == AccountStatus.ACTIVE;
    }

    /**
     * Returns true if this is a service account or API token.
     */
    public boolean isNonHuman() {
        return accountType == AccountType.SERVICE_ACCOUNT || accountType == AccountType.API_TOKEN;
    }

    /**
     * Returns true if this is a contractor account.
     */
    public boolean isContractor() {
        return accountType == AccountType.CONTRACTOR;
    }

    /**
     * Returns true if the credential has not been rotated within the given threshold.
     */
    public boolean hasStaleCredential(int thresholdDays) {
        if (credentialAgeDays >= 0) return credentialAgeDays > thresholdDays;
        if (lastCredentialRotation != null) {
            return ChronoUnit.DAYS.between(lastCredentialRotation, LocalDateTime.now()) > thresholdDays;
        }
        // If no rotation info and account is old, consider stale
        if (createdAt != null) {
            return ChronoUnit.DAYS.between(createdAt, LocalDateTime.now()) > thresholdDays;
        }
        return false;
    }

    /**
     * Infers account type from naming conventions when not explicitly set.
     */
    private static AccountType inferAccountType(String accountId, String displayName, String employeeId) {
        String id = (accountId != null ? accountId : "").toLowerCase();
        String name = (displayName != null ? displayName : "").toLowerCase();
        if (id.startsWith("svc-") || id.startsWith("svc_") || name.contains("service account")
                || name.contains("svc") || name.contains("pipeline")) {
            return AccountType.SERVICE_ACCOUNT;
        }
        if (id.contains("api-") || id.contains("token") || id.contains("bot-")) {
            return AccountType.API_TOKEN;
        }
        if (id.contains("ext") || id.contains("contractor") || id.contains("vendor")
                || name.contains("contractor") || name.contains("vendor")
                || (employeeId == null || employeeId.isBlank())) {
            // No employee ID often means external
            if (name.contains("service") || name.contains("svc")) return AccountType.SERVICE_ACCOUNT;
            if (employeeId == null || employeeId.isBlank()) return AccountType.CONTRACTOR;
        }
        return AccountType.EMPLOYEE;
    }
}
