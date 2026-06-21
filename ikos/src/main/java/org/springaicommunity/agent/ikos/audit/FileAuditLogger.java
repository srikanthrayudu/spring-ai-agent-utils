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

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

/**
 * Append-only, file-based audit logger.
 *
 * <p>Each audit entry is written as a single-line JSON to an append-only log file.
 * In-memory ring buffer provides fast {@link #recent(int)} lookups.
 *
 * <p>Production deployments should swap to {@code JdbcAuditLogger} or
 * externalize to a SIEM (Splunk, Elastic, etc.).
 *
 * @author Antigravity
 */
public class FileAuditLogger implements AuditLogger {

    private final Path auditFile;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedDeque<AuditEntry> recentBuffer;
    private static final int BUFFER_SIZE = 1000;

    public FileAuditLogger(String storagePath) {
        Path dir = Paths.get(storagePath).toAbsolutePath().normalize().resolve("audit");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create audit directory at: " + dir, e);
        }
        this.auditFile = dir.resolve("audit-trail.jsonl");
        this.objectMapper = new ObjectMapper();
        this.recentBuffer = new ConcurrentLinkedDeque<>();

        // Warm the buffer from existing log
        loadRecentFromDisk();
    }

    @Override
    public void log(String actor, AuditAction action, String targetId, Map<String, Object> details) {
        AuditEntry entry = new AuditEntry(
                UUID.randomUUID().toString(),
                actor != null ? actor : "SYSTEM",
                action,
                targetId,
                details != null ? details : Map.of(),
                LocalDateTime.now());

        // Append to file (immutable, append-only)
        try {
            String line = objectMapper.writeValueAsString(entry) + "\n";
            Files.writeString(auditFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            // Audit logging must never crash the application
            System.err.println("[IKOS-AUDIT] Failed to write audit entry: " + e.getMessage());
        }

        // Update ring buffer
        recentBuffer.addFirst(entry);
        while (recentBuffer.size() > BUFFER_SIZE) {
            recentBuffer.removeLast();
        }
    }

    @Override
    public List<AuditEntry> query(AuditQuery query) {
        List<AuditEntry> all = loadAllFromDisk();
        return all.stream()
                .filter(e -> query.actor() == null || query.actor().equals(e.actor()))
                .filter(e -> query.action() == null || query.action() == e.action())
                .filter(e -> query.targetId() == null || query.targetId().equals(e.targetId()))
                .filter(e -> query.since() == null || !e.timestamp().isBefore(query.since()))
                .filter(e -> query.until() == null || !e.timestamp().isAfter(query.until()))
                .limit(query.limit() > 0 ? query.limit() : 100)
                .toList();
    }

    @Override
    public List<AuditEntry> recent(int limit) {
        return recentBuffer.stream().limit(limit).toList();
    }

    /**
     * Returns the total count of audit entries.
     */
    public long count() {
        if (!Files.exists(auditFile)) return 0;
        try (Stream<String> lines = Files.lines(auditFile)) {
            return lines.count();
        } catch (IOException e) {
            return recentBuffer.size();
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────

    private void loadRecentFromDisk() {
        if (!Files.exists(auditFile)) return;
        try (Stream<String> lines = Files.lines(auditFile)) {
            lines.forEach(line -> {
                try {
                    AuditEntry entry = objectMapper.readValue(line, AuditEntry.class);
                    recentBuffer.addFirst(entry);
                    if (recentBuffer.size() > BUFFER_SIZE) recentBuffer.removeLast();
                } catch (Exception ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private List<AuditEntry> loadAllFromDisk() {
        if (!Files.exists(auditFile)) return List.of();
        List<AuditEntry> entries = new ArrayList<>();
        try (Stream<String> lines = Files.lines(auditFile)) {
            lines.forEach(line -> {
                try {
                    entries.add(objectMapper.readValue(line, AuditEntry.class));
                } catch (Exception ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return entries;
    }

}
