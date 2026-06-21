/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.connector;

import org.springaicommunity.agent.ikos.model.AuditEvent;
import org.springaicommunity.agent.ikos.model.IdentityAccount;

import java.time.Instant;
import java.util.List;

/**
 * SPI for pluggable identity platform data sources.
 *
 * <p>Implement this interface to connect IKOS to real IAM platforms
 * (Active Directory, AWS IAM, Okta, SailPoint, CyberArk, etc.).
 *
 * <p>Example:
 * <pre>
 * public class AwsIamDataSource implements IdentityDataSource {
 *     public String platformName() { return "AWS_IAM"; }
 *     public List&lt;IdentityAccount&gt; fetchAccounts() {
 *         // Use AWS SDK IAM client to list users, roles, policies
 *     }
 *     public List&lt;AuditEvent&gt; fetchAuditEvents(Instant since) {
 *         // Use CloudTrail to query IAM events
 *     }
 * }
 * </pre>
 *
 * @author Antigravity
 */
public interface IdentityDataSource {

    /**
     * Unique platform identifier (e.g. "ActiveDirectory", "AWS_IAM", "Okta").
     */
    String platformName();

    /**
     * Display name for UI/reports.
     */
    default String displayName() {
        return platformName();
    }

    /**
     * Fetch all identity accounts from this platform.
     *
     * @return list of accounts; empty if the source is unavailable
     */
    List<IdentityAccount> fetchAccounts();

    /**
     * Fetch audit events since the given timestamp.
     *
     * @param since only return events after this time
     * @return list of audit events; empty if the source is unavailable
     */
    List<AuditEvent> fetchAuditEvents(Instant since);

    /**
     * Whether this data source is currently reachable.
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Priority for source ordering (lower = first).
     */
    default int priority() {
        return 100;
    }

}
