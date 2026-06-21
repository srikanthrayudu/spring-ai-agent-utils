/*
 * Copyright 2026 - 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */
package org.springaicommunity.agent.ikos.identity;

import org.springaicommunity.agent.ikos.model.IdentityAccount;
import org.springaicommunity.agent.ikos.model.UnifiedIdentity;

import java.util.List;

/**
 * Correlates accounts from different identity platforms into unified identities.
 *
 * <p>Resolves that:
 * <pre>
 *   john.smith (AD) = jsmith (AWS) = john.smith@corp.com (Okta)
 * </pre>
 *
 * @author Antigravity
 */
public interface IdentityCorrelationEngine {

    /**
     * Ingests a batch of identity accounts from multiple platforms
     * and returns a list of correlated unified identities.
     *
     * @param accounts all known accounts across all platforms
     * @return correlated unified identities
     */
    List<UnifiedIdentity> correlate(List<IdentityAccount> accounts);

    /**
     * Finds the unified identity that a given account belongs to.
     *
     * @param accountId the platform account ID
     * @param platform  the platform name
     * @return the matched unified identity, or null if not correlated
     */
    UnifiedIdentity findIdentity(String accountId, String platform);
}
