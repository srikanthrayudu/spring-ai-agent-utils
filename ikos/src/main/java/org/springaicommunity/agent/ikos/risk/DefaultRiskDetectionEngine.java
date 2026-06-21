/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.risk;

import org.springaicommunity.agent.ikos.model.*;
import org.springaicommunity.agent.ikos.model.TemporaryAccessException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link RiskDetectionEngine} that checks for all
 * IKOS-defined risk patterns:
 * <ul>
 *   <li>Offboarding Gap — disabled in one platform, active in another</li>
 *   <li>Cross-Platform Admin — admin on 3+ platforms</li>
 *   <li>Dormant Admin — admin with no login > 90 days</li>
 *   <li>Privilege Spike — multiple role grants within 7 days</li>
 *   <li>Orphaned Account — no owner, still active</li>
 *   <li>Stale Exception — temporary access never revoked</li>
 * </ul>
 *
 * @author Antigravity
 */
public class DefaultRiskDetectionEngine implements RiskDetectionEngine {

    private static final long DORMANT_THRESHOLD_DAYS = 90;
    private final AtomicInteger riskCounter = new AtomicInteger(0);

    @Override
    public List<KnowledgeUnit> detectRisks(UnifiedIdentity identity) {
        List<KnowledgeUnit> risks = new ArrayList<>();

        detectOffboardingGap(identity, risks);
        detectCrossPlatformAdmin(identity, risks);
        detectDormantAdmin(identity, risks);
        detectOrphanedAccount(identity, risks);
        detectPrivilegeCreep(identity, risks);
        detectContractorExcessiveAccess(identity, risks);
        detectStaleServiceAccount(identity, risks);
        detectSoDViolation(identity, risks);
        detectCredentialRotationViolation(identity, risks);

        return risks;
    }

    @Override
    public List<KnowledgeUnit> detectRisks(List<UnifiedIdentity> identities) {
        List<KnowledgeUnit> allRisks = new ArrayList<>();
        for (UnifiedIdentity identity : identities) {
            allRisks.addAll(detectRisks(identity));
        }
        return allRisks;
    }

    // ── Risk Detection Rules ────────────────────────────────────────────────

    private void detectOffboardingGap(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        Set<String> disabled = identity.disabledPlatforms();
        Set<String> active = identity.activePlatforms();

        if (!disabled.isEmpty() && !active.isEmpty()) {
            String riskId = nextRiskId();
            String statement = String.format(
                    "OFFBOARDING GAP: %s is disabled on [%s] but still active on [%s]",
                    identity.displayName(),
                    String.join(", ", disabled),
                    String.join(", ", active));

            risks.add(buildRiskUnit(riskId, statement, identity,
                    RiskIndicator.critical(RiskType.OFFBOARDING_GAP, statement),
                    PolicyReference.pam("PAM-001", "Offboarding Synchronization",
                            "All identity platforms must be disabled within 24h of offboarding"),
                    PolicyReference.nist("AC-2", "Account Management",
                            "Remove or disable information system accounts when no longer needed")));
        }
    }

    private void detectCrossPlatformAdmin(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        long adminCount = identity.adminPlatformCount();
        if (adminCount >= 3) {
            String riskId = nextRiskId();
            Set<String> adminPlatforms = identity.accounts().stream()
                    .filter(IdentityAccount::isAdmin)
                    .map(IdentityAccount::platform)
                    .collect(java.util.stream.Collectors.toSet());

            String statement = String.format(
                    "CROSS-PLATFORM ADMIN: %s has admin privileges on %d platforms: [%s]",
                    identity.displayName(), adminCount,
                    String.join(", ", adminPlatforms));

            risks.add(buildRiskUnit(riskId, statement, identity,
                    RiskIndicator.high(RiskType.CROSS_PLATFORM_ADMIN, statement),
                    PolicyReference.nist("AC-6", "Least Privilege",
                            "Employ principle of least privilege for access authorizations"),
                    PolicyReference.pam("PAM-004", "Privileged Access Review",
                            "Admin access across 3+ platforms requires quarterly review")));
        }
    }

