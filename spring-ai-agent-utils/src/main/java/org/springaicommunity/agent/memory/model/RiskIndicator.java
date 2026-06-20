/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.memory.model;

/**
 * Represents a specific risk signal attached to a {@link KnowledgeUnit}.
 *
 * @param riskType     the category of identity risk
 * @param severity     urgency classification
 * @param description  human-readable risk explanation
 * @param riskScore    computed risk score [0.0–1.0], higher = riskier
 *
 * @author Antigravity
 */
public record RiskIndicator(
        RiskType riskType,
        RiskSeverity severity,
        String description,
        double riskScore
) {
    /**
     * Factory for a critical-severity risk indicator.
     */
    public static RiskIndicator critical(RiskType type, String description) {
        return new RiskIndicator(type, RiskSeverity.CRITICAL, description, 0.95);
    }

    /**
     * Factory for a high-severity risk indicator.
     */
    public static RiskIndicator high(RiskType type, String description) {
        return new RiskIndicator(type, RiskSeverity.HIGH, description, 0.80);
    }

    /**
     * Factory for a medium-severity risk indicator.
     */
    public static RiskIndicator medium(RiskType type, String description) {
        return new RiskIndicator(type, RiskSeverity.MEDIUM, description, 0.50);
    }
}
