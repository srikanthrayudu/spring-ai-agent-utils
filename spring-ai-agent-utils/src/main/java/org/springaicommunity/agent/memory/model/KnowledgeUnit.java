/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.agent.memory.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents the fundamental unit of knowledge/memory in the Knowledge Evolution Framework.
 * Every memory object (Observations, Patterns, Recommendations, Decisions, etc.) conforms to this model.
 *
 * <p>The {@code id} field is a plain string so that human-readable identifiers like
 * {@code "OBS-001"} are preserved throughout storage, retrieval, and logging.
 *
 * @author Antigravity
 */
public class KnowledgeUnit {

    /** Human-readable string identifier (e.g. "OBS-001", "PAT-007"). */
    private String id;
    private String statement;
    private KnowledgeType type;
    private Object context;
    private List<Evidence> evidence;
    private double confidence;
    private List<ExceptionRule> exceptions;
    private KnowledgeState state;
    private Outcome outcome;
    private LocalDateTime createdAt;
    private LocalDateTime lastReviewed;

    public KnowledgeUnit() {}

    public KnowledgeUnit(String id, String statement, KnowledgeType type, Object context,
                         List<Evidence> evidence, double confidence, List<ExceptionRule> exceptions,
                         KnowledgeState state, Outcome outcome, LocalDateTime createdAt, LocalDateTime lastReviewed) {
        this.id = id;
        this.statement = statement;
        this.type = type;
        this.context = context;
        this.evidence = evidence;
        this.confidence = confidence;
        this.exceptions = exceptions;
        this.state = state;
        this.outcome = outcome;
        this.createdAt = createdAt;
        this.lastReviewed = lastReviewed;
    }

    // --- Standard getters/setters (for Jackson serialization) ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public KnowledgeType getType() { return type; }
    public void setType(KnowledgeType type) { this.type = type; }

    public Object getContext() { return context; }
    public void setContext(Object context) { this.context = context; }

    public List<Evidence> getEvidence() { return evidence; }
    public void setEvidence(List<Evidence> evidence) { this.evidence = evidence; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public List<ExceptionRule> getExceptions() { return exceptions; }
    public void setExceptions(List<ExceptionRule> exceptions) { this.exceptions = exceptions; }

    public KnowledgeState getState() { return state; }
    public void setState(KnowledgeState state) { this.state = state; }

    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome outcome) { this.outcome = outcome; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastReviewed() { return lastReviewed; }
    public void setLastReviewed(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; }

    // --- Record-style accessors (backward compatibility with existing call sites) ---

    public String id() { return id; }
    public String statement() { return statement; }
    public KnowledgeType type() { return type; }
    public Object context() { return context; }
    public List<Evidence> evidence() { return evidence; }
    public double confidence() { return confidence; }
    public List<ExceptionRule> exceptions() { return exceptions; }
    public String lastReviewed() { return lastReviewed != null ? lastReviewed.toString() : null; }

    /** Backward compat stub — returns empty string. */
    public String source() { return ""; }

    // --- Builder ---

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .statement(this.statement)
            .type(this.type)
            .context(this.context)
            .evidence(this.evidence)
            .confidence(this.confidence)
            .exceptions(this.exceptions)
            .state(this.state)
            .outcome(this.outcome)
            .createdAt(this.createdAt)
            .lastReviewed(this.lastReviewed);
    }

    public static class Builder {
        private String id;
        private String statement;
        private KnowledgeType type;
        private Object context;
        private List<Evidence> evidence = List.of();
        private double confidence = 1.0; // Default safe value — existing data without confidence treated as trusted
        private List<ExceptionRule> exceptions = List.of();
        private KnowledgeState state = KnowledgeState.KNOWLEDGE; // Default safe value for backward compat
        private Outcome outcome;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime lastReviewed = LocalDateTime.now();

        public Builder id(String id) { this.id = id; return this; }
        public Builder statement(String statement) { this.statement = statement; return this; }
        public Builder type(KnowledgeType type) { this.type = type; return this; }
        public Builder context(Object context) { this.context = context; return this; }

        /** Set evidence directly as typed Evidence objects. */
        public Builder evidence(List<Evidence> evidence) { this.evidence = evidence; return this; }

        public Builder confidence(double confidence) { this.confidence = confidence; return this; }

        /** Set exceptions directly as typed ExceptionRule objects. */
        public Builder exceptions(List<ExceptionRule> exceptions) { this.exceptions = exceptions; return this; }

        public Builder state(KnowledgeState state) { this.state = state; return this; }
        public Builder outcome(Outcome outcome) { this.outcome = outcome; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder lastReviewed(LocalDateTime lastReviewed) { this.lastReviewed = lastReviewed; return this; }

        /** Backward compat: accept raw strings and wrap them into Evidence records. */
        public Builder evidenceStrings(List<String> evidenceStr) {
            if (evidenceStr != null) {
                this.evidence = evidenceStr.stream()
                    .map(e -> new Evidence("legacy", e, 0.5))
                    .toList();
            }
            return this;
        }

        /** Backward compat: accept raw strings and wrap them into ExceptionRule records. */
        public Builder exceptionStrings(List<String> exceptionsStr) {
            if (exceptionsStr != null) {
                this.exceptions = exceptionsStr.stream()
                    .map(e -> new ExceptionRule(e, ""))
                    .toList();
            }
            return this;
        }

        /**
         * Backward compat: accept a string timestamp (e.g. from Instant.now().toString()).
         * Parses it if it is an ISO LocalDateTime; otherwise silently ignores it.
         */
        public Builder lastReviewed(String dateStr) {
            if (dateStr != null) {
                try {
                    this.lastReviewed = LocalDateTime.parse(dateStr);
                } catch (Exception e1) {
                    try {
                        // Handle Instant format like "2026-06-20T03:00:00Z"
                        this.lastReviewed = LocalDateTime.ofInstant(Instant.parse(dateStr), java.time.ZoneOffset.UTC);
                    } catch (Exception e2) {
                        // ignore unparseable timestamps
                    }
                }
            }
            return this;
        }

        /** Backward compat: accepted but not stored (source tracking replaced by Evidence). */
        public Builder source(String source) { return this; }

        public KnowledgeUnit build() {
            return new KnowledgeUnit(id, statement, type, context, evidence, confidence,
                exceptions, state, outcome, createdAt, lastReviewed);
        }
    }
}
