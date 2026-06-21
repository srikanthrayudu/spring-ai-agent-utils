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
package org.springaicommunity.agent.ikos.promotion;

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;
import org.springaicommunity.agent.ikos.model.PromotionCandidate;

import java.util.Optional;

/**
 * Manages the full promotion lifecycle for a {@link KnowledgeUnit}:
 * nomination → human review → approve/reject.
 *
 * <p>Promotion moves a locally-validated unit into {@code GovernanceMemory}
 * as organizational knowledge. A human must approve every promotion;
 * AI cannot autonomously elevate a unit to organizational truth.
 *
 * @author Antigravity
 */
public interface PromotionEngine {

    /**
     * Nominate a validated unit for promotion, creating a {@link PromotionCandidate}
     * with status {@code PENDING}.
     *
     * @param candidate   the local unit being nominated
     * @param nominatedBy name or ID of the person initiating promotion
     * @return a new {@code PromotionCandidate} awaiting review
     */
    PromotionCandidate nominate(KnowledgeUnit candidate, String nominatedBy);

    /**
     * Approve a pending {@link PromotionCandidate}.
     *
     * <p>Creates a new {@link KnowledgeUnit} with {@code state=KNOWLEDGE} in
     * {@code GovernanceMemory} and deletes the source unit from
     * {@code ApplicationMemory}.
     *
     * @param candidate  the candidate to approve
     * @param approvedBy name or ID of the approving human
     * @return the newly promoted {@link KnowledgeUnit}
     */
    KnowledgeUnit approve(PromotionCandidate candidate, String approvedBy);

    /**
     * Reject a pending {@link PromotionCandidate}.
     *
     * <p>Reverts the source unit's state to {@code VALIDATED_PATTERN} and
     * records the rejection reason.
     *
     * @param candidate  the candidate to reject
     * @param rejectedBy name or ID of the reviewing human
     * @param reason     explanation for the rejection
     * @return the updated {@code PromotionCandidate} with status {@code REJECTED}
     */
    PromotionCandidate reject(PromotionCandidate candidate, String rejectedBy, String reason);

    /**
     * Returns a pending candidate by source ID, if any exists.
     */
    Optional<PromotionCandidate> findPendingBySourceId(String sourceId);
}
