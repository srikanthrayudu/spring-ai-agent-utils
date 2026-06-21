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
package org.springaicommunity.agent.ikos.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a {@link KnowledgeUnit} that has been nominated for promotion to
 * {@code GovernanceMemory}, pending human review.
 *
 * <p>The promotion workflow is:
 * <ol>
 *   <li>A VALIDATED_PATTERN or RECOMMENDATION is nominated → {@code PromotionCandidate(PENDING)}</li>
 *   <li>A human engineer reviews the candidate via tooling or UI.</li>
 *   <li>On {@code approve} → a new KnowledgeUnit(state=KNOWLEDGE) is persisted in GovernanceMemory.</li>
 *   <li>On {@code reject} → the original unit reverts to VALIDATED_PATTERN; reason is recorded.</li>
 * </ol>
 *
 * @author Antigravity
 */
public record PromotionCandidate(

        /** Unique ID for this promotion request (e.g. {@code "PROMO-001"}). */
        String id,

        /** ID of the source KnowledgeUnit being nominated. */
        String sourceId,

        /** Target ID for the promoted unit in GovernanceMemory (e.g. {@code "GPAT-001"}). */
        String proposedGlobalId,

        /** The statement of knowledge to be promoted. */
        String proposedStatement,

        /** The type the promoted unit will have in GovernanceMemory. */
        KnowledgeType targetType,

        /** All evidence items carried forward from the source unit. */
        List<Evidence> evidence,

        /** Validation result that qualified this unit for promotion. */
        ValidationResult validationResult,

        /** Name or ID of the person/system that nominated this candidate. */
        String proposedBy,

        /** Current review status. */
        PromotionStatus status,

        /** Name or ID of the human who approved or rejected. {@code null} while PENDING. */
        String reviewedBy,

        /** Reason provided on rejection. {@code null} when approved or still PENDING. */
        String rejectionReason,

        /** When the nomination was created. */
        LocalDateTime proposedAt,

        /** When the review decision was made. {@code null} while PENDING. */
        LocalDateTime reviewedAt

) {

    /**
     * Promotion review status.
     */
    public enum PromotionStatus {
        /** Awaiting human review. */
        PENDING,
        /** Approved — KnowledgeUnit promoted to GovernanceMemory. */
        APPROVED,
        /** Rejected — source unit reverted to VALIDATED_PATTERN. */
        REJECTED
    }

    /**
     * Factory method to create a new pending candidate.
     */
    public static PromotionCandidate nominate(
            String id, String sourceId, String proposedGlobalId,
            String proposedStatement, KnowledgeType targetType,
            List<Evidence> evidence, ValidationResult validationResult,
            String proposedBy) {
        return new PromotionCandidate(
                id, sourceId, proposedGlobalId, proposedStatement, targetType,
                evidence, validationResult, proposedBy,
                PromotionStatus.PENDING, null, null,
                LocalDateTime.now(), null);
    }

    /**
     * Returns a new candidate with status set to {@code APPROVED}.
     */
    public PromotionCandidate approve(String reviewedBy) {
        return new PromotionCandidate(
                id, sourceId, proposedGlobalId, proposedStatement, targetType,
                evidence, validationResult, proposedBy,
                PromotionStatus.APPROVED, reviewedBy, null,
                proposedAt, LocalDateTime.now());
    }

    /**
     * Returns a new candidate with status set to {@code REJECTED} and a reason recorded.
     */
    public PromotionCandidate reject(String reviewedBy, String reason) {
        return new PromotionCandidate(
                id, sourceId, proposedGlobalId, proposedStatement, targetType,
                evidence, validationResult, proposedBy,
                PromotionStatus.REJECTED, reviewedBy, reason,
                proposedAt, LocalDateTime.now());
    }

    public boolean isPending()  { return status == PromotionStatus.PENDING; }
    public boolean isApproved() { return status == PromotionStatus.APPROVED; }
    public boolean isRejected() { return status == PromotionStatus.REJECTED; }
}