    private void detectDormantAdmin(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        for (IdentityAccount account : identity.accounts()) {
            if (account.isAdmin() && account.isDormant(DORMANT_THRESHOLD_DAYS) && account.isActiveOnPlatform()) {
                String riskId = nextRiskId();
                String statement = String.format(
                        "DORMANT ADMIN: %s has admin on %s but has not logged in for >%d days",
                        identity.displayName(), account.platform(), DORMANT_THRESHOLD_DAYS);

                risks.add(buildRiskUnit(riskId, statement, identity,
                        RiskIndicator.high(RiskType.DORMANT_ADMIN, statement),
                        PolicyReference.pam("PAM-003", "Inactive Privileged Account Review",
                                "Admin accounts inactive for 90+ days must be reviewed and disabled"),
                        PolicyReference.nist("AC-2(3)", "Disable Inactive Accounts",
                                "Automatically disable information system accounts after 90 days of inactivity")));
            }
        }
    }

    private void detectOrphanedAccount(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        // An "orphaned" identity has active accounts but no employee ID and no manager
        boolean noOwner = (identity.employeeId() == null || identity.employeeId().isBlank())
                && (identity.metadata() == null || !identity.metadata().containsKey("manager"));
        boolean hasActive = !identity.activePlatforms().isEmpty();

        if (noOwner && hasActive) {
            String riskId = nextRiskId();
            String statement = String.format(
                    "ORPHANED ACCOUNT: %s has active accounts on [%s] but no identifiable owner",
                    identity.displayName(),
                    String.join(", ", identity.activePlatforms()));

            risks.add(buildRiskUnit(riskId, statement, identity,
                    RiskIndicator.medium(RiskType.ORPHANED_ACCOUNT, statement),
                    PolicyReference.accessControl("AC-001", "Account Ownership",
                            "All active accounts must have an identified owner and manager")));
        }
    }

    private void detectPrivilegeCreep(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        EffectivePrivilegeProfile profile = EffectivePrivilegeProfile.fromIdentity(identity);
        if (profile.totalPermissionCount() > 30 && profile.sensitivePermissions().size() > 5) {
            String riskId = nextRiskId();
            String statement = String.format(
                    "PRIVILEGE CREEP: %s has %d total permissions including %d sensitive permissions across %d platforms",
                    identity.displayName(), profile.totalPermissionCount(),
                    profile.sensitivePermissions().size(), identity.platforms().size());

            risks.add(buildRiskUnit(riskId, statement, identity,
                    RiskIndicator.medium(RiskType.PRIVILEGE_CREEP, statement),
                    PolicyReference.nist("AC-6(1)", "Authorize Access to Security Functions",
                            "Explicitly authorize access to security-relevant information"),
                    PolicyReference.pam("PAM-005", "Periodic Privilege Review",
                            "Accumulated privileges must be reviewed and right-sized quarterly")));
        }
    }

    private void detectContractorExcessiveAccess(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        boolean isContractor = (identity.employeeId() == null || identity.employeeId().isBlank())
                && identity.displayName() != null
                && (identity.displayName().toLowerCase().contains("contractor")
                    || identity.displayName().toLowerCase().contains("ext")
                    || identity.accounts().stream().anyMatch(a -> a.accountId().contains("ext") || a.accountId().contains("contractor")
                        || (a.email() != null && !a.email().endsWith("@acme.com") && !a.email().isBlank())));

        if (isContractor) {
            EffectivePrivilegeProfile prof = EffectivePrivilegeProfile.fromIdentity(identity);
            if (prof.totalPermissionCount() > 5 || !prof.sensitivePermissions().isEmpty()) {
                String riskId = nextRiskId();
                String statement = String.format(
                        "CONTRACTOR EXCESSIVE ACCESS: %s (non-employee) has %d permissions incl. %d sensitive across %d platforms",
                        identity.displayName(), prof.totalPermissionCount(),
                        prof.sensitivePermissions().size(), identity.platforms().size());

                risks.add(buildRiskUnit(riskId, statement, identity,
                        RiskIndicator.high(RiskType.PRIVILEGE_CREEP, statement),
                        PolicyReference.pam("PAM-007", "Third-Party Access Control",
                                "Contractors must follow least-privilege with quarterly access recertification"),
                        PolicyReference.nist("AC-6(5)", "Privileged Accounts",
                                "Restrict privileged accounts to only those personnel with explicit need")));
            }
        }
    }

