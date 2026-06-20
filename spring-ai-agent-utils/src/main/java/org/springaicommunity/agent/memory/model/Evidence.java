package org.springaicommunity.agent.memory.model;

/**
 * Represents data points supporting a KnowledgeUnit.
 */
public record Evidence(
        String sourceId,
        String description,
        double strength
) {
}
