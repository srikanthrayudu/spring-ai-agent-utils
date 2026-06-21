/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.risk;

import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fingerprint-based deduplication for risk observations.
 *
 * <p>Running risk detection multiple times against the same data currently
 * creates duplicate findings with different IDs. This engine generates
 * a deterministic fingerprint from (identity + risk type + platform) and
 * deduplicates identical findings.
 *
 * <p>When a duplicate is found, the newer finding's confidence is merged
 * with the existing one (max wins), and the existing entry is updated
 * rather than creating a new one.
 *
 * @author Antigravity
 */
public class RiskDeduplicationEngine {

    /**
     * Result of deduplication.
     */
    public record DeduplicationResult(
            List<KnowledgeUnit> uniqueRisks,
            int originalCount,
            int deduplicatedCount,
            int duplicatesRemoved
    ) {
        public double reductionPercentage() {
            return originalCount == 0 ? 0.0
                    : (double) duplicatesRemoved / originalCount * 100.0;
        }
    }

    /**
     * Deduplicate a list of risk knowledge units.
     *
     * <p>Fingerprint is based on a SHA-256 hash of:
     * (identity reference extracted from context) + (risk type keyword) + (platform list).
     *
     * @param risks raw risk observations (may contain duplicates)
     * @return deduplicated result with merged confidence
     */
    public DeduplicationResult deduplicate(List<KnowledgeUnit> risks) {
        if (risks == null || risks.isEmpty()) {
            return new DeduplicationResult(List.of(), 0, 0, 0);
        }

        Map<String, KnowledgeUnit> seen = new LinkedHashMap<>();

        for (KnowledgeUnit risk : risks) {
            String fingerprint = computeFingerprint(risk);

            if (seen.containsKey(fingerprint)) {
                // Merge: keep the higher confidence
                KnowledgeUnit existing = seen.get(fingerprint);
                if (risk.confidence() > existing.confidence()) {
                    seen.put(fingerprint, risk);
                }
            } else {
                seen.put(fingerprint, risk);
            }
        }

        List<KnowledgeUnit> unique = new ArrayList<>(seen.values());
        return new DeduplicationResult(
                unique,
                risks.size(),
                unique.size(),
                risks.size() - unique.size());
    }

    /**
     * Compute a deterministic fingerprint for a risk observation.
     *
     * <p>Extracts:
     * <ul>
     *   <li>Identity reference from context ("Identity: X")</li>
     *   <li>Risk type keyword from the statement (OFFBOARDING, DORMANT, SOD, etc.)</li>
     *   <li>Platform list from the context</li>
     * </ul>
     */
    public String computeFingerprint(KnowledgeUnit risk) {
        String identity = extractIdentityRef(risk);
        String riskType = extractRiskType(risk);
        String platforms = extractPlatforms(risk);

        String raw = identity + "|" + riskType + "|" + platforms;
        return sha256(raw);
    }

    // ── Extraction ───────────────────────────────────────────────────────

    private String extractIdentityRef(KnowledgeUnit risk) {
        String ctx = risk.context() != null ? risk.context().toString() : "";
        // Pattern: "Identity: John Smith (UID-xxx)"
        int idx = ctx.indexOf("Identity:");
        if (idx >= 0) {
            String sub = ctx.substring(idx + 9).trim();
            int end = sub.indexOf("|");
            return end > 0 ? sub.substring(0, end).trim() : sub.trim();
        }
        // Fallback: first 50 chars of statement
        String stmt = risk.statement() != null ? risk.statement() : "";
        return stmt.length() > 50 ? stmt.substring(0, 50) : stmt;
    }

    private String extractRiskType(KnowledgeUnit risk) {
        String stmt = risk.statement() != null ? risk.statement().toUpperCase() : "";
        // Match known risk type keywords
        String[] keywords = {
                "OFFBOARDING GAP", "CROSS-PLATFORM ADMIN", "DORMANT ADMIN",
                "SOD VIOLATION", "PRIVILEGE CREEP", "ORPHANED ACCOUNT",
                "STALE SERVICE ACCOUNT", "STALE EXCEPTION",
                "CONTRACTOR EXCESSIVE ACCESS", "CREDENTIAL ROTATION VIOLATION"
        };
        for (String kw : keywords) {
            if (stmt.contains(kw)) return kw;
        }
        return "UNKNOWN";
    }

    private String extractPlatforms(KnowledgeUnit risk) {
        String ctx = risk.context() != null ? risk.context().toString() : "";
        int idx = ctx.indexOf("Platforms:");
        if (idx >= 0) {
            String sub = ctx.substring(idx + 10).trim();
            int end = sub.indexOf("|");
            return end > 0 ? sub.substring(0, end).trim() : sub.trim();
        }
        return "";
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().substring(0, 16); // first 16 hex chars
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available
            return Integer.toHexString(input.hashCode());
        }
    }

}
