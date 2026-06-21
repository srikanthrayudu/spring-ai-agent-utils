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
 * A reference to a governance policy, compliance control, or internal standard
 * that a {@link KnowledgeUnit} relates to.
 *
 * @param policyId      unique identifier (e.g. "PAM-004", "NIST-AC-6")
 * @param framework     policy framework (e.g. "NIST SP 800-53", "Internal PAM Policy")
 * @param controlTitle  human-readable control name
 * @param description   brief description of the policy requirement
 *
 * @author Antigravity
 */
public record PolicyReference(
        String policyId,
        String framework,
        String controlTitle,
        String description
) {
    /**
     * Factory for NIST SP 800-53 controls.
     */
    public static PolicyReference nist(String controlId, String title, String description) {
        return new PolicyReference(controlId, "NIST SP 800-53", title, description);
    }

    /**
     * Factory for internal PAM policies.
     */
    public static PolicyReference pam(String policyId, String title, String description) {
        return new PolicyReference(policyId, "Internal PAM Policy", title, description);
    }

    /**
     * Factory for internal access control policies.
     */
    public static PolicyReference accessControl(String policyId, String title, String description) {
        return new PolicyReference(policyId, "Access Control Policy", title, description);
    }
}
