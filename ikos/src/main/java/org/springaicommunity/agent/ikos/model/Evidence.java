package org.springaicommunity.agent.ikos.model;

/**
 * Represents data points supporting a KnowledgeUnit.
 */
public record Evidence(
        String sourceId,
        String description,
        double strength
) {
}
