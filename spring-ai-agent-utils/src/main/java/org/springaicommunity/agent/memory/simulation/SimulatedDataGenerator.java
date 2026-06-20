/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.simulation;

import org.springaicommunity.agent.memory.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates realistic simulated identity data for the IKOS hackathon demo.
 *
 * <p>Produces:
 * <ul>
 *   <li>300+ identity accounts across 5 platforms (AD, AWS, Okta, Salesforce, ServiceNow)</li>
 *   <li>Nested group/role mappings with inherited permissions</li>
 *   <li>800+ audit events (logins, privilege changes, resource access)</li>
 *   <li>50+ offboarding records with gap detection</li>
 * </ul>
 *
 * <p>Anomaly distribution (per problem statement):
 * <pre>
 *   Orphaned/stale accounts: ~12%
 *   Over-privileged identities: ~10%
 *   Privilege escalation events: ~7%
 *   Token/credential abuse: ~4%
 *   Legitimate high-privilege (false positive traps): ~18%
 *   Normal activity: ~49%
 * </pre>
 *
 * @author Antigravity
 */
public class SimulatedDataGenerator {

    private final Random random = new Random(42); // deterministic for demo
    private final AtomicInteger eventCounter = new AtomicInteger(0);

    // ── Name pools ──────────────────────────────────────────────────────────

    private static final String[] FIRST_NAMES = {
            "John", "Sarah", "Mike", "Emily", "James", "Lisa", "David", "Anna",
            "Robert", "Jennifer", "Carlos", "Priya", "Wei", "Fatima", "Ahmed",
            "Maria", "Thomas", "Yuki", "Dmitri", "Aisha", "Kevin", "Rachel",
            "Brian", "Samantha", "Alex", "Olivia", "Nathan", "Sofia", "Marcus",
            "Diana", "Eric", "Megan", "Chris", "Laura", "Patrick", "Nicole",
            "Ryan", "Jessica", "Daniel", "Michelle", "Sean", "Amanda", "Jose",
            "Stephanie", "Andrew", "Rebecca", "Tyler", "Heather", "Brandon", "Kimberly"
    };

    private static final String[] LAST_NAMES = {
            "Smith", "Jones", "Chen", "Wilson", "Brown", "Davis", "Garcia",
            "Miller", "Johnson", "Williams", "Anderson", "Thomas", "Jackson",
            "White", "Harris", "Martin", "Thompson", "Moore", "Taylor", "Lee",
            "Clark", "Rodriguez", "Lewis", "Walker", "Hall", "Allen", "Young",
            "King", "Wright", "Lopez", "Hill", "Scott", "Green", "Adams",
            "Baker", "Nelson", "Carter", "Mitchell", "Perez", "Roberts"
    };

    private static final String[] DEPARTMENTS = {
            "Engineering", "Security", "IT", "Finance", "HR", "Legal",
            "Marketing", "Sales", "Operations", "DevOps", "Data Science",
            "Product", "Customer Support", "Compliance", "Executive"
    };

    private static final String[] PLATFORMS = {
            "ActiveDirectory", "AWS_IAM", "Okta", "Salesforce", "ServiceNow"
    };

    // ── AD Roles & Groups ───────────────────────────────────────────────────

    private static final String[][] AD_NORMAL_ROLES = {
            {"Domain Users"}, {"Domain Users", "Remote Desktop Users"},
            {"Domain Users", "VPN-Users"}, {"Domain Users", "IT-Team"},
    };
    private static final String[][] AD_ADMIN_ROLES = {
            {"Domain Admins", "Schema Admins", "Enterprise Admins"},
            {"Domain Admins", "IT-Team"}, {"AD-Admins", "Security-Team"},
    };
    private static final String[][] AD_GROUPS = {
            {"IT-Team"}, {"Security-Team"}, {"Engineering"}, {"Contractors"},
            {"Finance-Users"}, {"HR-Users"}, {"Sales-Users"}, {"Remote Desktop Users"},
    };

    // ── AWS Roles ───────────────────────────────────────────────────────────

