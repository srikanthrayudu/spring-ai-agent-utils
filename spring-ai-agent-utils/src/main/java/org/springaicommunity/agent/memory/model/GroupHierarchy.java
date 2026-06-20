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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a nested group hierarchy for computing effective permissions
 * through group traversal. Handles the "hidden privilege through nested groups"
 * scenario (e.g., Case 2: svc-etl inherited Global Admin through nested Azure AD group).
 *
 * <pre>
 *   User → IT-Team → AD-Admins → Domain Admins (hidden admin)
 *   User → Cloud-Devs → AWS-PowerUsers → S3FullAccess
 * </pre>
 *
 * @author Antigravity
 */
public class GroupHierarchy {

    /**
     * Represents a single group node in the hierarchy.
     */
    public record GroupNode(
            String groupName,
            String platform,
            Set<String> parentGroups,
            Set<String> grantedPermissions,
            boolean isAdminGroup
    ) {}

    private final Map<String, GroupNode> groups = new LinkedHashMap<>();

    /**
     * Registers a group in the hierarchy.
     */
    public GroupHierarchy addGroup(String name, String platform,
                                   Set<String> parents, Set<String> permissions, boolean isAdmin) {
        groups.put(key(name, platform), new GroupNode(name, platform, parents, permissions, isAdmin));
        return this;
    }

    /**
     * Resolves all effective permissions for a user's direct group memberships,
     * traversing the entire parent chain.
     *
     * @param directGroups the user's directly assigned groups on a platform
     * @param platform     the platform to resolve within
     * @return all effective permissions (direct + inherited)
     */
    public Set<String> resolveEffectivePermissions(List<String> directGroups, String platform) {
        Set<String> effectivePerms = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();

        for (String group : directGroups) {
            traverseGroup(group, platform, effectivePerms, visited);
        }

        return effectivePerms;
    }

    /**
     * Resolves whether the user inherits admin through nested group membership.
     *
     * @param directGroups user's directly assigned groups
     * @param platform     the platform
     * @return true if admin is inherited through any group chain
     */
    public boolean inheritsAdmin(List<String> directGroups, String platform) {
        Set<String> visited = new HashSet<>();
        for (String group : directGroups) {
            if (checkAdminInherited(group, platform, visited)) return true;
        }
        return false;
    }

    /**
     * Returns the full inheritance chain from a group to admin (for explainability).
     *
     * @param directGroups user's groups
     * @param platform     the platform
     * @return list of group chains that lead to admin (e.g., "IT-Team → AD-Admins → Domain Admins")
     */
    public List<String> getAdminInheritanceChains(List<String> directGroups, String platform) {
        List<String> chains = new ArrayList<>();
        for (String group : directGroups) {
            List<String> path = new ArrayList<>();
            findAdminPaths(group, platform, path, chains, new HashSet<>());
        }
        return chains;
    }

    /**
     * Returns the total number of registered groups.
     */
    public int size() {
        return groups.size();
    }

    /**
     * Returns all registered group names.
     */
    public Set<String> allGroupNames() {
        return groups.values().stream().map(GroupNode::groupName).collect(Collectors.toSet());
    }

    // ── Private Traversal ───────────────────────────────────────────────────

    private void traverseGroup(String groupName, String platform,
                                Set<String> effectivePerms, Set<String> visited) {
        String k = key(groupName, platform);
        if (visited.contains(k)) return; // prevent cycles
        visited.add(k);

        GroupNode node = groups.get(k);
        if (node == null) {
            // Group not in hierarchy — add the group name itself as a permission
            effectivePerms.add(groupName);
            return;
        }

        // Add this group's granted permissions
        effectivePerms.addAll(node.grantedPermissions());

        // Traverse parent groups
        for (String parent : node.parentGroups()) {
            traverseGroup(parent, platform, effectivePerms, visited);
        }
    }

    private boolean checkAdminInherited(String groupName, String platform, Set<String> visited) {
        String k = key(groupName, platform);
        if (visited.contains(k)) return false;
        visited.add(k);

        GroupNode node = groups.get(k);
        if (node == null) return false;
        if (node.isAdminGroup()) return true;

        for (String parent : node.parentGroups()) {
            if (checkAdminInherited(parent, platform, visited)) return true;
        }
        return false;
    }

    private void findAdminPaths(String groupName, String platform,
                                 List<String> currentPath, List<String> results, Set<String> visited) {
        String k = key(groupName, platform);
        if (visited.contains(k)) return;
        visited.add(k);
        currentPath.add(groupName);

        GroupNode node = groups.get(k);
        if (node != null) {
            if (node.isAdminGroup()) {
                results.add(String.join(" → ", currentPath));
            }
            for (String parent : node.parentGroups()) {
                findAdminPaths(parent, platform, new ArrayList<>(currentPath), results, new HashSet<>(visited));
            }
        }

        // Don't remove from currentPath since we copy above
    }

