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
package org.springaicommunity.agent.memory.promotion;

import org.springaicommunity.agent.memory.model.Confidence;
import org.springaicommunity.agent.memory.model.KnowledgeState;
import org.springaicommunity.agent.memory.model.KnowledgeType;
import org.springaicommunity.agent.memory.model.KnowledgeUnit;
import org.springaicommunity.agent.memory.model.PromotionCandidate;
import org.springaicommunity.agent.memory.model.ValidationResult;
import org.springaicommunity.agent.memory.storage.MemoryStorage;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link PromotionEngine}.
 *
 * <p>Manages an in-memory list of pending {@link PromotionCandidate} objects.
 * On approval, creates a {@link KnowledgeUnit} with {@code state=KNOWLEDGE}
 * and persists it to {@link MemoryStorage}. On rejection, updates the candidate
 * status and leaves the source unit intact.
 *
 * @author Antigravity
 */
public class DefaultPromotionEngine implements PromotionEngine {

    private final MemoryStorage storage;
    private final PromotionRules rules;

    /** In-memory pending queue — replace with persistence for production use. */
    private final List<PromotionCandidate> pendingCandidates = new ArrayList<>();

    public DefaultPromotionEngine(MemoryStorage storage) {
        this(storage, new PromotionRules() {});
    }

    public DefaultPromotionEngine(MemoryStorage storage, PromotionRules rules) {
        Assert.notNull(storage, "Storage must not be null");
        Assert.notNull(rules, "PromotionRules must not be null");
        this.storage = storage;
        this.rules = rules;
    }

    @Override
    public PromotionCandidate nominate(KnowledgeUnit candidate, String nominatedBy) {
        Assert.notNull(candidate, "Candidate must not be null");
        Assert.hasText(nominatedBy, "NominatedBy must not be blank");

        // Determine global type mapping
        KnowledgeType targetType = mapToGlobalType(candidate.type());

        String candidateId = "PROMO-" + candidate.id();
        String proposedGlobalId = "G" + candidate.id();

        PromotionCandidate promo = PromotionCandidate.nominate(
                candidateId, candidate.id(), proposedGlobalId,
                candidate.statement(), targetType,
                candidate.evidence(), ValidationResult.passing(candidate.confidence()),
                nominatedBy);

        this.pendingCandidates.add(promo);
        return promo;
    }

    @Override
    public KnowledgeUnit approve(PromotionCandidate candidate, String approvedBy) {
        Assert.notNull(candidate, "Candidate must not be null");
        Assert.hasText(approvedBy, "ApprovedBy must not be blank");
        Assert.isTrue(candidate.isPending(), "Candidate is not in PENDING state: " + candidate.status());

        // Create promoted unit in GovernanceMemory
        double boostedConfidence = Math.min(1.0, candidate.validationResult().confidenceScore() + 0.1);
        KnowledgeUnit promoted = KnowledgeUnit.builder()
                .id(candidate.proposedGlobalId())
                .statement(candidate.proposedStatement())
                .type(candidate.targetType())
                .evidence(candidate.evidence())
                .confidence(boostedConfidence)
                .state(KnowledgeState.KNOWLEDGE)
                .lastReviewed(LocalDateTime.now())
                .build();

        this.storage.saveKnowledgeUnit(promoted);
        this.storage.deleteKnowledgeUnit(candidate.sourceId());

        // Update the in-memory candidate record
        PromotionCandidate approved = candidate.approve(approvedBy);
        this.pendingCandidates.replaceAll(p -> p.id().equals(candidate.id()) ? approved : p);

        return promoted;
    }

    @Override
    public PromotionCandidate reject(PromotionCandidate candidate, String rejectedBy, String reason) {
        Assert.notNull(candidate, "Candidate must not be null");
        Assert.hasText(rejectedBy, "RejectedBy must not be blank");
        Assert.isTrue(candidate.isPending(), "Candidate is not in PENDING state: " + candidate.status());

        PromotionCandidate rejected = candidate.reject(rejectedBy, reason);
        this.pendingCandidates.replaceAll(p -> p.id().equals(candidate.id()) ? rejected : p);
        return rejected;
    }

    @Override
    public Optional<PromotionCandidate> findPendingBySourceId(String sourceId) {
        return this.pendingCandidates.stream()
                .filter(c -> c.sourceId().equals(sourceId) && c.isPending())
                .findFirst();
    }

    /** Returns a snapshot of all candidates (pending + decided). */
    public List<PromotionCandidate> allCandidates() {
        return List.copyOf(this.pendingCandidates);
    }

    private KnowledgeType mapToGlobalType(KnowledgeType local) {
        return switch (local) {
            case LOCAL_PATTERN  -> KnowledgeType.GLOBAL_PATTERN;
            case LOCAL_OPINION  -> KnowledgeType.GLOBAL_OPINION;
            case RECOMMENDATION -> KnowledgeType.RECOMMENDATION;
            // IKOS type mappings
            case RISK_OBSERVATION, SECURITY_INCIDENT, AUDIT_FINDING,
                 REMEDIATION_ACTION -> KnowledgeType.SECURITY_KNOWLEDGE;
            default             -> KnowledgeType.ENGINEERING_KNOWLEDGE;
        };
    }
}
