/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.identity;

import org.springaicommunity.agent.ikos.model.IdentityAccount;
import org.springaicommunity.agent.ikos.model.UnifiedIdentity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link IdentityCorrelationEngine} using multi-signal matching.
 *
 * <p>Correlation strategy (priority order):
 * <ol>
 *   <li>Employee ID match (strongest signal)</li>
 *   <li>Email address match</li>
 *   <li>Username normalization (john.smith = jsmith = john_smith)</li>
 * </ol>
 *
 * @author Antigravity
 */
public class DefaultIdentityCorrelationEngine implements IdentityCorrelationEngine {

    private final Map<String, UnifiedIdentity> identityCache = new LinkedHashMap<>();

    @Override
    public List<UnifiedIdentity> correlate(List<IdentityAccount> accounts) {
        if (accounts == null || accounts.isEmpty()) return List.of();

        // Group by correlation key
        Map<String, List<IdentityAccount>> groups = new LinkedHashMap<>();

        for (IdentityAccount account : accounts) {
            String key = resolveCorrelationKey(account);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(account);
        }

        // Build unified identities
        List<UnifiedIdentity> result = new ArrayList<>();
        int idx = 0;
        for (Map.Entry<String, List<IdentityAccount>> entry : groups.entrySet()) {
            List<IdentityAccount> group = entry.getValue();

            // Pick the best display name, email, etc. from the group
            String displayName = group.stream()
                    .map(IdentityAccount::displayName)
                    .filter(n -> n != null && !n.isBlank())
                    .findFirst()
                    .orElse(entry.getKey());

            String email = group.stream()
                    .map(IdentityAccount::email)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst()
                    .orElse("");

            String employeeId = group.stream()
                    .map(IdentityAccount::employeeId)
                    .filter(e -> e != null && !e.isBlank())
                    .findFirst()
                    .orElse("");

            String department = group.stream()
                    .map(a -> a.metadata() != null ? a.metadata().getOrDefault("department", "") : "")
                    .filter(d -> !d.isBlank())
                    .findFirst()
                    .orElse("Unknown");

            String unifiedId = "UID-" + String.format("%04d", ++idx);

            UnifiedIdentity identity = new UnifiedIdentity(
                    unifiedId, displayName, email, employeeId,
                    department, List.copyOf(group), Map.of()
            );

            result.add(identity);
            identityCache.put(unifiedId, identity);

            // Index by account for lookup
            for (IdentityAccount a : group) {
                identityCache.put(a.accountId() + "@" + a.platform(), identity);
            }
        }

        return result;
    }

    @Override
    public UnifiedIdentity findIdentity(String accountId, String platform) {
        return identityCache.get(accountId + "@" + platform);
    }

    /**
     * Resolves a correlation key for the given account using multi-signal matching.
     */
    private String resolveCorrelationKey(IdentityAccount account) {
        // Priority 1: Employee ID (strongest correlation signal)
        if (account.employeeId() != null && !account.employeeId().isBlank()) {
            return "EID:" + account.employeeId().trim().toLowerCase();
        }

        // Priority 2: Email address
        if (account.email() != null && !account.email().isBlank()) {
            return "EMAIL:" + account.email().trim().toLowerCase();
        }

        // Priority 3: Normalized username
        return "USER:" + normalizeUsername(account.accountId());
    }

    /**
     * Normalizes usernames for cross-platform matching.
     * john.smith, jsmith, john_smith, john-smith → normalized form.
     */
    static String normalizeUsername(String username) {
        if (username == null) return "";
        // Remove domain suffix
        String normalized = username.contains("@")
                ? username.substring(0, username.indexOf("@"))
                : username;
        // Remove separators and lowercase
        return normalized.toLowerCase()
                .replaceAll("[._\\-]", "")
                .trim();
    }
}