    private void detectStaleServiceAccount(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        boolean isSvcAccount = identity.displayName() != null
                && (identity.displayName().toLowerCase().contains("service")
                    || identity.displayName().toLowerCase().contains("svc")
                    || identity.displayName().toLowerCase().contains("pipeline"));

        if (isSvcAccount) {
            for (IdentityAccount account : identity.accounts()) {
                if (account.isAdmin() && account.isDormant(180) && account.isActiveOnPlatform()) {
                    String riskId = nextRiskId();
                    String statement = String.format(
                            "STALE SERVICE ACCOUNT: %s has admin on %s, last used >180 days ago — credential rotation required",
                            identity.displayName(), account.platform());

                    risks.add(buildRiskUnit(riskId, statement, identity,
                            RiskIndicator.critical(RiskType.TOKEN_ABUSE, statement),
                            PolicyReference.pam("PAM-008", "Service Account Lifecycle",
                                    "Service accounts with admin must rotate credentials every 90 days"),
                            PolicyReference.nist("IA-5(1)", "Password-Based Authentication",
                                    "Enforce password/key rotation for all automated/service accounts")));
                }
            }
        }
    }

    private void detectSoDViolation(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        // SoD: admin on identity provider (Okta/AD) AND cloud infra (AWS) = toxic combination
        boolean adminOnIdp = identity.accounts().stream()
                .anyMatch(a -> a.isAdmin() && (a.platform().equalsIgnoreCase("Okta")
                        || a.platform().equalsIgnoreCase("ActiveDirectory")));
        boolean adminOnCloud = identity.accounts().stream()
                .anyMatch(a -> a.isAdmin() && (a.platform().equalsIgnoreCase("AWS_IAM")
                        || a.platform().equalsIgnoreCase("Azure")));

        if (adminOnIdp && adminOnCloud) {
            String riskId = nextRiskId();
            String statement = String.format(
                    "SOD VIOLATION: %s has admin on both identity provider AND cloud infrastructure — toxic privilege combination",
                    identity.displayName());

            risks.add(buildRiskUnit(riskId, statement, identity,
                    RiskIndicator.critical(RiskType.SOD_VIOLATION, statement),
                    PolicyReference.pam("PAM-009", "Separation of Duties",
                            "No single identity may have admin on both identity provider and cloud infrastructure"),
                    PolicyReference.nist("AC-5", "Separation of Duties",
                            "Separate duties of individuals to prevent malicious activity without collusion")));
        }
    }

    /**
     * Detects credential rotation violations: API tokens/service accounts with
     * credentials older than policy threshold (365 days for tokens, 90 days for service accounts).
     * Addresses Case 3: "Developer's API token never rotated, discovered 6 months later."
     */
    private void detectCredentialRotationViolation(UnifiedIdentity identity, List<KnowledgeUnit> risks) {
        for (IdentityAccount account : identity.accounts()) {
            if (!account.isActiveOnPlatform()) continue;

            int threshold = account.accountType() == IdentityAccount.AccountType.API_TOKEN ? 365
                    : account.accountType() == IdentityAccount.AccountType.SERVICE_ACCOUNT ? 90
                    : -1;

            if (threshold > 0 && account.hasStaleCredential(threshold)) {
                String riskId = nextRiskId();
                String statement = String.format(
                        "CREDENTIAL ROTATION VIOLATION: %s on %s has credentials older than %d days — rotation required",
                        identity.displayName(), account.platform(), threshold);

                risks.add(buildRiskUnit(riskId, statement, identity,
                        RiskIndicator.medium(RiskType.TOKEN_ABUSE, statement),
                        PolicyReference.pam("PAM-008", "Credential Lifecycle Management",
                                "Credentials must be rotated per policy: 90 days for service accounts, 365 days for API tokens"),
                        PolicyReference.nist("IA-5(1)", "Authenticator Management",
                                "Enforce credential rotation for all automated and service accounts")));
            }
        }
    }

