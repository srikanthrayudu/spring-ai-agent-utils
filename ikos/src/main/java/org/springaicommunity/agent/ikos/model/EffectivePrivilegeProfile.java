/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the computed effective privileges of a {@link UnifiedIdentity}
 * across all platforms, after group traversal and permission expansion.
 *
 * <p>This is not assigned privilege — this is <b>effective privilege</b>:
 * <pre>
 *   User → Group A → Group B → Global Admin
 * </pre>
 *
 * @param identityId             unified identity ID
 * @param displayName            identity display name
 * @param platformPrivileges     map of platform → list of effective permissions
 * @param adminPlatforms         platforms where effective privilege includes admin
 * @param sensitivePermissions   high-risk permissions detected
 * @param totalPermissionCount   total count of effective permissions across all platforms
 * @param privilegeRiskScore     computed privilege risk score [0.0–1.0]
 * @param inheritanceChains      admin inheritance chains for explainability
 *
 * @author Antigravity
 */
public record EffectivePrivilegeProfile(
        String identityId,
        String displayName,
        Map<String, List<String>> platformPrivileges,
        Set<String> adminPlatforms,
        List<String> sensitivePermissions,
        int totalPermissionCount,
        double privilegeRiskScore,
        List<String> inheritanceChains
) {

    /**
     * Computes effective privilege without group hierarchy (flat merge of roles + groups).
     */
    public static EffectivePrivilegeProfile fromIdentity(UnifiedIdentity identity) {
        return fromIdentity(identity, null);
    }

    /**
     * Computes effective privilege WITH group hierarchy traversal.
     * This resolves hidden admin through nested group membership.
     *
     * @param identity  the unified identity
     * @param hierarchy the group hierarchy for traversal (null for flat merge)
     */
    public static EffectivePrivilegeProfile fromIdentity(UnifiedIdentity identity, GroupHierarchy hierarchy) {
        Map<String, List<String>> privMap = new LinkedHashMap<>();
        Set<String> adminPlats = new LinkedHashSet<>();
        List<String> chains = new ArrayList<>();

        for (IdentityAccount account : identity.accounts()) {
            List<String> effectivePerms = new ArrayList<>();

            if (hierarchy != null) {
                // ═══ WITH HIERARCHY: Traverse nested groups ═══
                // Resolve effective permissions through parent chain
                Set<String> resolvedPerms = hierarchy.resolveEffectivePermissions(
                        account.groups(), account.platform());
                effectivePerms.addAll(resolvedPerms);

                // Also add directly assigned roles
                effectivePerms.addAll(account.roles());

                // Check if admin is inherited through groups
                boolean inheritsAdmin = hierarchy.inheritsAdmin(account.groups(), account.platform());
                if (account.isAdmin() || inheritsAdmin) {
                    adminPlats.add(account.platform());
                }

                // Get inheritance chains for explainability
                List<String> adminChains = hierarchy.getAdminInheritanceChains(
                        account.groups(), account.platform());
                for (String chain : adminChains) {
                    chains.add(account.platform() + ": " + chain);
                }
            } else {
                // ═══ WITHOUT HIERARCHY: Flat merge ═══
                effectivePerms.addAll(account.roles());
                effectivePerms.addAll(account.groups());

                if (account.isAdmin()) {
                    adminPlats.add(account.platform());
                }
            }

            // Deduplicate and store
            privMap.merge(account.platform(), effectivePerms, (a, b) -> {
                a.addAll(b);
                return a;
            });
        }

        // Detect sensitive permissions
        List<String> sensitive = privMap.values().stream()
                .flatMap(List::stream)
                .filter(EffectivePrivilegeProfile::isSensitivePermission)
                .distinct()
                .toList();

        int totalPerms = privMap.values().stream().mapToInt(List::size).sum();
        double riskScore = computePrivilegeRisk(adminPlats.size(), sensitive.size(), totalPerms);

        return new EffectivePrivilegeProfile(
                identity.unifiedId(), identity.displayName(),
                privMap, adminPlats, sensitive, totalPerms, riskScore, chains);
    }

    private static boolean isSensitivePermission(String permission) {
        String lower = permission.toLowerCase();
        return lower.contains("admin") || lower.contains("root")
                || lower.contains("superuser") || lower.contains("global")
                || lower.contains("owner") || lower.contains("full_access")
                || lower.contains("fullaccess") || lower.contains("fullcontrol")
                || lower.contains("delete") || lower.contains("iam:")
                || lower.contains("domainadmin") || lower.contains("schemaadmin")
                || lower.contains("forestadmin") || lower.contains("modifyalldata");
    }

    private static double computePrivilegeRisk(int adminCount, int sensitiveCount, int totalPerms) {
        double score = 0.0;
        if (adminCount >= 3) score += 0.40;
        else if (adminCount >= 2) score += 0.25;
        else if (adminCount >= 1) score += 0.15;

        if (sensitiveCount > 10) score += 0.30;
        else if (sensitiveCount > 5) score += 0.20;
        else if (sensitiveCount > 0) score += 0.10;

        if (totalPerms > 50) score += 0.20;
        else if (totalPerms > 20) score += 0.10;

        return Math.min(1.0, score);
    }

    /**
     * Returns true if this identity has hidden admin through nested groups.
     */
    public boolean hasHiddenAdmin() {
        return !inheritanceChains.isEmpty();
    }
}
