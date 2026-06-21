package org.springaicommunity.agent.ikos.model;

/**
 * Defines conditions under which a Pattern or Recommendation does not apply.
 */
public record ExceptionRule(
        String ruleDescription,
        String contextCondition
) {
}
