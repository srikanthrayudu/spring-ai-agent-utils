/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.web;

import org.springaicommunity.agent.ikos.Ikos;
import org.springaicommunity.agent.ikos.audit.AuditLogger;
import org.springaicommunity.agent.ikos.audit.AuditLogger.AuditAction;
import org.springaicommunity.agent.ikos.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for IKOS — enables integration with SOAR/SIEM platforms,
 * dashboards, and third-party tooling.
 *
 * <p>Endpoints:
 * <pre>
 *   GET  /api/ikos/risks         — List all risk observations
 *   GET  /api/ikos/knowledge     — List all knowledge units
 *   GET  /api/ikos/knowledge/{id} — Get a specific knowledge unit
 *   POST /api/ikos/incidents     — Record a security incident
 *   POST /api/ikos/scan          — Trigger an on-demand risk scan
 *   GET  /api/ikos/audit         — Query audit trail
 *   GET  /api/ikos/health        — Health check with component status
 * </pre>
 *
 * @author Antigravity
 */
@RestController
@RequestMapping("${ikos.api.base-path:/api/ikos}")
public class IkosRestController {

    private final Ikos ikos;
    private final AuditLogger auditLogger;

    public IkosRestController(Ikos ikos, AuditLogger auditLogger) {
        this.ikos = ikos;
        this.auditLogger = auditLogger;
    }

    // ── Knowledge ────────────────────────────────────────────────────────

    @GetMapping("/knowledge")
    public ResponseEntity<List<KnowledgeUnit>> listKnowledge(
            @RequestParam(required = false) KnowledgeType type) {
        List<KnowledgeUnit> units = type != null
                ? ikos.storage().listKnowledgeUnitsByType(type)
                : ikos.storage().listKnowledgeUnits();

        auditLogger.log("API", AuditAction.TOOL_CALL, "knowledge",
                Map.of("action", "list", "type", String.valueOf(type), "count", units.size()));

        return ResponseEntity.ok(units);
    }

    @GetMapping("/knowledge/{id}")
    public ResponseEntity<KnowledgeUnit> getKnowledge(@PathVariable String id) {
        auditLogger.log("API", AuditAction.TOOL_CALL, id, Map.of("action", "get"));
        return ikos.storage().getKnowledgeUnit(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Risks ────────────────────────────────────────────────────────────

    @GetMapping("/risks")
    public ResponseEntity<List<KnowledgeUnit>> listRisks() {
        List<KnowledgeUnit> risks = ikos.storage().listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
        List<KnowledgeUnit> incidents = ikos.storage().listKnowledgeUnitsByType(KnowledgeType.SECURITY_INCIDENT);

        List<KnowledgeUnit> all = new java.util.ArrayList<>(risks);
        all.addAll(incidents);

        auditLogger.log("API", AuditAction.TOOL_CALL, "risks",
                Map.of("count", all.size()));
        return ResponseEntity.ok(all);
    }

    // ── Incidents ─────────────────────────────────────────────────────────

    @PostMapping("/incidents")
    public ResponseEntity<Map<String, String>> recordIncident(@RequestBody IncidentRequest request) {
        String result = ikos.governanceTools().recordSecurityIncident(
                request.id(), request.statement(), request.context(), request.source());

        auditLogger.log("API", AuditAction.CREATE, request.id(),
                Map.of("type", "SECURITY_INCIDENT", "source", request.source()));

        return ResponseEntity.ok(Map.of("status", "created", "id", request.id(), "detail", result));
    }

    record IncidentRequest(String id, String statement, String context, String source) {}

    // ── On-Demand Scan ───────────────────────────────────────────────────

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerScan(
            @RequestParam(defaultValue = "100") int identityCount) {

        auditLogger.log("API", AuditAction.SCAN_STARTED, "on-demand",
                Map.of("identityCount", identityCount));

        var data = new org.springaicommunity.agent.ikos.simulation.SimulatedDataGenerator()
                .generate(identityCount);
        List<UnifiedIdentity> identities = ikos.correlationEngine().correlate(data.accounts());
        List<KnowledgeUnit> risks = ikos.riskEngine().detectRisks(identities);

        for (KnowledgeUnit risk : risks) {
            ikos.storage().saveKnowledgeUnit(risk);
        }

        auditLogger.log("API", AuditAction.SCAN_COMPLETED, "on-demand",
                Map.of("identities", identities.size(), "risksDetected", risks.size()));

        return ResponseEntity.ok(Map.of(
                "status", "completed",
                "identities", identities.size(),
                "risksDetected", risks.size(),
                "accounts", data.accounts().size()));
    }

    // ── Audit Trail ──────────────────────────────────────────────────────

    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogger.AuditEntry>> auditTrail(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditLogger.recent(limit));
    }

    // ── Health ────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        List<KnowledgeUnit> all = ikos.storage().listKnowledgeUnits();
        long risks = all.stream().filter(u -> u.type() == KnowledgeType.RISK_OBSERVATION).count();
        long incidents = all.stream().filter(u -> u.type() == KnowledgeType.SECURITY_INCIDENT).count();
        long patterns = all.stream().filter(u -> u.type() == KnowledgeType.GLOBAL_PATTERN
                || u.type() == KnowledgeType.LOCAL_PATTERN).count();

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "version", "0.11.0-SNAPSHOT",
                "knowledgeUnits", all.size(),
                "riskObservations", risks,
                "securityIncidents", incidents,
                "patterns", patterns,
                "storagePath", ikos.storage().toString()));
    }

}