    private static final String[][] AWS_NORMAL_ROLES = {
            {"ReadOnlyAccess"}, {"AmazonS3ReadOnlyAccess", "AmazonEC2ReadOnlyAccess"},
            {"PowerUserAccess"}, {"AmazonS3ReadOnlyAccess"},
    };
    private static final String[][] AWS_ADMIN_ROLES = {
            {"AdministratorAccess", "IAMFullAccess", "SecurityAudit"},
            {"AdministratorAccess", "S3FullAccess", "EC2FullAccess"},
    };
    private static final String[][] AWS_GROUPS = {
            {"DevOps"}, {"CloudAdmins"}, {"ContractorDevs"}, {"ReadOnly"},
            {"CI-CD"}, {"SecurityAudit"}, {"DataEngineers"},
    };

    // ── Okta Roles ──────────────────────────────────────────────────────────

    private static final String[][] OKTA_NORMAL_ROLES = {
            {"ReadOnlyAdmin"}, {"AppAdmin"}, {"HelpDeskAdmin"}, {"GroupAdmin"},
    };
    private static final String[][] OKTA_ADMIN_ROLES = {
            {"SuperAdmin", "ReportAdmin"}, {"GlobalAdmin", "AppAdmin"},
    };

    // ── IP ranges ───────────────────────────────────────────────────────────

    private static final String[] CORP_IPS = {
            "10.0.1.", "10.0.2.", "10.0.3.", "172.16.0.", "172.16.1."
    };
    private static final String[] EXTERNAL_IPS = {
            "203.0.113.", "198.51.100.", "185.220.101.", "91.219.237.", "45.33.32."
    };

    // ═══════════════════════════════════════════════════════════════════════
    // Generate All Data
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Result of data generation.
     */
    public record GeneratedData(
            List<IdentityAccount> accounts,
            List<AuditEvent> auditEvents,
            List<OffboardingRecord> offboardingRecords,
            List<TemporaryAccessException> temporaryExceptions,
            GroupHierarchy groupHierarchy,
            Map<String, String> dataStats
    ) {}

