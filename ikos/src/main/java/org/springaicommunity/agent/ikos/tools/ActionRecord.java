/*
 * Copyright 2026 - 2026 the original author or authors.
 */
package org.springaicommunity.agent.ikos.tools;

import java.time.LocalDateTime;

/**
 * Immutable record of a single remediation action for audit compliance.
 * Top-level class to avoid synthetic inner class issues with exec-maven-plugin.
 */
public final class ActionRecord {
    private final String txId;
    private final String actionType;
    private final String target;
    private final String platform;
    private final String details;
    private final LocalDateTime timestamp;

    public ActionRecord(String txId, String actionType, String target,
                        String platform, String details, LocalDateTime timestamp) {
        this.txId = txId;
        this.actionType = actionType;
        this.target = target;
        this.platform = platform;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String txId() { return txId; }
    public String actionType() { return actionType; }
    public String target() { return target; }
    public String platform() { return platform; }
    public String details() { return details; }
    public LocalDateTime timestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s → %s on %s (%s)", txId, actionType, target, platform, details);
    }
}
