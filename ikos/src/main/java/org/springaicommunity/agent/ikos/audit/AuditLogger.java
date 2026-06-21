/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.audit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Immutable audit trail for all IKOS operations.
 *
 * <p>Every tool invocation, knowledge mutation, promotion decision,
 * and remediation action is logged with actor, timestamp, and payload.
 *
 * <p>Enterprise compliance requires this for SOX, HIPAA, and FedRAMP.
 *
 * @author Antigravity
 */
public interface AuditLogger {

    /**
     * Log an action to the audit trail.
     *
     * @param actor     who performed the action (user ID, system name)
     * @param action    what was done (CREATE, UPDATE, DELETE, TOOL_CALL, PROMOTE, REJECT)
     * @param targetId  the ID of the affected knowledge unit or entity
     * @param details   additional structured metadata
     */
    void log(String actor, AuditAction action, String targetId, Map<String, Object> details);

    /**
     * Query audit entries with filters.
     */
    List<AuditEntry> query(AuditQuery query);

    /**
     * Get the last N audit entries.
     */
    List<AuditEntry> recent(int limit);

    // ── Value Types ──────────────────────────────────────────────────────

    enum AuditAction {
        CREATE, UPDATE, DELETE,
        TOOL_CALL, TOOL_RESULT,
        PROMOTE, REJECT,
        RISK_DETECTED, RISK_REMEDIATED,
        LOGIN, SCAN_STARTED, SCAN_COMPLETED,
        CONFIG_CHANGED
    }

    record AuditEntry(
            String id,
            String actor,
            AuditAction action,
            String targetId,
            Map<String, Object> details,
            LocalDateTime timestamp
    ) {}

    record AuditQuery(
            String actor,
            AuditAction action,
            String targetId,
            LocalDateTime since,
            LocalDateTime until,
            int limit
    ) {
        public static AuditQuery all(int limit) {
            return new AuditQuery(null, null, null, null, null, limit);
        }

        public static AuditQuery byActor(String actor, int limit) {
            return new AuditQuery(actor, null, null, null, null, limit);
        }

        public static AuditQuery byTarget(String targetId, int limit) {
            return new AuditQuery(null, null, targetId, null, null, limit);
        }

        public static AuditQuery since(LocalDateTime since, int limit) {
            return new AuditQuery(null, null, null, since, null, limit);
        }
    }

}