    /**
     * Generates a complete simulated dataset.
     *
     * @param identityCount number of unique identities to generate
     */
    public GeneratedData generate(int identityCount) {
        List<IdentityAccount> allAccounts = new ArrayList<>();
        List<AuditEvent> allEvents = new ArrayList<>();
        List<OffboardingRecord> offboardings = new ArrayList<>();
        List<TemporaryAccessException> exceptions = new ArrayList<>();
        GroupHierarchy hierarchy = GroupHierarchy.buildEnterpriseHierarchy();

        int normalCount = 0, offboardingGapCount = 0, overPrivCount = 0;
        int dormantAdminCount = 0, tokenAbuseCount = 0, legitimateHighPrivCount = 0;
        int svcAccountCount = 0, staleExceptionCount = 0;

        for (int i = 0; i < identityCount; i++) {
            double roll = random.nextDouble();
            String empId = "EMP-" + String.format("%04d", i + 1);

            if (roll < 0.12) {
                // ~12% Offboarding gap
                allAccounts.addAll(generateOffboardingGap(i, empId));
                offboardings.add(generateOffboardingRecord(i, empId));
                offboardingGapCount++;
            } else if (roll < 0.22) {
                // ~10% Over-privileged
                allAccounts.addAll(generateOverPrivileged(i, empId));
                overPrivCount++;
            } else if (roll < 0.29) {
                // ~7% Dormant admin
                allAccounts.addAll(generateDormantAdmin(i, empId));
                dormantAdminCount++;
            } else if (roll < 0.33) {
                // ~4% Token abuse / stale service account
                allAccounts.addAll(generateServiceAccount(i));
                svcAccountCount++;
                tokenAbuseCount++;
            } else if (roll < 0.38) {
                // ~5% Stale exception (Case 4: temporary admin never revoked)
                allAccounts.addAll(generateLegitimateHighPriv(i, empId));
                exceptions.add(generateStaleException(i, empId));
                staleExceptionCount++;
            } else if (roll < 0.53) {
                // ~15% Legitimate high-privilege (false positive traps)
                allAccounts.addAll(generateLegitimateHighPriv(i, empId));
                legitimateHighPrivCount++;
            } else {
                // ~47% Normal
                allAccounts.addAll(generateNormalUser(i, empId));
                normalCount++;
            }
        }

        // Generate additional offboarding records for clean terminations
        for (int i = 0; i < 30; i++) {
            offboardings.add(generateCleanOffboarding(identityCount + i));
        }

        // Generate additional clean exceptions (properly revoked)
        for (int i = 0; i < 10; i++) {
            exceptions.add(generateCleanException(identityCount + 30 + i));
        }

        // Generate audit events (scale proportionally)
        int eventCount = Math.max(800, identityCount * 5);
        allEvents.addAll(generateAuditEvents(allAccounts, eventCount));

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Total Accounts", String.valueOf(allAccounts.size()));
        stats.put("Unique Identities", String.valueOf(identityCount));
        stats.put("Platforms", "5 (AD, AWS, Okta, Salesforce, ServiceNow)");
        stats.put("Audit Events", String.valueOf(allEvents.size()));
        stats.put("Offboarding Records", String.valueOf(offboardings.size()));
        stats.put("Temp Exceptions", String.valueOf(exceptions.size()));
        stats.put("Normal Users", String.valueOf(normalCount) + " (" + pct(normalCount, identityCount) + ")");
        stats.put("Offboarding Gaps", String.valueOf(offboardingGapCount) + " (" + pct(offboardingGapCount, identityCount) + ")");
        stats.put("Over-Privileged", String.valueOf(overPrivCount) + " (" + pct(overPrivCount, identityCount) + ")");
        stats.put("Dormant Admins", String.valueOf(dormantAdminCount) + " (" + pct(dormantAdminCount, identityCount) + ")");
        stats.put("Token/Svc Abuse", String.valueOf(tokenAbuseCount) + " (" + pct(tokenAbuseCount, identityCount) + ")");
        stats.put("Stale Exceptions", String.valueOf(staleExceptionCount) + " (" + pct(staleExceptionCount, identityCount) + ")");
        stats.put("Legit High-Priv", String.valueOf(legitimateHighPrivCount) + " (" + pct(legitimateHighPrivCount, identityCount) + ")");
        stats.put("Group Hierarchy", String.valueOf(hierarchy.size()) + " groups");

        return new GeneratedData(allAccounts, allEvents, offboardings, exceptions, hierarchy, stats);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Identity Generators by Type
    // ═══════════════════════════════════════════════════════════════════════

    private List<IdentityAccount> generateNormalUser(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;
        String email = first.toLowerCase() + "." + last.toLowerCase() + "@acme.com";
        String dept = pick(DEPARTMENTS);

        List<IdentityAccount> accounts = new ArrayList<>();

        // AD account
        accounts.add(new IdentityAccount(
                first.toLowerCase() + "." + last.toLowerCase(), "ActiveDirectory", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AD_NORMAL_ROLES)), List.of(pickArr(AD_GROUPS)),
                false, LocalDateTime.now().minusDays(random.nextInt(30)),
                LocalDateTime.now().minusYears(1 + random.nextInt(4)),
                Map.of("department", dept)));

        // 60% chance of Okta
        if (random.nextDouble() < 0.6) {
            accounts.add(new IdentityAccount(
                    email, "Okta", name, email, empId,
                    IdentityAccount.AccountStatus.ACTIVE,
                    List.of("ReadOnlyAdmin"), List.of("Everyone"),
                    false, LocalDateTime.now().minusDays(random.nextInt(14)),
                    LocalDateTime.now().minusYears(1 + random.nextInt(3)), Map.of()));
        }

        // 30% chance of AWS
        if (random.nextDouble() < 0.3) {
            accounts.add(new IdentityAccount(
                    first.charAt(0) + last.toLowerCase(), "AWS_IAM", name, email, empId,
                    IdentityAccount.AccountStatus.ACTIVE,
                    List.of(pickArr(AWS_NORMAL_ROLES)), List.of(pickArr(AWS_GROUPS)),
                    false, LocalDateTime.now().minusDays(random.nextInt(30)),
                    LocalDateTime.now().minusYears(1 + random.nextInt(3)), Map.of()));
        }

