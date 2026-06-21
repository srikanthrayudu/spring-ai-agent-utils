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

/**
 * Enumerates the identity risk types detected by the Risk Detection Engine.
 * Each type maps to MITRE ATT&CK techniques and NIST controls.
 *
 * @author Antigravity
 */
public enum RiskType {

    /** User disabled in one platform but still active in another. */
    OFFBOARDING_GAP("T1078", "Valid Accounts", "AC-2"),

    /** User has admin privileges across 3+ platforms simultaneously. */
    CROSS_PLATFORM_ADMIN("T1078.004", "Valid Accounts: Cloud Accounts", "AC-6"),

    /** Admin account with no login activity for > 90 days. */
    DORMANT_ADMIN("T1078", "Valid Accounts", "AC-2(3)"),

    /** Multiple role grants within a short timeframe (e.g. 7 days). */
    PRIVILEGE_SPIKE("T1098", "Account Manipulation", "AC-6(1)"),

    /** Old API token or service account key with unexpected usage patterns. */
    TOKEN_ABUSE("T1550", "Use Alternate Authentication Material", "IA-5(1)"),

    /** Account with no identifiable owner that remains active. */
    ORPHANED_ACCOUNT("T1136", "Create Account", "IA-4"),

    /** Separation of Duties violation: conflicting roles held by same identity. */
    SOD_VIOLATION("T1098", "Account Manipulation", "AC-5"),

    /** Temporary/exception access that was never revoked after expiry. */
    STALE_EXCEPTION("T1098.001", "Additional Cloud Credentials", "AC-2(2)"),

    /** Privilege accumulation through nested group memberships. */
    PRIVILEGE_CREEP("T1098", "Account Manipulation", "AC-6(5)"),

    /** Generic catch-all for custom risk rules. */
    CUSTOM("T1078", "Valid Accounts", "AC-2");

    private final String mitreId;
    private final String mitreName;
    private final String nistControl;

    RiskType(String mitreId, String mitreName, String nistControl) {
        this.mitreId = mitreId;
        this.mitreName = mitreName;
        this.nistControl = nistControl;
    }

    /** Returns the MITRE ATT&CK technique ID (e.g., "T1078"). */
    public String mitreId() { return mitreId; }

    /** Returns the MITRE ATT&CK technique name (e.g., "Valid Accounts"). */
    public String mitreName() { return mitreName; }

    /** Returns the NIST SP 800-53 control (e.g., "AC-2"). */
    public String nistControl() { return nistControl; }

    /** Returns formatted MITRE reference string. */
    public String mitreRef() {
        return String.format("MITRE ATT&CK %s: %s", mitreId, mitreName);
    }
}