    // ── Stale Exception Detection ────────────────────────────────────────────

    /**
     * Detects stale temporary access exceptions — elevated access that was never revoked.
     * Addresses Case 4: "On-call engineer granted temporary admin, privileges never revoked."
     *
     * @param exceptions list of temporary access exception records
     * @return list of risk knowledge units for stale exceptions
     */
    public List<KnowledgeUnit> detectStaleExceptions(List<TemporaryAccessException> exceptions) {
        List<KnowledgeUnit> risks = new ArrayList<>();
        if (exceptions == null) return risks;

        for (TemporaryAccessException exc : exceptions) {
            if (exc.isStale()) {
                String riskId = nextRiskId();
                String statement = String.format(
                        "STALE EXCEPTION: %s was granted temporary admin on [%s] for '%s' — expired %d days ago but never revoked on [%s]",
                        exc.displayName(),
                        String.join(", ", exc.grantedPlatforms()),
                        exc.reason(),
                        exc.daysOverdue(),
                        String.join(", ", exc.unrevokedPlatforms()));

                List<Evidence> evidence = List.of(
                        new Evidence("exception:" + exc.exceptionId(),
                                String.format("Exception %s: granted %s, expired %s, approver: %s",
                                        exc.exceptionId(), exc.grantedAt(), exc.expiresAt(), exc.approver()),
                                0.9),
                        new Evidence("unrevoked-platforms",
                                "Still active on: " + String.join(", ", exc.unrevokedPlatforms()),
                                0.85));

                risks.add(KnowledgeUnit.builder()
                        .id(riskId)
                        .statement(statement)
                        .type(KnowledgeType.RISK_OBSERVATION)
                        .state(KnowledgeState.OBSERVATION)
                        .context(String.format("Identity: %s (%s) | Risk: STALE_EXCEPTION (CRITICAL) | Overdue: %d days",
                                exc.displayName(), exc.employeeId(), exc.daysOverdue()))
                        .evidence(evidence)
                        .confidence(0.95)
                        .lastReviewed(LocalDateTime.now())
                        .build());
            }
        }
        return risks;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private KnowledgeUnit buildRiskUnit(String riskId, String statement, UnifiedIdentity identity,
                                        RiskIndicator risk, PolicyReference... policies) {
        List<Evidence> evidence = identity.accounts().stream()
                .map(a -> new Evidence(
                        a.platform() + ":" + a.accountId(),
                        String.format("[%s] %s — status: %s, admin: %s, lastLogin: %s",
                                a.platform(), a.accountId(), a.status(), a.isAdmin(),
                                a.lastLogin() != null ? a.lastLogin().toString() : "never"),
                        a.isAdmin() ? 0.8 : 0.5))
                .toList();

        return KnowledgeUnit.builder()
                .id(riskId)
                .statement(statement)
                .type(KnowledgeType.RISK_OBSERVATION)
                .state(KnowledgeState.OBSERVATION)
                .context(String.format("Identity: %s (%s) | Platforms: %s | Risk: %s (%s)",
                        identity.displayName(), identity.unifiedId(),
                        String.join(", ", identity.platforms()),
                        risk.riskType(), risk.severity()))
                .evidence(evidence)
                .confidence(risk.riskScore())
                .lastReviewed(LocalDateTime.now())
                .build();
    }

    private String nextRiskId() {
        return "RISK-" + String.format("%04d", riskCounter.incrementAndGet());
    }
}
