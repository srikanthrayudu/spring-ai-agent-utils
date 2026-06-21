/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.ikos.model.KnowledgeType;
import org.springaicommunity.agent.ikos.model.KnowledgeUnit;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link FileMemoryStorage} — CRUD operations, persistence, filtering.
 */
@DisplayName("FileMemoryStorage")
class FileMemoryStorageTest {

    @TempDir
    Path tempDir;

    private FileMemoryStorage storage;

    @BeforeEach
    void setUp() {
        storage = new FileMemoryStorage(tempDir.toString());
    }

    @Test
    @DisplayName("Save and retrieve a knowledge unit by ID")
    void saveAndGet() {
        KnowledgeUnit unit = KnowledgeUnit.builder()
                .id("OBS-001")
                .statement("Suspicious login from unknown IP")
                .type(KnowledgeType.OBSERVATION)
                .confidence(0.5)
                .lastReviewed(LocalDateTime.now())
                .build();

        storage.saveKnowledgeUnit(unit);
        Optional<KnowledgeUnit> retrieved = storage.getKnowledgeUnit("OBS-001");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().statement()).isEqualTo("Suspicious login from unknown IP");
        assertThat(retrieved.get().confidence()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Get non-existent unit returns empty")
    void getNonExistent() {
        assertThat(storage.getKnowledgeUnit("DOES-NOT-EXIST")).isEmpty();
    }

    @Test
    @DisplayName("List all knowledge units")
    void listAll() {
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("Alert 1")
                .type(KnowledgeType.OBSERVATION).confidence(0.3).build());
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-002").statement("Alert 2")
                .type(KnowledgeType.RISK_OBSERVATION).confidence(0.7).build());

        List<KnowledgeUnit> all = storage.listKnowledgeUnits();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Filter by knowledge type")
    void filterByType() {
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("Observation")
                .type(KnowledgeType.OBSERVATION).confidence(0.3).build());
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("RISK-001").statement("Risk")
                .type(KnowledgeType.RISK_OBSERVATION).confidence(0.7).build());

        List<KnowledgeUnit> risks = storage.listKnowledgeUnitsByType(KnowledgeType.RISK_OBSERVATION);
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).id()).isEqualTo("RISK-001");
    }

    @Test
    @DisplayName("Delete a knowledge unit")
    void delete() {
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("To delete")
                .type(KnowledgeType.OBSERVATION).confidence(0.3).build());

        assertThat(storage.getKnowledgeUnit("OBS-001")).isPresent();
        storage.deleteKnowledgeUnit("OBS-001");
        assertThat(storage.getKnowledgeUnit("OBS-001")).isEmpty();
    }

    @Test
    @DisplayName("Overwrite existing unit with same ID")
    void overwrite() {
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("Original")
                .type(KnowledgeType.OBSERVATION).confidence(0.3).build());
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("Updated")
                .type(KnowledgeType.OBSERVATION).confidence(0.9).build());

        Optional<KnowledgeUnit> unit = storage.getKnowledgeUnit("OBS-001");
        assertThat(unit).isPresent();
        assertThat(unit.get().statement()).isEqualTo("Updated");
        assertThat(unit.get().confidence()).isEqualTo(0.9);
    }

    @Test
    @DisplayName("Persists to disk and survives re-instantiation")
    void persistence() {
        storage.saveKnowledgeUnit(KnowledgeUnit.builder()
                .id("OBS-001").statement("Persisted")
                .type(KnowledgeType.OBSERVATION).confidence(0.5).build());

        // Create a new storage instance pointing to the same directory
        FileMemoryStorage storage2 = new FileMemoryStorage(tempDir.toString());
        Optional<KnowledgeUnit> unit = storage2.getKnowledgeUnit("OBS-001");
        assertThat(unit).isPresent();
        assertThat(unit.get().statement()).isEqualTo("Persisted");
    }

}