    private static String key(String group, String platform) {
        return platform + ":" + group;
    }

    // ── Factory: Build a realistic enterprise hierarchy ─────────────────────

    /**
     * Creates a realistic enterprise group hierarchy covering AD, AWS, Okta.
     */
    public static GroupHierarchy buildEnterpriseHierarchy() {
        GroupHierarchy h = new GroupHierarchy();

        // ═══ Active Directory ═══
        h.addGroup("Domain Users", "ActiveDirectory", Set.of(), Set.of("BasicLogin", "ReadSharedDrives"), false);
        h.addGroup("IT-Team", "ActiveDirectory", Set.of("Domain Users"), Set.of("RemoteDesktop", "VPN-Access"), false);
        h.addGroup("Security-Team", "ActiveDirectory", Set.of("Domain Users"), Set.of("SIEM-Access", "LogReview"), false);
        h.addGroup("Contractors", "ActiveDirectory", Set.of("Domain Users"), Set.of("LimitedVPN"), false);
        h.addGroup("AD-Admins", "ActiveDirectory", Set.of("IT-Team"), Set.of("GroupPolicyEdit", "UserManagement"), false);
        h.addGroup("Domain Admins", "ActiveDirectory", Set.of("AD-Admins"), Set.of("DomainAdmin", "SchemaAdmin", "FullControl"), true);
        h.addGroup("Schema Admins", "ActiveDirectory", Set.of("Domain Admins"), Set.of("SchemaModify"), true);
        h.addGroup("Enterprise Admins", "ActiveDirectory", Set.of("Domain Admins"), Set.of("ForestAdmin", "CrossDomainAdmin"), true);
        h.addGroup("Remote Desktop Users", "ActiveDirectory", Set.of("Domain Users"), Set.of("RemoteDesktop"), false);
        h.addGroup("VPN-Users", "ActiveDirectory", Set.of("Domain Users"), Set.of("VPN-Access"), false);

        // ═══ AWS IAM ═══
        h.addGroup("ReadOnly", "AWS_IAM", Set.of(), Set.of("s3:GetObject", "ec2:Describe*"), false);
        h.addGroup("DevOps", "AWS_IAM", Set.of("ReadOnly"), Set.of("ec2:*", "s3:*", "lambda:*"), false);
        h.addGroup("CloudAdmins", "AWS_IAM", Set.of("DevOps"), Set.of("iam:*", "organizations:*", "AdministratorAccess"), true);
        h.addGroup("ContractorDevs", "AWS_IAM", Set.of("ReadOnly"), Set.of("s3:PutObject", "lambda:InvokeFunction"), false);
        h.addGroup("CI-CD", "AWS_IAM", Set.of("DevOps"), Set.of("codepipeline:*", "codebuild:*", "ecr:*"), false);
        h.addGroup("SecurityAudit", "AWS_IAM", Set.of("ReadOnly"), Set.of("cloudtrail:*", "guardduty:*", "config:*"), false);

        // ═══ Okta ═══
        h.addGroup("Everyone", "Okta", Set.of(), Set.of("SSO-Login", "Self-Service"), false);
        h.addGroup("IT-Admins", "Okta", Set.of("Everyone"), Set.of("AppAssignment", "GroupManagement"), false);
        h.addGroup("OktaAdmins", "Okta", Set.of("IT-Admins"), Set.of("SuperAdmin", "PolicyAdmin", "ReportAdmin"), true);
        h.addGroup("Vendor-Access", "Okta", Set.of("Everyone"), Set.of("LimitedSSO"), false);

        // ═══ Salesforce ═══
        h.addGroup("Standard-Users", "Salesforce", Set.of(), Set.of("Read", "CreateRecords"), false);
        h.addGroup("Vendor-Access", "Salesforce", Set.of("Standard-Users"), Set.of("LimitedRead"), false);
        h.addGroup("SalesAdmins", "Salesforce", Set.of("Standard-Users"), Set.of("SystemAdmin", "ModifyAllData", "ManageUsers"), true);

        // ═══ ServiceNow ═══
        h.addGroup("Users", "ServiceNow", Set.of(), Set.of("ReadIncidents", "CreateIncidents"), false);
        h.addGroup("ServiceAccounts", "ServiceNow", Set.of("Users"), Set.of("admin", "itil", "asset", "cmdb_write"), true);

        return h;
    }
}
