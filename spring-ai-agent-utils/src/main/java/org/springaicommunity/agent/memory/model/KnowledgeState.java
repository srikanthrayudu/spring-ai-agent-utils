package org.springaicommunity.agent.memory.model;

/**
 * Represents the full lifecycle state of a {@link KnowledgeUnit} within the
 * Knowledge Evolution Framework (KEF).
 *
 * <p>State transitions follow the learning pipeline:
 * <pre>
 * OBSERVATION → PATTERN_CANDIDATE → VALIDATED_PATTERN → RECOMMENDATION
 *           → PROMOTION_CANDIDATE → KNOWLEDGE → DEPRECATED → ARCHIVED
 * </pre>
 *
 * @author Antigravity
 */
public enum KnowledgeState {

    /** Raw fact captured from a project event. Confidence starts low. */
    OBSERVATION,

    /** Candidate pattern discovered from multiple observations. Not yet validated. */
    PATTERN_CANDIDATE,

    /** Pattern confirmed by validation engine and free of contradictions. */
    VALIDATED_PATTERN,

    /** Actionable recommendation derived from a validated pattern. */
    RECOMMENDATION,

    /** Nominated for promotion to GovernanceMemory. Awaiting human review. */
    PROMOTION_CANDIDATE,

    /** Promoted, human-approved organizational knowledge. Highest trust. */
    KNOWLEDGE,

    /** Superseded by a newer or contradicting unit. Kept for audit history. */
    DEPRECATED,

    /** Removed from active retrieval. Retained only for long-term archival. */
    ARCHIVED
}
