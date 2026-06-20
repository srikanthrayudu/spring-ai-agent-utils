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
 * Severity classification for identity risk observations.
 *
 * @author Antigravity
 */
public enum RiskSeverity {

    /** Informational — no immediate action needed. */
    LOW,

    /** Should be reviewed within the current review cycle. */
    MEDIUM,

    /** Requires action within 48 hours. */
    HIGH,

    /** Requires immediate remediation. */
    CRITICAL
}