        return accounts;
    }

    private List<IdentityAccount> generateOffboardingGap(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;
        String email = first.toLowerCase() + "." + last.toLowerCase() + "@acme.com";

        List<IdentityAccount> accounts = new ArrayList<>();

        // AD: DISABLED
        accounts.add(new IdentityAccount(
                first.toLowerCase() + "." + last.toLowerCase(), "ActiveDirectory", name, email, empId,
                IdentityAccount.AccountStatus.DISABLED,
                List.of("Domain Users"), List.of("IT-Team"),
                false, LocalDateTime.now().minusDays(30 + random.nextInt(90)),
                LocalDateTime.now().minusYears(2 + random.nextInt(3)),
                Map.of("department", pick(DEPARTMENTS))));

        // AWS: still ACTIVE (the gap!)
        accounts.add(new IdentityAccount(
                first.charAt(0) + last.toLowerCase(), "AWS_IAM", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AWS_NORMAL_ROLES)), List.of(pickArr(AWS_GROUPS)),
                random.nextDouble() < 0.3,
                LocalDateTime.now().minusDays(random.nextInt(60)),
                LocalDateTime.now().minusYears(2 + random.nextInt(3)), Map.of()));

        // 50% also still active on Okta
        if (random.nextDouble() < 0.5) {
            accounts.add(new IdentityAccount(
                    email, "Okta", name, email, empId,
                    IdentityAccount.AccountStatus.ACTIVE,
                    List.of("ReadOnlyAdmin"), List.of("Everyone"),
                    false, LocalDateTime.now().minusDays(random.nextInt(30)),
                    LocalDateTime.now().minusYears(2), Map.of()));
        }

        return accounts;
    }

    private List<IdentityAccount> generateOverPrivileged(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;
        String email = first.toLowerCase() + "." + last.toLowerCase() + "@acme.com";

        List<IdentityAccount> accounts = new ArrayList<>();

        // Admin on AD
        accounts.add(new IdentityAccount(
                first.toLowerCase() + "." + last.toLowerCase(), "ActiveDirectory", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AD_ADMIN_ROLES)), List.of("Security-Team"),
                true, LocalDateTime.now().minusDays(random.nextInt(30)),
                LocalDateTime.now().minusYears(3 + random.nextInt(2)),
                Map.of("department", "Security")));

        // Admin on AWS
        accounts.add(new IdentityAccount(
                first.charAt(0) + last.toLowerCase(), "AWS_IAM", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AWS_ADMIN_ROLES)), List.of("CloudAdmins"),
                true, LocalDateTime.now().minusDays(random.nextInt(14)),
                LocalDateTime.now().minusYears(2 + random.nextInt(2)), Map.of()));

        // Admin on Okta (triple admin = SoD violation)
        accounts.add(new IdentityAccount(
                email, "Okta", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(OKTA_ADMIN_ROLES)), List.of("OktaAdmins"),
                true, LocalDateTime.now().minusDays(random.nextInt(14)),
                LocalDateTime.now().minusYears(2), Map.of()));

        return accounts;
    }

    private List<IdentityAccount> generateDormantAdmin(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;
        String email = first.toLowerCase() + "." + last.toLowerCase() + "@acme.com";

        List<IdentityAccount> accounts = new ArrayList<>();

        // Admin on AD but dormant >90 days
        accounts.add(new IdentityAccount(
                first.toLowerCase() + "." + last.toLowerCase(), "ActiveDirectory", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AD_ADMIN_ROLES)), List.of("IT-Team"),
                true, LocalDateTime.now().minusDays(90 + random.nextInt(180)),
                LocalDateTime.now().minusYears(3 + random.nextInt(3)),
                Map.of("department", pick(DEPARTMENTS))));

        // Maybe also on AWS, also dormant
        if (random.nextDouble() < 0.5) {
            accounts.add(new IdentityAccount(
                    first.charAt(0) + last.toLowerCase(), "AWS_IAM", name, email, empId,
                    IdentityAccount.AccountStatus.ACTIVE,
                    List.of(pickArr(AWS_ADMIN_ROLES)), List.of("CloudAdmins"),
                    true, LocalDateTime.now().minusDays(100 + random.nextInt(200)),
                    LocalDateTime.now().minusYears(2), Map.of()));
        }

        return accounts;
    }

    private List<IdentityAccount> generateServiceAccount(int idx) {
        String name = pick(new String[]{"svc-etl-prod", "svc-deploy", "svc-backup", "svc-monitoring",
                "svc-pipeline", "svc-reporting", "svc-integration", "svc-dataflow"}) + "-" + idx;

        List<IdentityAccount> accounts = new ArrayList<>();

        accounts.add(new IdentityAccount(
                name, "AWS_IAM", name + " Service Account", "", "",
                IdentityAccount.AccountStatus.ACTIVE,
                List.of("AdministratorAccess", "IAMFullAccess"), List.of("CI-CD"),
                true, LocalDateTime.now().minusDays(180 + random.nextInt(200)),
                LocalDateTime.now().minusYears(2 + random.nextInt(2)), Map.of()));

        if (random.nextDouble() < 0.4) {
            accounts.add(new IdentityAccount(
                    name, "ServiceNow", name + " SvcAcct", "", "",
                    IdentityAccount.AccountStatus.ACTIVE,
                    List.of("admin", "itil"), List.of("ServiceAccounts"),
                    true, LocalDateTime.now().minusDays(200 + random.nextInt(100)),
                    LocalDateTime.now().minusYears(1), Map.of()));
        }

        return accounts;
    }

    private List<IdentityAccount> generateLegitimateHighPriv(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;
        String email = first.toLowerCase() + "." + last.toLowerCase() + "@acme.com";
        String dept = pick(new String[]{"Security", "IT", "DevOps", "Executive"});

        List<IdentityAccount> accounts = new ArrayList<>();

        // Admin on AD but recently active (legitimate)
        accounts.add(new IdentityAccount(
                first.toLowerCase() + "." + last.toLowerCase(), "ActiveDirectory", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of(pickArr(AD_ADMIN_ROLES)), List.of("Security-Team"),
                true, LocalDateTime.now().minusDays(random.nextInt(7)),
                LocalDateTime.now().minusYears(4 + random.nextInt(3)),
                Map.of("department", dept, "manager", "CISO")));

        // Admin on AWS but justified (security team)
        accounts.add(new IdentityAccount(
                first.charAt(0) + last.toLowerCase(), "AWS_IAM", name, email, empId,
                IdentityAccount.AccountStatus.ACTIVE,
                List.of("SecurityAudit", "IAMReadOnlyAccess"), List.of("SecurityAudit"),
                false, LocalDateTime.now().minusDays(random.nextInt(3)),
                LocalDateTime.now().minusYears(3), Map.of()));

        return accounts;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Audit Event Generator
    // ═══════════════════════════════════════════════════════════════════════

    private List<AuditEvent> generateAuditEvents(List<IdentityAccount> accounts, int count) {
        List<AuditEvent> events = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            IdentityAccount account = accounts.get(random.nextInt(accounts.size()));
            double roll = random.nextDouble();

            AuditEvent.EventType type;
            String desc;
            boolean success = true;
            String ip = pick(CORP_IPS) + (10 + random.nextInt(240));

            if (roll < 0.40) {
                type = AuditEvent.EventType.LOGIN_SUCCESS;
                desc = "Successful login to " + account.platform();
            } else if (roll < 0.50) {
                type = AuditEvent.EventType.LOGIN_FAILURE;
                desc = "Failed login attempt on " + account.platform();
                success = false;
            } else if (roll < 0.58) {
                type = AuditEvent.EventType.PRIVILEGE_GRANT;
                desc = "Role granted: " + pick(new String[]{"Admin", "PowerUser", "ReadOnly"});
            } else if (roll < 0.63) {
                type = AuditEvent.EventType.ROLE_CHANGE;
                desc = "Role changed on " + account.platform();
            } else if (roll < 0.73) {
                type = AuditEvent.EventType.RESOURCE_ACCESS;
                desc = "Accessed " + pick(new String[]{"S3 bucket", "EC2 instance", "RDS database", "Lambda function"});
            } else if (roll < 0.80) {
                type = AuditEvent.EventType.API_CALL;
                desc = "API call: " + pick(new String[]{"iam:CreateUser", "s3:PutObject", "ec2:RunInstances", "sts:AssumeRole"});
            } else if (roll < 0.85) {
                type = AuditEvent.EventType.TOKEN_USAGE;
                desc = "API token used from " + (random.nextDouble() < 0.3 ? "external" : "internal") + " IP";
                if (random.nextDouble() < 0.3) ip = pick(EXTERNAL_IPS) + (10 + random.nextInt(240));
            } else if (roll < 0.90) {
                type = AuditEvent.EventType.GROUP_ADD;
                desc = "Added to group: " + pick(new String[]{"CloudAdmins", "DevOps", "SecurityAudit", "IT-Team"});
            } else if (roll < 0.95) {
                type = AuditEvent.EventType.PASSWORD_CHANGE;
                desc = "Password changed on " + account.platform();
            } else {
                type = AuditEvent.EventType.MFA_BYPASS;
                desc = "MFA bypass detected on " + account.platform();
                ip = pick(EXTERNAL_IPS) + (10 + random.nextInt(240));
            }

            events.add(new AuditEvent(
                    "EVT-" + String.format("%05d", eventCounter.incrementAndGet()),
                    account.accountId(), account.platform(), type, desc,
                    LocalDateTime.now().minusDays(random.nextInt(90)).minusHours(random.nextInt(24)),
                    ip,
                    pick(new String[]{"prod-db", "staging-s3", "corp-vpn", "okta-sso", "salesforce-api"}),
                    success, Map.of()));
        }

        return events;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Offboarding Record Generators
    // ═══════════════════════════════════════════════════════════════════════

    private OffboardingRecord generateOffboardingRecord(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        Map<String, Boolean> status = new LinkedHashMap<>();
        status.put("ActiveDirectory", true); // disabled
        status.put("AWS_IAM", random.nextDouble() < 0.6 ? false : true); // gap!
        status.put("Okta", random.nextDouble() < 0.5 ? false : true);
        status.put("Salesforce", random.nextDouble() < 0.3 ? false : true);

        return new OffboardingRecord(empId, first + " " + last,
                LocalDate.now().minusDays(30 + random.nextInt(120)),
                status, pick(OffboardingRecord.TerminationType.values()));
    }

    private OffboardingRecord generateCleanOffboarding(int idx) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        Map<String, Boolean> status = new LinkedHashMap<>();
        status.put("ActiveDirectory", true);
        status.put("AWS_IAM", true);
        status.put("Okta", true);

        return new OffboardingRecord("EMP-" + String.format("%04d", idx),
                first + " " + last,
                LocalDate.now().minusDays(60 + random.nextInt(300)),
                status, pick(OffboardingRecord.TerminationType.values()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Temporary Access Exception Generators
    // ═══════════════════════════════════════════════════════════════════════

    private TemporaryAccessException generateStaleException(int idx, String empId) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        String name = first + " " + last;

        List<String> grantedPlatforms = new ArrayList<>(List.of("ActiveDirectory", "AWS_IAM", "Okta"));
        List<String> grantedRoles = List.of("Domain Admins", "AdministratorAccess", "SuperAdmin");

        // Expired 10-60 days ago
        LocalDateTime granted = LocalDateTime.now().minusDays(30 + random.nextInt(60));
        LocalDateTime expires = granted.plusDays(3); // 3-day exception

        // Only partially revoked (the gap!)
        List<String> revoked = new ArrayList<>();
        if (random.nextDouble() < 0.4) revoked.add("ActiveDirectory");
        // AWS and Okta never revoked = the risk

        String[] reasons = {
                "Incident response P1",
                "Emergency production fix",
                "Security investigation",
                "Infrastructure migration",
                "Audit compliance check"
        };

        return new TemporaryAccessException(
                "EXC-" + String.format("%04d", idx),
                empId, name, grantedPlatforms, grantedRoles,
                granted, expires, pick(reasons),
                revoked, pick(new String[]{"CISO", "VP-Engineering", "Security-Lead", "IT-Director"}));
    }

    private TemporaryAccessException generateCleanException(int idx) {
        String first = pick(FIRST_NAMES);
        String last = pick(LAST_NAMES);
        List<String> platforms = List.of("ActiveDirectory", "AWS_IAM");

        LocalDateTime granted = LocalDateTime.now().minusDays(90 + random.nextInt(180));
        LocalDateTime expires = granted.plusDays(7);

        return new TemporaryAccessException(
                "EXC-" + String.format("%04d", idx),
                "EMP-" + String.format("%04d", idx),
                first + " " + last,
                platforms,
                List.of("PowerUserAccess", "IT-Team"),
                granted, expires, "Planned maintenance window",
                new ArrayList<>(platforms), // fully revoked
                "Change-Board");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private String pick(String[] arr) {
        return arr[random.nextInt(arr.length)];
    }

    private <T> T pick(T[] arr) {
        return arr[random.nextInt(arr.length)];
    }

    private String[] pickArr(String[][] arrs) {
        return arrs[random.nextInt(arrs.length)];
    }

    private String pct(int count, int total) {
        return String.format("%.0f%%", (double) count / total * 100);
    }
}
