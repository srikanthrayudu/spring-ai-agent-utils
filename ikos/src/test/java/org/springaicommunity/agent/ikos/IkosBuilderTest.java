/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link Ikos} builder — verifies all components are wired correctly.
 */
@DisplayName("Ikos Builder")
class IkosBuilderTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Builds with storagePath and all defaults")
    void buildsWithDefaults() {
        Ikos ikos = Ikos.builder()
                .storagePath(tempDir.toString())
                .build();

        assertThat(ikos.storage()).isNotNull();
        assertThat(ikos.appMemory()).isNotNull();
        assertThat(ikos.govMemory()).isNotNull();
        assertThat(ikos.pipeline()).isNotNull();
        assertThat(ikos.promotionEngine()).isNotNull();
        assertThat(ikos.learningEngine()).isNotNull();
        assertThat(ikos.correlationEngine()).isNotNull();
        assertThat(ikos.riskEngine()).isNotNull();
        assertThat(ikos.governanceTools()).isNotNull();
        assertThat(ikos.engineeringTools()).isNotNull();
        assertThat(ikos.contextBuilder()).isNotNull();
        assertThat(ikos.advisor()).isNotNull();
        assertThat(ikos.behavioralAnalyzer()).isNotNull();
        assertThat(ikos.dashboardGenerator()).isNotNull();
        assertThat(ikos.consolidationEngine()).isNotNull();
    }

    @Test
    @DisplayName("Tools are functional after builder construction")
    void toolsAreFunctional() {
        Ikos ikos = Ikos.builder()
                .storagePath(tempDir.toString())
                .build();

        // Pipeline should work
        var obs = ikos.pipeline().createObservation(
                "OBS-001", "Test observation", "test-context", "test-source");
        assertThat(obs).isNotNull();
        assertThat(obs.id()).isEqualTo("OBS-001");

        // Storage should have the observation
        assertThat(ikos.storage().getKnowledgeUnit("OBS-001")).isPresent();

        // Governance tools should be callable
        String result = ikos.governanceTools().listAllKnowledge();
        assertThat(result).isNotBlank();
    }

    @Test
    @DisplayName("Fails with no storagePath and no storage")
    void failsWithoutStorage() {
        assertThatThrownBy(() -> Ikos.builder().build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Advisor configurable via builder")
    void advisorConfigurable() {
        Ikos ikos = Ikos.builder()
                .storagePath(tempDir.toString())
                .advisorMaxUnits(5)
                .advisorOrder(200)
                .build();

        assertThat(ikos.advisor()).isNotNull();
        assertThat(ikos.advisor().getOrder()).isEqualTo(200);
    }

}
