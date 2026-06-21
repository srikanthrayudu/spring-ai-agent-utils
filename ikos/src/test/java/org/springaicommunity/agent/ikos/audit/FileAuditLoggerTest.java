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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FileAuditLogger}.
 */
@DisplayName("FileAuditLogger")
class FileAuditLoggerTest {

    @TempDir
    Path tempDir;

    private FileAuditLogger logger;

    @BeforeEach
    void setUp() {
        logger = new FileAuditLogger(tempDir.toString());
    }

    @Test
    @DisplayName("Logs and retrieves audit entries")
    void logAndRetrieve() {
        logger.log("analyst-1", AuditLogger.AuditAction.CREATE, "RISK-001",
                Map.of("severity", "CRITICAL"));
        logger.log("analyst-1", AuditLogger.AuditAction.TOOL_CALL, "ListIdentityRisks",
                Map.of("tool", "IdentityGovernanceTools"));

        List<AuditLogger.AuditEntry> recent = logger.recent(10);
        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).actor()).isEqualTo("analyst-1");
    }

    @Test
    @DisplayName("Query by actor")
    void queryByActor() {
        logger.log("alice", AuditLogger.AuditAction.CREATE, "RISK-001", Map.of());
        logger.log("bob", AuditLogger.AuditAction.CREATE, "RISK-002", Map.of());
        logger.log("alice", AuditLogger.AuditAction.PROMOTE, "RISK-001", Map.of());

        List<AuditLogger.AuditEntry> aliceActions =
                logger.query(AuditLogger.AuditQuery.byActor("alice", 100));
        assertThat(aliceActions).hasSize(2);
    }

    @Test
    @DisplayName("Query by target")
    void queryByTarget() {
        logger.log("sys", AuditLogger.AuditAction.RISK_DETECTED, "RISK-001", Map.of());
        logger.log("sys", AuditLogger.AuditAction.RISK_REMEDIATED, "RISK-001", Map.of());
        logger.log("sys", AuditLogger.AuditAction.RISK_DETECTED, "RISK-002", Map.of());

        List<AuditLogger.AuditEntry> risk1 =
                logger.query(AuditLogger.AuditQuery.byTarget("RISK-001", 100));
        assertThat(risk1).hasSize(2);
    }

    @Test
    @DisplayName("Persists to disk and survives re-instantiation")
    void persistence() {
        logger.log("analyst", AuditLogger.AuditAction.SCAN_STARTED, "scan-1", Map.of());
        assertThat(logger.count()).isEqualTo(1);

        // Create a new logger pointing to the same directory
        FileAuditLogger logger2 = new FileAuditLogger(tempDir.toString());
        assertThat(logger2.count()).isEqualTo(1);
        assertThat(logger2.recent(10)).hasSize(1);
    }

    @Test
    @DisplayName("Empty on no entries")
    void emptyOnNoEntries() {
        assertThat(logger.recent(10)).isEmpty();
        assertThat(logger.count()).isZero();
    }

}
